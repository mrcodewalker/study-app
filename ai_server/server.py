"""
╔══════════════════════════════════════════════════════════╗
║        AI Study Server  •  Powered by Ollama             ║
╚══════════════════════════════════════════════════════════╝
"""

import re, time, socket, json, hashlib, requests, uvicorn, uuid
from datetime import datetime
from pathlib import Path
from fastapi import FastAPI, HTTPException, Request
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import HTMLResponse
from pydantic import BaseModel, Field
from typing import Optional

# ─────────────────────────────────────────────
#  Config
# ─────────────────────────────────────────────

OLLAMA_BASE   = "http://localhost:11434"
MODEL         = "qwen2.5:7b"
HOST          = "0.0.0.0"
PORT          = 8000
MAX_TOKENS    = -1
HISTORY_FILE  = Path(__file__).parent / "history.json"
CACHE_FILE    = Path(__file__).parent / "cache.json"
SESSIONS_FILE = Path(__file__).parent / "sessions.json"
MAX_SESSION_MESSAGES = 40   # keep last N messages per session

# ─────────────────────────────────────────────
#  Persistent storage helpers
# ─────────────────────────────────────────────

def _load_json(path: Path, default):
    try:
        return json.loads(path.read_text(encoding="utf-8")) if path.exists() else default
    except Exception:
        return default

def _save_json(path: Path, data):
    path.write_text(json.dumps(data, ensure_ascii=False, indent=2), encoding="utf-8")

_cache: dict    = _load_json(CACHE_FILE, {})
_history: list  = _load_json(HISTORY_FILE, [])
_sessions: dict = _load_json(SESSIONS_FILE, {})  # session_id -> list of {role, content}

_stats = {
    "requests": len(_history),
    "cards_generated": sum(h.get("cards_count", 0) for h in _history),
    "cache_hits": 0,
    "chat_messages": sum(len(v) for v in _sessions.values()),
    "started_at": datetime.now().isoformat(),
}

# ─────────────────────────────────────────────
#  App
# ─────────────────────────────────────────────

app = FastAPI(
    title="AI Study Server",
    description="Local LLM server for StudyApp — flashcards + general chat via Ollama.",
    version="4.0.0",
)
app.add_middleware(CORSMiddleware, allow_origins=["*"], allow_methods=["*"], allow_headers=["*"])

# ─────────────────────────────────────────────
#  Schemas
# ─────────────────────────────────────────────

class GenerateRequest(BaseModel):
    topic: str     = Field(..., min_length=1, max_length=200)
    count: int     = Field(10, ge=1, le=50)
    language: str  = Field("Vietnamese")
    card_type: str = Field("term_def")

class Card(BaseModel):
    front: str
    back: str

class GenerateResponse(BaseModel):
    cards: list[Card]
    error_lines: list[str]
    raw_output: str
    duration_ms: int
    model: str
    from_cache: bool = False

class ChatMessage(BaseModel):
    role: str     # "user" | "assistant"
    content: str

class ChatRequest(BaseModel):
    session_id: str  = Field(default_factory=lambda: str(uuid.uuid4()))
    message: str     = Field(..., min_length=1, max_length=4000)
    language: str    = Field("Vietnamese")

class ChatResponse(BaseModel):
    session_id: str
    reply: str
    duration_ms: int
    model: str
    history_length: int

class StatusResponse(BaseModel):
    status: str
    model: str
    message: str
    uptime_seconds: int
    stats: dict
    lan_ip: str = ""

# ─────────────────────────────────────────────
#  Core helpers
# ─────────────────────────────────────────────

def _ollama_status() -> tuple[bool, bool]:
    try:
        r = requests.get(f"{OLLAMA_BASE}/api/tags", timeout=3)
        models = [m["name"] for m in r.json().get("models", [])]
        return True, any(MODEL.split(":")[0] in m for m in models)
    except Exception:
        return False, False

def _cache_key(topic: str, count: int, language: str, card_type: str) -> str:
    raw = f"{topic.lower().strip()}|{count}|{language.lower()}|{card_type}"
    return hashlib.md5(raw.encode()).hexdigest()

def _local_ip() -> str:
    try:
        s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
        s.connect(("8.8.8.8", 80))
        ip = s.getsockname()[0]; s.close()
        return ip
    except Exception:
        return "unknown"

def _has_cjk(text: str) -> bool:
    return any('\u4e00' <= c <= '\u9fff' or '\u3400' <= c <= '\u4dbf' for c in text)

def _parse(raw: str) -> tuple[list[Card], list[str]]:
    strip_num = re.compile(r"^\s*\d+[\.\)]\s*")
    strip_md  = re.compile(r"\*{1,2}|_{1,2}|`")
    cards, errors, seen = [], [], set()
    for line in raw.splitlines():
        line = line.strip()
        if not line or line.startswith("#") or line.startswith("<"):
            continue
        cleaned = strip_md.sub("", strip_num.sub("", line)).strip()
        if "~" not in cleaned:
            if cleaned:
                errors.append(line)
            continue
        sep = cleaned.index("~")
        front, back = cleaned[:sep].strip(), cleaned[sep + 1:].strip()
        if not front or not back:
            errors.append(line); continue
        if _has_cjk(back):
            errors.append(f"[CJK detected, skipped] {line}"); continue
        key = front.lower()
        if key not in seen:
            seen.add(key)
            cards.append(Card(front=front, back=back))
    return cards, errors

def _record_history(topic, count, language, cards_count, duration_ms, from_cache, client_ip, card_type="term_def"):
    entry = {
        "time": datetime.now().strftime("%Y-%m-%d %H:%M:%S"),
        "topic": topic, "count": count, "language": language,
        "card_type": card_type, "cards_count": cards_count,
        "duration_ms": duration_ms, "from_cache": from_cache, "client_ip": client_ip,
    }
    _history.insert(0, entry)
    if len(_history) > 200: _history.pop()
    _save_json(HISTORY_FILE, _history)

def _get_session(session_id: str) -> list:
    return _sessions.get(session_id, [])

def _save_session(session_id: str, messages: list):
    _sessions[session_id] = messages[-MAX_SESSION_MESSAGES:]
    _save_json(SESSIONS_FILE, _sessions)

def _build_chat_system_prompt(language: str) -> str:
    return (
        f"You are KMAStudy AI, a helpful and friendly study assistant for students. "
        f"You help with studying, explaining concepts, answering questions, summarizing topics, "
        f"giving study tips, and general knowledge questions. "
        f"You can also help create study plans, explain difficult topics simply, "
        f"and motivate students. "
        f"IMPORTANT: Always respond in {language}. "
        f"Be concise, clear, and encouraging. Use bullet points or numbered lists when helpful. "
        f"If asked to create flashcards, suggest using the AI Generate feature in the app."
    )

# ─────────────────────────────────────────────
#  Flashcard prompt builder
# ─────────────────────────────────────────────

def _build_prompt(topic: str, count: int, language: str, card_type: str = "term_def") -> str:
    count = max(1, min(count, 50))
    n = count
    examples = {
        "term_def": (
            "Array ~ Cấu trúc dữ liệu lưu trữ các phần tử cùng kiểu theo chỉ số liên tiếp trong bộ nhớ.\n"
            "Stack ~ Cấu trúc dữ liệu LIFO: phần tử thêm vào sau sẽ được lấy ra trước.\n"
            "Queue ~ Cấu trúc dữ liệu FIFO: phần tử thêm vào trước sẽ được lấy ra trước."
        ),
        "en_vi": (
            "Recursion ~ Đệ quy — kỹ thuật hàm tự gọi lại chính nó để giải quyết bài toán con.\n"
            "Pointer ~ Con trỏ — biến lưu địa chỉ bộ nhớ của biến khác.\n"
            "Inheritance ~ Kế thừa — cơ chế lớp con nhận thuộc tính và phương thức từ lớp cha."
        ),
        "vi_en": (
            "Đệ quy ~ Recursion — a technique where a function calls itself to solve subproblems.\n"
            "Con trỏ ~ Pointer — a variable that stores the memory address of another variable.\n"
            "Kế thừa ~ Inheritance — a mechanism where a subclass acquires properties from a parent class."
        ),
        "qa": (
            "Stack và Queue khác nhau như thế nào? ~ Stack là LIFO (vào sau ra trước), Queue là FIFO (vào trước ra trước).\n"
            "Độ phức tạp thời gian của Binary Search là bao nhiêu? ~ O(log n) vì mỗi bước loại bỏ một nửa không gian tìm kiếm.\n"
            "Khi nào nên dùng Linked List thay vì Array? ~ Khi cần thêm/xóa phần tử thường xuyên ở đầu/giữa danh sách."
        ),
        "antonym": (
            "Đồng bộ (Synchronous) ~ Trái nghĩa: Bất đồng bộ (Asynchronous) | Đồng nghĩa: Tuần tự (Sequential)\n"
            "Tĩnh (Static) ~ Trái nghĩa: Động (Dynamic) | Đồng nghĩa: Cố định (Fixed)\n"
            "Mã hóa (Encryption) ~ Trái nghĩa: Giải mã (Decryption) | Đồng nghĩa: Mã hóa dữ liệu (Data encoding)"
        ),
        "code": (
            "len(list) ~ Trả về số phần tử trong list. Ví dụ: len([1,2,3]) → 3\n"
            "list.append(x) ~ Thêm phần tử x vào cuối list. Ví dụ: a.append(5) → [1,2,3,5]\n"
            "dict.get(key, default) ~ Lấy giá trị theo key, trả default nếu không tìm thấy."
        ),
    }
    roles = {
        "term_def": "You are an expert educator specializing in creating precise academic flashcards.",
        "en_vi":    "You are a bilingual English-Vietnamese language teacher.",
        "vi_en":    "You are a bilingual Vietnamese-English language teacher.",
        "qa":       "You are an expert exam question writer for academic subjects.",
        "antonym":  "You are a linguistics expert specializing in vocabulary relationships.",
        "code":     "You are a senior software engineer and programming instructor.",
    }
    tasks = {
        "term_def": (f"Create {n} flashcards for the topic \"{topic}\".\nEach card: Technical Term ~ Definition in {language}\n- Front: exact technical term from \"{topic}\"\n- Back: precise, concise definition (1-2 sentences max) in {language}\n- Do NOT generate generic or off-topic cards"),
        "en_vi":    (f"Create {n} English-Vietnamese vocabulary flashcards for the topic \"{topic}\".\nEach card: English word or phrase ~ Vietnamese meaning\n- Front: English term commonly used in \"{topic}\"\n- Back: Vietnamese translation + short usage note if helpful"),
        "vi_en":    (f"Create {n} Vietnamese-English vocabulary flashcards for the topic \"{topic}\".\nEach card: Vietnamese word or phrase ~ English translation\n- Front: Vietnamese term related to \"{topic}\"\n- Back: English translation + one example sentence"),
        "qa":       (f"Create {n} question-answer flashcards to test knowledge of \"{topic}\".\nEach card: Question ~ Answer (both in {language})\n- Front: a specific, clear exam-style question about \"{topic}\"\n- Back: a concise, accurate answer (1-3 sentences)\n- Do NOT use 'Câu 1', 'Câu 2' as front — write the actual question"),
        "antonym":  (f"Create {n} antonym/synonym flashcards for vocabulary related to \"{topic}\".\nEach card: Word ~ Antonym | Synonym: ...\n- Front: a word or term related to \"{topic}\"\n- Back: its antonym, then 'Đồng nghĩa:' followed by a synonym"),
        "code":     (f"Create {n} programming flashcards about \"{topic}\".\nEach card: Function/Concept ~ Explanation + Example\n- Front: function name, command, or programming concept from \"{topic}\"\n- Back: what it does (1 sentence) + minimal code example or syntax"),
    }
    role = roles.get(card_type, roles["term_def"])
    task = tasks.get(card_type, tasks["term_def"])
    example = examples.get(card_type, examples["term_def"])
    lang_enforce = (f"CRITICAL LANGUAGE RULE: You MUST write ALL text in {language} only.\nDo NOT use Chinese, Japanese, or any other language.\n")
    return (
        f"{role}\n\nTASK:\n{task}\n\n{lang_enforce}\n"
        f"OUTPUT FORMAT (follow exactly):\n- One flashcard per line\n- Format: Front ~ Back\n"
        f"- Output EXACTLY {n} lines\n- NO line numbers, NO bullet points, NO markdown\n"
        f"- NO blank lines between cards\n- NO intro sentence, NO closing sentence\n"
        f"- Start immediately with the first card\n\n"
        f"EXAMPLE OUTPUT (3 cards):\n{example}\n\nNow generate {n} flashcards about \"{topic}\" in {language}:\n"
    )

# ─────────────────────────────────────────────
# ─────────────────────────────────────────────
#  Dashboard HTML (Light Mode)
# ─────────────────────────────────────────────

def _dashboard_html() -> str:

    import html as _html
    running, ready = _ollama_status()
    local_ip = _local_ip()
    uptime = int((datetime.now() - datetime.fromisoformat(_stats["started_at"])).total_seconds())
    uptime_str = f"{uptime//3600}h {(uptime%3600)//60}m {uptime%60}s" if uptime >= 3600 else f"{uptime//60}m {uptime%60}s"

    status_color  = "#16a34a" if ready else ("#d97706" if running else "#dc2626")
    status_bg     = "#f0fdf4" if ready else ("#fffbeb" if running else "#fef2f2")
    status_border = "#bbf7d0" if ready else ("#fde68a" if running else "#fecaca")
    status_text   = "🟢 Model sẵn sàng" if ready else ("🟡 Model chưa load" if running else "🔴 Ollama offline")

    # ── Flashcard history rows ────────────────────────────────────────────────
    TYPE_BADGE_MAP = {
        "term_def": ("📖 Thuật ngữ",       "type-term_def"),
        "en_vi":    ("🇬🇧→🇻🇳 Anh–Việt",   "type-en_vi"),
        "vi_en":    ("🇻🇳→🇬🇧 Việt–Anh",   "type-vi_en"),
        "qa":       ("❓ Hỏi–Đáp",          "type-qa"),
        "antonym":  ("🔄 Trái/Đồng nghĩa", "type-antonym"),
        "code":     ("💻 Lập trình",        "type-code"),
    }
    fc_rows = ""
    fc_data_parts = []
    for idx, h in enumerate(_history[:50]):
        cache_badge = '<span class="badge-cache">⚡ cache</span>' if h.get("from_cache") else ""
        dur = h["duration_ms"]
        dur_str   = "—" if dur == 0 else (f"{dur/1000:.1f}s" if dur >= 1000 else f"{dur}ms")
        dur_color = "#16a34a" if dur < 5000 else ("#d97706" if dur < 15000 else "#dc2626")
        topic_safe = _html.escape(h["topic"])
        # Build per-row detail data for modal (include cached cards if available)
        ct = h.get("card_type", "term_def")
        ckey = _cache_key(h["topic"], h["cards_count"], h["language"], ct)
        cached_cards = _cache.get(ckey, {}).get("cards", [])
        detail = {
            "time": h["time"],
            "topic": h["topic"],
            "card_type": ct,
            "language": h["language"],
            "cards_count": h["cards_count"],
            "duration_ms": dur,
            "from_cache": h.get("from_cache", False),
            "client_ip": h.get("client_ip", "—"),
            "cards": cached_cards,
        }
        detail_json = json.dumps(detail, ensure_ascii=False).replace("\\", "\\\\").replace("`", "\\`")
        fc_data_parts.append(f'`{detail_json}`')
        type_label, type_cls = TYPE_BADGE_MAP.get(ct, (ct, "type-term_def"))
        type_badge = f'<span class="type-badge {type_cls}">{type_label}</span>'
        fc_rows += f"""<tr class="fc-row" onclick="openFlashcardDetail({idx})" title="Xem chi tiết">
          <td class="col-time">{h['time']}</td>
          <td class="col-topic">{topic_safe}{cache_badge}</td>
          <td class="col-type">{type_badge}</td>
          <td class="col-lang"><span class="lang-pill">{h['language']}</span></td>
          <td class="col-cards"><span class="cards-num">{h['cards_count']}</span></td>
          <td class="col-dur" style="color:{dur_color};font-weight:600">{dur_str}</td>
          <td class="col-ip">{h.get('client_ip','—')}</td>
        </tr>"""
    if not fc_rows:
        fc_rows = '<tr><td colspan="7" class="empty-row">Chưa có request nào</td></tr>'
    fc_data_js = "[" + ",\n".join(fc_data_parts) + "]"

    # ── Chat session rows + embedded JSON for modal ───────────────────────────
    sess_rows = ""
    sessions_json_parts = []
    for sid, msgs in list(_sessions.items()):
        msg_count  = len(msgs)
        user_msgs  = [m for m in msgs if m["role"] == "user"]
        preview    = _html.escape(user_msgs[-1]["content"][:80] + ("…" if len(user_msgs[-1]["content"]) > 80 else "")) if user_msgs else "(trống)"
        # Build JSON for this session (escape for JS string)
        msgs_json  = json.dumps(msgs, ensure_ascii=False).replace("\\", "\\\\").replace("`", "\\`")
        sid_short  = sid[:8] + "…"
        sessions_json_parts.append(f'"{sid}": `{msgs_json}`')
        sess_rows += f"""<tr class="sess-row" onclick="openSession('{sid}')" title="Xem chi tiết">
          <td class="col-sid"><code>{sid_short}</code></td>
          <td class="col-preview">{preview}</td>
          <td class="col-msgcount"><span class="cards-num">{msg_count}</span></td>
          <td class="col-action"><span class="view-btn">Xem →</span></td>
        </tr>"""
    if not sess_rows:
        sess_rows = '<tr><td colspan="4" class="empty-row">Chưa có session chat nào</td></tr>'

    sessions_js = "{" + ",\n".join(sessions_json_parts) + "}"

    total_sessions   = len(_sessions)
    total_chat_msgs  = sum(len(v) for v in _sessions.values())

    return f"""<!DOCTYPE html>
<html lang="vi">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width,initial-scale=1">
  <meta http-equiv="refresh" content="15">
  <title>KMAStudy AI Server</title>
  <style>
    *, *::before, *::after {{ box-sizing: border-box; margin: 0; padding: 0; }}
    body {{
      font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', 'Inter', sans-serif;
      background: #f8fafc; color: #1e293b; padding: 32px; min-height: 100vh;
    }}
    /* ── Header ── */
    .header {{ margin-bottom: 8px; display:flex; align-items:center; gap:14px; }}
    .header-icon {{ width:44px;height:44px;background:linear-gradient(135deg,#3f665c,#5e5b7a);border-radius:14px;display:flex;align-items:center;justify-content:center;font-size:22px; }}
    .header h1 {{ font-size:1.5rem;font-weight:800;color:#1e293b;letter-spacing:-.5px; }}
    .header .sub {{ color:#64748b;font-size:.82rem;margin-top:2px; }}
    .header .sub a {{ color:#3f665c;text-decoration:none;font-weight:600; }}
    .header .sub a:hover {{ text-decoration:underline; }}
    /* ── Status ── */
    .status-badge {{
      display:inline-flex;align-items:center;gap:8px;
      padding:8px 16px;border-radius:99px;font-size:.82rem;font-weight:700;
      border:1.5px solid {status_border};color:{status_color};
      background:{status_bg};margin:16px 0 24px;
    }}
    /* ── Stats grid ── */
    .stats {{ display:grid;grid-template-columns:repeat(6,1fr);gap:12px;margin-bottom:20px; }}
    @media(max-width:1000px){{ .stats{{ grid-template-columns:repeat(3,1fr); }} }}
    @media(max-width:600px) {{ .stats{{ grid-template-columns:repeat(2,1fr); }} }}
    .stat {{ background:#fff;border:1px solid #e2e8f0;border-radius:14px;padding:18px 16px;box-shadow:0 1px 3px rgba(0,0,0,.06); }}
    .stat-val {{ font-size:1.8rem;font-weight:800;color:#3f665c;line-height:1; }}
    .stat-lbl {{ font-size:.7rem;color:#94a3b8;margin-top:5px;text-transform:uppercase;letter-spacing:.6px;font-weight:600; }}
    /* ── Card ── */
    .card {{ background:#fff;border:1px solid #e2e8f0;border-radius:16px;overflow:hidden;margin-bottom:16px;box-shadow:0 1px 4px rgba(0,0,0,.06); }}
    .card-head {{ padding:14px 20px;border-bottom:1px solid #f1f5f9;font-size:.82rem;font-weight:700;color:#475569;display:flex;align-items:center;gap:8px;background:#f8fafc; }}
    /* ── Connection ── */
    .conn-grid {{ display:grid;grid-template-columns:1fr 1fr; }}
    @media(max-width:700px){{ .conn-grid{{ grid-template-columns:1fr; }} }}
    .conn-row {{ padding:13px 20px;border-bottom:1px solid #f1f5f9;font-size:.84rem;display:flex;justify-content:space-between;align-items:center;gap:12px; }}
    .conn-row:last-child {{ border-bottom:none; }}
    .conn-key {{ color:#64748b;font-size:.78rem;font-weight:600;white-space:nowrap; }}
    .conn-val {{ color:#3f665c;font-family:'SF Mono','Fira Code',monospace;font-size:.82rem;font-weight:700;background:#f0fdf4;padding:3px 8px;border-radius:6px; }}
    .conn-note {{ padding:10px 20px;font-size:.76rem;color:#94a3b8;border-top:1px solid #f1f5f9;background:#fffbeb; }}
    /* ── Table shared ── */
    .table-wrap {{ overflow-x:auto; }}
    table {{ width:100%;border-collapse:collapse;font-size:.83rem; }}
    thead tr {{ background:#f8fafc; }}
    th {{ padding:12px 16px;text-align:left;font-size:.7rem;font-weight:700;color:#94a3b8;text-transform:uppercase;letter-spacing:.7px;white-space:nowrap;border-bottom:1px solid #e2e8f0; }}
    td {{ padding:11px 16px;border-bottom:1px solid #f1f5f9;vertical-align:middle; }}
    tbody tr:last-child td {{ border-bottom:none; }}
    tbody tr:hover td {{ background:#f8fafc; }}
    /* ── Flashcard table cols ── */
    .col-time  {{ color:#94a3b8;font-size:.78rem;white-space:nowrap;width:140px; }}
    .col-topic {{ color:#1e293b;font-weight:600;word-break:break-word;max-width:320px; }}
    .col-type  {{ width:110px; }}
    .col-lang  {{ width:90px; }}
    .col-cards {{ width:60px;text-align:center; }}
    .col-dur   {{ width:80px;text-align:right;font-family:monospace;font-size:.8rem; }}
    .col-ip    {{ color:#cbd5e1;font-size:.76rem;font-family:monospace;width:120px; }}
    /* ── Session table cols ── */
    .col-sid      {{ width:120px;color:#64748b;font-size:.78rem; }}
    .col-preview  {{ color:#1e293b;font-weight:500;word-break:break-word; }}
    .col-msgcount {{ width:70px;text-align:center; }}
    .col-action   {{ width:70px;text-align:center; }}
    .fc-row {{ cursor:pointer; }}
    .fc-row:hover td {{ background:#eff6ff !important; }}
    .sess-row {{ cursor:pointer; }}
    .sess-row:hover td {{ background:#f0fdf4 !important; }}
    .view-btn {{ display:inline-block;padding:3px 10px;border-radius:99px;background:#dcfce7;color:#15803d;font-size:.75rem;font-weight:700; }}
    /* ── Pills / badges ── */
    .badge-cache {{ display:inline-block;padding:1px 7px;border-radius:99px;font-size:.68rem;font-weight:700;background:#ede9fe;color:#7c3aed;margin-left:6px;vertical-align:middle; }}
    .lang-pill   {{ display:inline-block;padding:3px 10px;border-radius:99px;font-size:.72rem;background:#f1f5f9;color:#475569;border:1px solid #e2e8f0;font-weight:600; }}
    /* ── Type badges (per card_type) ── */
    .type-badge    {{ display:inline-flex;align-items:center;gap:4px;padding:3px 9px;border-radius:99px;font-size:.7rem;font-weight:700;white-space:nowrap; }}
    .type-term_def {{ background:#dbeafe;color:#1d4ed8; }}
    .type-en_vi    {{ background:#dcfce7;color:#15803d; }}
    .type-vi_en    {{ background:#d1fae5;color:#065f46; }}
    .type-qa       {{ background:#fef9c3;color:#854d0e; }}
    .type-antonym  {{ background:#fce7f3;color:#9d174d; }}
    .type-code     {{ background:#1e293b;color:#e2e8f0; }}
    .cards-num   {{ display:inline-block;min-width:28px;text-align:center;padding:2px 8px;border-radius:6px;font-weight:800;font-size:.85rem;background:#f0fdf4;color:#16a34a; }}
    .empty-row   {{ text-align:center;color:#cbd5e1;padding:40px 16px !important;font-size:.85rem; }}
    /* ── Endpoint grid ── */
    .endpoint-grid {{ display:grid;grid-template-columns:1fr 1fr;gap:0; }}
    .endpoint-item {{ padding:12px 20px;border-bottom:1px solid #f1f5f9;display:flex;align-items:center;gap:10px; }}
    .endpoint-item:nth-child(odd) {{ border-right:1px solid #f1f5f9; }}
    .method-badge {{ padding:2px 8px;border-radius:6px;font-size:.7rem;font-weight:800;letter-spacing:.5px; }}
    .get  {{ background:#dbeafe;color:#1d4ed8; }}
    .post {{ background:#dcfce7;color:#15803d; }}
    .del  {{ background:#fee2e2;color:#dc2626; }}
    .endpoint-path {{ font-family:monospace;font-size:.82rem;color:#1e293b;font-weight:600; }}
    .endpoint-desc {{ font-size:.75rem;color:#94a3b8;margin-top:2px; }}
    /* ── Modal overlay ── */
    .modal-overlay {{
      display:none;position:fixed;inset:0;background:rgba(15,23,42,.45);
      z-index:1000;align-items:center;justify-content:center;padding:24px;
    }}
    .modal-overlay.open {{ display:flex; }}
    .modal {{
      background:#fff;border-radius:20px;width:100%;max-width:640px;
      max-height:85vh;display:flex;flex-direction:column;
      box-shadow:0 24px 60px rgba(0,0,0,.18);overflow:hidden;
    }}
    .modal-header {{
      padding:18px 20px;border-bottom:1px solid #f1f5f9;
      display:flex;align-items:center;gap:12px;background:#f8fafc;
    }}
    .modal-title {{ font-size:1rem;font-weight:700;color:#1e293b;flex:1; }}
    .modal-sub   {{ font-size:.75rem;color:#94a3b8;margin-top:2px; }}
    .modal-close {{
      width:32px;height:32px;border-radius:99px;border:none;
      background:#f1f5f9;color:#64748b;font-size:1.1rem;cursor:pointer;
      display:flex;align-items:center;justify-content:center;
    }}
    .modal-close:hover {{ background:#e2e8f0; }}
    /* ── Flashcard detail modal ── */
    .fc-detail-grid {{ display:grid;grid-template-columns:1fr 1fr;gap:12px;padding:20px; }}
    @media(max-width:500px){{ .fc-detail-grid{{ grid-template-columns:1fr; }} }}
    .fc-detail-card {{
      border-radius:14px;padding:18px 16px;min-height:80px;
      display:flex;flex-direction:column;gap:6px;
    }}
    .fc-detail-card.front {{
      background:linear-gradient(135deg,#f0fdf4,#dcfce7);
      border:1.5px solid #bbf7d0;
    }}
    .fc-detail-card.back {{
      background:linear-gradient(135deg,#eff6ff,#dbeafe);
      border:1.5px solid #bfdbfe;
    }}
    .fc-detail-label {{
      font-size:.68rem;font-weight:800;text-transform:uppercase;letter-spacing:.8px;
      color:#94a3b8;margin-bottom:2px;
    }}
    .fc-detail-card.front .fc-detail-label {{ color:#16a34a; }}
    .fc-detail-card.back  .fc-detail-label {{ color:#2563eb; }}
    .fc-detail-text {{
      font-size:.92rem;font-weight:600;color:#1e293b;line-height:1.5;word-break:break-word;
    }}
    .fc-meta-grid {{
      display:grid;grid-template-columns:repeat(3,1fr);gap:8px;
      padding:0 20px 20px;
    }}
    .fc-meta-item {{
      background:#f8fafc;border:1px solid #e2e8f0;border-radius:10px;
      padding:10px 12px;
    }}
    .fc-meta-key {{ font-size:.68rem;color:#94a3b8;font-weight:700;text-transform:uppercase;letter-spacing:.6px; }}
    .fc-meta-val {{ font-size:.85rem;font-weight:700;color:#1e293b;margin-top:3px; }}
    .fc-topic-banner {{
      margin:16px 20px 0;padding:14px 16px;
      background:linear-gradient(135deg,#f8fafc,#f1f5f9);
      border:1px solid #e2e8f0;border-radius:12px;
    }}
    .fc-topic-banner .lbl {{ font-size:.68rem;color:#94a3b8;font-weight:700;text-transform:uppercase;letter-spacing:.6px; }}
    .fc-topic-banner .val {{ font-size:1rem;font-weight:800;color:#1e293b;margin-top:4px; }}
    /* ── Cards list inside fc modal ── */
    .fc-cards-section {{ padding:0 20px 20px; }}
    .fc-cards-btn {{
      width:100%;padding:10px 16px;border-radius:10px;border:1.5px dashed #bfdbfe;
      background:#eff6ff;color:#1d4ed8;font-size:.82rem;font-weight:700;cursor:pointer;
      display:flex;align-items:center;justify-content:center;gap:8px;margin-bottom:12px;
      transition:background .15s;
    }}
    .fc-cards-btn:hover {{ background:#dbeafe; }}
    .fc-cards-btn.loaded {{ border-style:solid;background:#fff;color:#1e293b;justify-content:space-between; }}
    .fc-card-item {{
      border:1px solid #e2e8f0;border-radius:12px;overflow:hidden;margin-bottom:8px;
    }}
    .fc-card-item-front {{
      padding:10px 14px;background:linear-gradient(90deg,#f0fdf4,#f8fafc);
      font-size:.85rem;font-weight:700;color:#1e293b;border-bottom:1px solid #e2e8f0;
      display:flex;align-items:center;gap:8px;
    }}
    .fc-card-item-back {{
      padding:10px 14px;background:#fff;
      font-size:.83rem;color:#475569;line-height:1.5;
    }}
    .fc-card-num {{
      min-width:22px;height:22px;border-radius:6px;background:#dcfce7;color:#15803d;
      font-size:.7rem;font-weight:800;display:inline-flex;align-items:center;justify-content:center;flex-shrink:0;
    }}
    /* ── Chat messages inside modal ── */
    .chat-body {{
      flex:1;overflow-y:auto;padding:20px 16px;
      display:flex;flex-direction:column;gap:12px;background:#f8fafc;
    }}
    .msg-row {{ display:flex;align-items:flex-end;gap:8px; }}
    .msg-row.user  {{ flex-direction:row-reverse; }}
    .avatar {{
      width:30px;height:30px;border-radius:99px;flex-shrink:0;
      display:flex;align-items:center;justify-content:center;font-size:14px;
    }}
    .avatar.bot  {{ background:linear-gradient(135deg,#3f665c,#5e5b7a); }}
    .avatar.user {{ background:#dcfce7;color:#15803d;font-weight:700;font-size:.75rem; }}
    .bubble {{
      max-width:78%;padding:10px 14px;border-radius:16px;
      font-size:.85rem;line-height:1.55;word-break:break-word;
    }}
    .bubble.bot  {{ background:#fff;border:1px solid #e2e8f0;border-bottom-left-radius:4px;color:#1e293b; }}
    .bubble.user {{ background:#3f665c;color:#fff;border-bottom-right-radius:4px; }}
    .bubble.error {{ background:#fef2f2;border:1px solid #fecaca;color:#dc2626; }}
  </style>
</head>
<body>
  <div class="header">
    <div class="header-icon">🤖</div>
    <div>
      <h1>KMAStudy AI Server</h1>
      <p class="sub">
        Tự động làm mới mỗi 15s &nbsp;·&nbsp;
        <a href="/docs">API Docs</a> &nbsp;·&nbsp;
        <a href="/history">History JSON</a> &nbsp;·&nbsp;
        <a href="/sessions">Sessions JSON</a> &nbsp;·&nbsp;
        Model: <strong style="color:#3f665c">{MODEL}</strong>
      </p>
    </div>
  </div>

  <div class="status-badge">{status_text}</div>

  <div class="stats">
    <div class="stat"><div class="stat-val">{_stats['requests']}</div><div class="stat-lbl">Flashcard Requests</div></div>
    <div class="stat"><div class="stat-val">{_stats['cards_generated']}</div><div class="stat-lbl">Cards Generated</div></div>
    <div class="stat"><div class="stat-val">{_stats['cache_hits']}</div><div class="stat-lbl">Cache Hits</div></div>
    <div class="stat"><div class="stat-val">{len(_cache)}</div><div class="stat-lbl">Cached Topics</div></div>
    <div class="stat"><div class="stat-val">{total_sessions}</div><div class="stat-lbl">Chat Sessions</div></div>
    <div class="stat"><div class="stat-val">{uptime_str}</div><div class="stat-lbl">Uptime</div></div>
  </div>

  <!-- Connection -->
  <div class="card">
    <div class="card-head">🌐 Kết nối</div>
    <div class="conn-grid">
      <div class="conn-row"><span class="conn-key">Localhost</span><span class="conn-val">http://localhost:{PORT}</span></div>
      <div class="conn-row"><span class="conn-key">LAN · Điện thoại thật</span><span class="conn-val">http://{local_ip}:{PORT}</span></div>
      <div class="conn-row"><span class="conn-key">Android Emulator</span><span class="conn-val">http://10.0.2.2:{PORT}</span></div>
      <div class="conn-row"><span class="conn-key">API Documentation</span><span class="conn-val"><a href="/docs" style="color:#3f665c">localhost:{PORT}/docs</a></span></div>
    </div>
    <div class="conn-note">💡 Điện thoại thật dùng địa chỉ LAN. Nếu không kết nối được → Windows Firewall → Allow port {PORT}.</div>
  </div>

  <!-- Endpoints -->
  <div class="card">
    <div class="card-head">📡 API Endpoints</div>
    <div class="endpoint-grid">
      <div class="endpoint-item"><span class="method-badge get">GET</span><div><div class="endpoint-path">/status</div><div class="endpoint-desc">Kiểm tra trạng thái server & model</div></div></div>
      <div class="endpoint-item"><span class="method-badge post">POST</span><div><div class="endpoint-path">/generate</div><div class="endpoint-desc">Tạo flashcard từ chủ đề</div></div></div>
      <div class="endpoint-item"><span class="method-badge post">POST</span><div><div class="endpoint-path">/chat</div><div class="endpoint-desc">Chat AI với session lưu trữ</div></div></div>
      <div class="endpoint-item"><span class="method-badge get">GET</span><div><div class="endpoint-path">/chat/{{session_id}}</div><div class="endpoint-desc">Lấy lịch sử chat của session</div></div></div>
      <div class="endpoint-item"><span class="method-badge del">DEL</span><div><div class="endpoint-path">/chat/{{session_id}}</div><div class="endpoint-desc">Xóa session chat</div></div></div>
      <div class="endpoint-item"><span class="method-badge del">DEL</span><div><div class="endpoint-path">/cache</div><div class="endpoint-desc">Xóa cache flashcard</div></div></div>
    </div>
  </div>

  <!-- Flashcard history -->
  <div class="card">
    <div class="card-head">🃏 Lịch sử Flashcard <span style="color:#cbd5e1;font-weight:400;margin-left:6px">(50 gần nhất)</span></div>
    <div class="table-wrap">
      <table>
        <thead><tr>
          <th>Thời gian</th><th>Chủ đề</th><th>Loại</th>
          <th>Ngôn ngữ</th><th style="text-align:center">Thẻ</th>
          <th style="text-align:right">Thời lượng</th><th>Client IP</th>
        </tr></thead>
        <tbody>{fc_rows}</tbody>
      </table>
    </div>
  </div>

  <!-- Chat sessions -->
  <div class="card">
    <div class="card-head">💬 Chat Sessions <span style="color:#cbd5e1;font-weight:400;margin-left:6px">({total_sessions} sessions · {total_chat_msgs} tin nhắn) — bấm để xem chi tiết</span></div>
    <div class="table-wrap">
      <table>
        <thead><tr>
          <th>Session ID</th><th>Tin nhắn cuối</th>
          <th style="text-align:center">Số tin</th><th style="text-align:center">Chi tiết</th>
        </tr></thead>
        <tbody>{sess_rows}</tbody>
      </table>
    </div>
  </div>

  <!-- Flashcard Detail Modal -->
  <div class="modal-overlay" id="fc-modal" onclick="closeFcOnOverlay(event)">
    <div class="modal" style="max-width:560px">
      <div class="modal-header">
        <div style="font-size:22px">🃏</div>
        <div style="flex:1">
          <div class="modal-title" id="fc-modal-title">Chi tiết Flashcard Request</div>
          <div class="modal-sub" id="fc-modal-sub"></div>
        </div>
        <button class="modal-close" onclick="closeFcModal()">✕</button>
      </div>
      <div id="fc-modal-body" style="overflow-y:auto;max-height:calc(85vh - 80px)">
        <!-- filled by JS -->
      </div>
    </div>
  </div>

  <!-- Chat Session Modal -->
  <div class="modal-overlay" id="modal" onclick="closeOnOverlay(event)">
    <div class="modal">
      <div class="modal-header">
        <div style="font-size:22px">🤖</div>
        <div style="flex:1">
          <div class="modal-title" id="modal-title">Chat Session</div>
          <div class="modal-sub" id="modal-sub"></div>
        </div>
        <button class="modal-close" onclick="closeModal()">✕</button>
      </div>
      <div class="chat-body" id="chat-body"></div>
    </div>
  </div>

  <script>
    const SESSIONS = {sessions_js};
    const FC_DATA  = {fc_data_js};

    // ── Flashcard detail modal ────────────────────────────────────────────
    const TYPE_LABELS = {{
      term_def: {{ label: '📖 Thuật ngữ', cls: 'type-term_def' }},
      en_vi:    {{ label: '🇬🇧→🇻🇳 Anh–Việt', cls: 'type-en_vi' }},
      vi_en:    {{ label: '🇻🇳→🇬🇧 Việt–Anh', cls: 'type-vi_en' }},
      qa:       {{ label: '❓ Hỏi–Đáp', cls: 'type-qa' }},
      antonym:  {{ label: '🔄 Trái/Đồng nghĩa', cls: 'type-antonym' }},
      code:     {{ label: '💻 Lập trình', cls: 'type-code' }},
    }};

    function openFlashcardDetail(idx) {{
      const raw = FC_DATA[idx];
      if (!raw) return;
      const d = JSON.parse(raw);
      const body  = document.getElementById('fc-modal-body');
      const title = document.getElementById('fc-modal-title');
      const sub   = document.getElementById('fc-modal-sub');

      title.textContent = 'Chi tiết Flashcard Request';
      sub.textContent   = d.time;

      const dur = d.duration_ms;
      const durStr   = dur === 0 ? '⚡ cache' : (dur >= 1000 ? (dur/1000).toFixed(1)+'s' : dur+'ms');
      const cacheStr = d.from_cache ? '⚡ Có (cache)' : '❌ Không';
      const tinfo    = TYPE_LABELS[d.card_type] || {{ label: d.card_type, cls: 'type-term_def' }};
      const hasCards = d.cards && d.cards.length > 0;

      body.innerHTML = `
        <div class="fc-topic-banner">
          <div class="lbl">Chủ đề</div>
          <div class="val">${{escHtml(d.topic)}}</div>
        </div>
        <div class="fc-meta-grid">
          <div class="fc-meta-item">
            <div class="fc-meta-key">Loại thẻ</div>
            <div class="fc-meta-val" style="margin-top:5px"><span class="type-badge ${{tinfo.cls}}">${{tinfo.label}}</span></div>
          </div>
          <div class="fc-meta-item">
            <div class="fc-meta-key">Ngôn ngữ</div>
            <div class="fc-meta-val">${{escHtml(d.language)}}</div>
          </div>
          <div class="fc-meta-item">
            <div class="fc-meta-key">Số thẻ tạo</div>
            <div class="fc-meta-val" style="color:#16a34a">${{d.cards_count}}</div>
          </div>
          <div class="fc-meta-item">
            <div class="fc-meta-key">Thời lượng</div>
            <div class="fc-meta-val">${{durStr}}</div>
          </div>
          <div class="fc-meta-item">
            <div class="fc-meta-key">Cache</div>
            <div class="fc-meta-val">${{cacheStr}}</div>
          </div>
          <div class="fc-meta-item">
            <div class="fc-meta-key">Client IP</div>
            <div class="fc-meta-val" style="font-family:monospace;font-size:.78rem">${{escHtml(d.client_ip)}}</div>
          </div>
        </div>
        <div class="fc-cards-section">
          ${{hasCards
            ? `<button class="fc-cards-btn" id="fc-toggle-btn" onclick="toggleCards(${{idx}})">
                 <span>🃏 Xem ${{d.cards.length}} thẻ đã tạo</span><span>▼</span>
               </button>
               <div id="fc-cards-list" style="display:none"></div>`
            : `<div style="text-align:center;padding:12px;font-size:.8rem;color:#94a3b8;background:#f8fafc;border-radius:10px;border:1px dashed #e2e8f0">
                 ⚠️ Không tìm thấy thẻ trong cache (có thể đã bị xóa)
               </div>`
          }}
        </div>
      `;

      document.getElementById('fc-modal').classList.add('open');
    }}

    function toggleCards(idx) {{
      const raw = FC_DATA[idx];
      if (!raw) return;
      const d = JSON.parse(raw);
      const list = document.getElementById('fc-cards-list');
      const btn  = document.getElementById('fc-toggle-btn');
      if (!list) return;

      if (list.style.display === 'none') {{
        // Build cards HTML
        let html = '';
        d.cards.forEach((c, i) => {{
          html += `<div class="fc-card-item">
            <div class="fc-card-item-front">
              <span class="fc-card-num">${{i+1}}</span>
              ${{escHtml(c.front)}}
            </div>
            <div class="fc-card-item-back">${{escHtml(c.back)}}</div>
          </div>`;
        }});
        list.innerHTML = html;
        list.style.display = 'block';
        btn.classList.add('loaded');
        btn.innerHTML = `<span>🃏 ${{d.cards.length}} thẻ</span><span>▲ Thu gọn</span>`;
      }} else {{
        list.style.display = 'none';
        btn.classList.remove('loaded');
        btn.innerHTML = `<span>🃏 Xem ${{d.cards.length}} thẻ đã tạo</span><span>▼</span>`;
      }}
    }}

    function closeFcModal() {{
      document.getElementById('fc-modal').classList.remove('open');
    }}

    function closeFcOnOverlay(e) {{
      if (e.target === document.getElementById('fc-modal')) closeFcModal();
    }}

    // ── Chat session modal ────────────────────────────────────────────────

    // ── Chat session modal ────────────────────────────────────────────────
    function openSession(sid) {{
      const raw = SESSIONS[sid];
      if (!raw) return;
      const msgs = JSON.parse(raw);
      const body = document.getElementById('chat-body');
      const title = document.getElementById('modal-title');
      const sub   = document.getElementById('modal-sub');

      title.textContent = 'Session: ' + sid.slice(0,8) + '…';
      sub.textContent   = msgs.length + ' tin nhắn';

      body.innerHTML = '';
      msgs.forEach(m => {{
        const isUser = m.role === 'user';
        const row = document.createElement('div');
        row.className = 'msg-row' + (isUser ? ' user' : '');

        const av = document.createElement('div');
        av.className = 'avatar ' + (isUser ? 'user' : 'bot');
        av.textContent = isUser ? 'U' : '🤖';

        const bub = document.createElement('div');
        bub.className = 'bubble ' + (isUser ? 'user' : 'bot');
        // Render newlines as <br>
        bub.innerHTML = escHtml(m.content).replace(/\\n/g, '<br>');

        row.appendChild(av);
        row.appendChild(bub);
        body.appendChild(row);
      }});

      document.getElementById('modal').classList.add('open');
      // Scroll to bottom
      setTimeout(() => {{ body.scrollTop = body.scrollHeight; }}, 50);
    }}

    function closeModal() {{
      document.getElementById('modal').classList.remove('open');
    }}

    function closeOnOverlay(e) {{
      if (e.target === document.getElementById('modal')) closeModal();
    }}

    function escHtml(s) {{
      return s.replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;').replace(/"/g,'&quot;');
    }}

    document.addEventListener('keydown', e => {{ if (e.key === 'Escape') {{ closeModal(); closeFcModal(); }} }});
  </script>
</body>
</html>"""


@app.get("/", response_class=HTMLResponse, include_in_schema=False)
def dashboard():
    return _dashboard_html()

@app.get("/history", include_in_schema=False)
def get_history():
    return _history

@app.get("/sessions", include_in_schema=False)
def get_sessions():
    return {sid: {"message_count": len(msgs), "messages": msgs} for sid, msgs in _sessions.items()}

@app.delete("/cache", include_in_schema=False)
def clear_cache():
    _cache.clear()
    _save_json(CACHE_FILE, _cache)
    return {"cleared": True}

@app.get("/status", response_model=StatusResponse)
def get_status():
    running, ready = _ollama_status()
    uptime = int((datetime.now() - datetime.fromisoformat(_stats["started_at"])).total_seconds())
    if not running:
        return StatusResponse(status="ollama_offline", model="", uptime_seconds=uptime,
            message="Ollama chưa chạy. Tải tại: https://ollama.com/download", stats=_stats)
    if not ready:
        return StatusResponse(status="no_model", model="", uptime_seconds=uptime,
            message=f"Chạy lệnh: ollama pull {MODEL}", stats=_stats)
    return StatusResponse(status="ready", model=MODEL, uptime_seconds=uptime,
        message="Sẵn sàng!", stats=_stats, lan_ip=_local_ip())

@app.post("/generate", response_model=GenerateResponse)
def generate(req: GenerateRequest, request: Request):
    client_ip = request.client.host if request else "unknown"
    running, ready = _ollama_status()
    if not running: raise HTTPException(503, detail="Ollama offline")
    if not ready:   raise HTTPException(503, detail=f"Model chưa tải. Chạy: ollama pull {MODEL}")
    _stats["requests"] += 1

    key = _cache_key(req.topic, req.count, req.language, req.card_type)
    if key in _cache:
        _stats["cache_hits"] += 1
        cached = _cache[key]
        _record_history(req.topic, req.count, req.language, len(cached["cards"]), 0, True, client_ip, req.card_type)
        return GenerateResponse(cards=[Card(**c) for c in cached["cards"]],
            error_lines=cached["error_lines"], raw_output=cached["raw_output"],
            duration_ms=0, model=MODEL, from_cache=True)

    prompt = _build_prompt(req.topic, req.count, req.language, req.card_type)
    t0 = time.time()
    try:
        resp = requests.post(f"{OLLAMA_BASE}/api/generate",
            json={"model": MODEL, "prompt": prompt, "stream": False,
                  "options": {"temperature": 0.4, "top_k": 40, "top_p": 0.9,
                              "num_predict": MAX_TOKENS, "stop": ["---", "\n\n\n"]}},
            timeout=300)
        resp.raise_for_status()
        raw = resp.json().get("response", "").strip()
    except requests.Timeout:
        raise HTTPException(504, detail="Model timeout — giảm số lượng thẻ và thử lại")
    except Exception as e:
        raise HTTPException(500, detail=f"Generation failed: {e}")

    duration = int((time.time() - t0) * 1000)
    cards, errors = _parse(raw)
    _stats["cards_generated"] += len(cards)
    _cache[key] = {"cards": [c.dict() for c in cards], "error_lines": errors, "raw_output": raw}
    _save_json(CACHE_FILE, _cache)
    _record_history(req.topic, req.count, req.language, len(cards), duration, False, client_ip, req.card_type)
    return GenerateResponse(cards=cards, error_lines=errors, raw_output=raw,
                            duration_ms=duration, model=MODEL, from_cache=False)

# ── Chat endpoints ────────────────────────────────────────────────────────────

@app.post("/chat", response_model=ChatResponse)
def chat(req: ChatRequest, request: Request):
    running, ready = _ollama_status()
    if not running: raise HTTPException(503, detail="Ollama offline")
    if not ready:   raise HTTPException(503, detail=f"Model chưa tải. Chạy: ollama pull {MODEL}")

    session_id = req.session_id
    history = _get_session(session_id)

    # Build messages for Ollama chat API
    system_prompt = _build_chat_system_prompt(req.language)
    messages = [{"role": "system", "content": system_prompt}]
    messages.extend(history)
    messages.append({"role": "user", "content": req.message})

    t0 = time.time()
    try:
        resp = requests.post(f"{OLLAMA_BASE}/api/chat",
            json={"model": MODEL, "messages": messages, "stream": False,
                  "options": {"temperature": 0.7, "top_k": 40, "top_p": 0.9, "num_predict": 1024}},
            timeout=120)
        resp.raise_for_status()
        reply = resp.json().get("message", {}).get("content", "").strip()
    except requests.Timeout:
        raise HTTPException(504, detail="Model timeout")
    except Exception as e:
        raise HTTPException(500, detail=f"Chat failed: {e}")

    duration = int((time.time() - t0) * 1000)

    # Save to session
    history.append({"role": "user", "content": req.message})
    history.append({"role": "assistant", "content": reply})
    _save_session(session_id, history)
    _stats["chat_messages"] = _stats.get("chat_messages", 0) + 2

    return ChatResponse(session_id=session_id, reply=reply,
                        duration_ms=duration, model=MODEL,
                        history_length=len(_sessions.get(session_id, [])))

@app.get("/chat/{session_id}")
def get_chat_history(session_id: str):
    return {"session_id": session_id, "messages": _get_session(session_id)}

@app.delete("/chat/{session_id}")
def delete_chat_session(session_id: str):
    if session_id in _sessions:
        del _sessions[session_id]
        _save_json(SESSIONS_FILE, _sessions)
    return {"deleted": True, "session_id": session_id}

# ─────────────────────────────────────────────
#  Startup banner
# ─────────────────────────────────────────────

def _print_banner():
    running, ready = _ollama_status()
    local_ip = _local_ip()
    lines = [
        "",
        "  ╔══════════════════════════════════════════╗",
        "  ║      KMAStudy AI Server  v4.0.0          ║",
        "  ╚══════════════════════════════════════════╝",
        "",
        f"  {'✓  ' + MODEL + '  sẵn sàng' if ready else ('⚠  Ollama chưa chạy → https://ollama.com/download' if not running else f'⚠  Chạy: ollama pull {MODEL}')}",
        "",
        f"  🌐  Localhost        →  http://localhost:{PORT}",
        f"  🌐  LAN (real phone) →  http://{local_ip}:{PORT}",
        f"  📱  Emulator         →  http://10.0.2.2:{PORT}",
        f"  📖  Dashboard        →  http://localhost:{PORT}",
        f"  📖  API docs         →  http://localhost:{PORT}/docs",
        "",
        f"  💬  Chat API         →  POST /chat",
        f"  🃏  Flashcard API    →  POST /generate",
        "",
        f"  📦  Cache: {len(_cache)} topics  |  Sessions: {len(_sessions)}  |  History: {len(_history)} requests",
        "",
        "  Nhấn Ctrl+C để dừng",
        "",
    ]
    print("\n".join(lines))

if __name__ == "__main__":
    _print_banner()
    uvicorn.run(app, host=HOST, port=PORT, log_level="warning")
