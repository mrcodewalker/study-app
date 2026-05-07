"""
╔══════════════════════════════════════════════════════════╗
║           AI Flashcard Server  •  Powered by Ollama      ║
╚══════════════════════════════════════════════════════════╝
"""

import re, time, socket, json, hashlib, requests, uvicorn
from datetime import datetime
from pathlib import Path
from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import HTMLResponse
from pydantic import BaseModel, Field

# ─────────────────────────────────────────────
#  Config
# ─────────────────────────────────────────────

OLLAMA_BASE  = "http://localhost:11434"
MODEL        = "qwen2.5:7b"
HOST         = "0.0.0.0"
PORT         = 8000
MAX_TOKENS   = -1    # -1 = unlimited, let model finish naturally
HISTORY_FILE = Path(__file__).parent / "history.json"
CACHE_FILE   = Path(__file__).parent / "cache.json"

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

# In-memory state (loaded from disk on startup)
_cache: dict   = _load_json(CACHE_FILE, {})   # key → GenerateResponse dict
_history: list = _load_json(HISTORY_FILE, []) # list of request records
_stats = {
    "requests": len(_history),
    "cards_generated": sum(h.get("cards_count", 0) for h in _history),
    "cache_hits": 0,
    "started_at": datetime.now().isoformat(),
}

# ─────────────────────────────────────────────
#  App
# ─────────────────────────────────────────────

app = FastAPI(
    title="AI Flashcard Server",
    description="Local LLM server for StudyApp — generates flashcards via Ollama.",
    version="3.0.0",
)
app.add_middleware(CORSMiddleware, allow_origins=["*"], allow_methods=["*"], allow_headers=["*"])

# ─────────────────────────────────────────────
#  Schemas
# ─────────────────────────────────────────────

class GenerateRequest(BaseModel):
    topic: str    = Field(..., min_length=1, max_length=200)
    count: int    = Field(10, ge=1, le=50)
    language: str = Field("Vietnamese")
    card_type: str = Field("term_def")  # term_def | en_vi | vi_en | qa | antonym | code

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

def _build_prompt(topic: str, count: int, language: str, card_type: str = "term_def") -> str:
    count = max(1, min(count, 50))
    n = count

    # ── Few-shot examples per type ────────────────────────────────────────────
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

    # ── System role per type ──────────────────────────────────────────────────
    roles = {
        "term_def": "You are an expert educator specializing in creating precise academic flashcards.",
        "en_vi":    "You are a bilingual English-Vietnamese language teacher.",
        "vi_en":    "You are a bilingual Vietnamese-English language teacher.",
        "qa":       "You are an expert exam question writer for academic subjects.",
        "antonym":  "You are a linguistics expert specializing in vocabulary relationships.",
        "code":     "You are a senior software engineer and programming instructor.",
    }

    # ── Task description per type ─────────────────────────────────────────────
    tasks = {
        "term_def": (
            f"Create {n} flashcards for the topic \"{topic}\".\n"
            f"Each card: Technical Term ~ Definition in {language}\n"
            f"- Front: exact technical term, concept, or keyword from \"{topic}\"\n"
            f"- Back: precise, concise definition (1-2 sentences max) in {language}\n"
            f"- Cover the most important concepts of \"{topic}\"\n"
            f"- Do NOT generate generic or off-topic cards"
        ),
        "en_vi": (
            f"Create {n} English-Vietnamese vocabulary flashcards for the topic \"{topic}\".\n"
            f"Each card: English word or phrase ~ Vietnamese meaning\n"
            f"- Front: English term commonly used in \"{topic}\"\n"
            f"- Back: Vietnamese translation + short usage note or example if helpful\n"
            f"- Focus on domain-specific vocabulary of \"{topic}\""
        ),
        "vi_en": (
            f"Create {n} Vietnamese-English vocabulary flashcards for the topic \"{topic}\".\n"
            f"Each card: Vietnamese word or phrase ~ English translation\n"
            f"- Front: Vietnamese term related to \"{topic}\"\n"
            f"- Back: English translation + one example sentence using the word\n"
            f"- Focus on domain-specific vocabulary of \"{topic}\""
        ),
        "qa": (
            f"Create {n} question-answer flashcards to test knowledge of \"{topic}\".\n"
            f"Each card: Question ~ Answer (both in {language})\n"
            f"- Front: a specific, clear exam-style question about \"{topic}\"\n"
            f"  (e.g. 'What is...?', 'How does...work?', 'What is the difference between...?')\n"
            f"- Back: a concise, accurate answer (1-3 sentences)\n"
            f"- Questions must test real understanding, not just memorization\n"
            f"- Do NOT use 'Câu 1', 'Câu 2' as front — write the actual question"
        ),
        "antonym": (
            f"Create {n} antonym/synonym flashcards for vocabulary related to \"{topic}\".\n"
            f"Each card: Word ~ Antonym | Synonym: ...\n"
            f"- Front: a word or term related to \"{topic}\"\n"
            f"- Back: its antonym, then 'Đồng nghĩa:' followed by a synonym\n"
            f"- All words must be relevant to the domain of \"{topic}\""
        ),
        "code": (
            f"Create {n} programming flashcards about \"{topic}\".\n"
            f"Each card: Function/Concept ~ Explanation + Example\n"
            f"- Front: function name, command, algorithm, or programming concept from \"{topic}\"\n"
            f"- Back: what it does (1 sentence) + minimal code example or syntax\n"
            f"- Cover practical, commonly-used aspects of \"{topic}\""
        ),
    }

    role = roles.get(card_type, roles["term_def"])
    task = tasks.get(card_type, tasks["term_def"])
    example = examples.get(card_type, examples["term_def"])

    lang_enforce = (
        f"CRITICAL LANGUAGE RULE: You MUST write ALL text in {language} only.\n"
        f"Do NOT use Chinese, Japanese, or any other language.\n"
        f"Even for technical English terms on the front, the back MUST be in {language}.\n"
    )

    return (
        f"{role}\n\n"
        f"TASK:\n{task}\n\n"
        f"{lang_enforce}\n"
        f"OUTPUT FORMAT (follow exactly):\n"
        f"- One flashcard per line\n"
        f"- Format: Front ~ Back\n"
        f"- Output EXACTLY {n} lines\n"
        f"- NO line numbers, NO bullet points, NO markdown\n"
        f"- NO blank lines between cards\n"
        f"- NO intro sentence, NO closing sentence, NO explanation\n"
        f"- Start immediately with the first card\n\n"
        f"EXAMPLE OUTPUT (3 cards, same format you must follow):\n"
        f"{example}\n\n"
        f"Now generate {n} flashcards about \"{topic}\" in {language}:\n"
    )

def _has_cjk(text: str) -> bool:
    """Detect Chinese/Japanese characters that shouldn't appear in Vietnamese output."""
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
            errors.append(line)
            continue
        # Skip cards with Chinese/Japanese characters in the back
        if _has_cjk(back):
            errors.append(f"[CJK detected, skipped] {line}")
            continue
        key = front.lower()
        if key not in seen:
            seen.add(key)
            cards.append(Card(front=front, back=back))
    return cards, errors

def _local_ip() -> str:
    try:
        s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
        s.connect(("8.8.8.8", 80))
        ip = s.getsockname()[0]; s.close()
        return ip
    except Exception:
        return "unknown"

def _record_history(topic, count, language, cards_count, duration_ms, from_cache, client_ip, card_type="term_def"):
    entry = {
        "time": datetime.now().strftime("%Y-%m-%d %H:%M:%S"),
        "topic": topic,
        "count": count,
        "language": language,
        "card_type": card_type,
        "cards_count": cards_count,
        "duration_ms": duration_ms,
        "from_cache": from_cache,
        "client_ip": client_ip,
    }
    _history.insert(0, entry)
    if len(_history) > 200:
        _history.pop()
    _save_json(HISTORY_FILE, _history)

# ─────────────────────────────────────────────
#  Dashboard HTML
# ─────────────────────────────────────────────

def _dashboard_html() -> str:
    running, ready = _ollama_status()
    local_ip = _local_ip()
    uptime = int((datetime.now() - datetime.fromisoformat(_stats["started_at"])).total_seconds())
    uptime_str = f"{uptime//3600}h {(uptime%3600)//60}m {uptime%60}s" if uptime >= 3600 else f"{uptime//60}m {uptime%60}s"

    status_color = "#4ade80" if ready else ("#facc15" if running else "#f87171")
    status_bg    = "#052e16" if ready else ("#422006" if running else "#2d0a0a")
    status_text  = "🟢 Ready" if ready else ("🟡 Model not loaded" if running else "🔴 Ollama offline")

    rows = ""
    for h in _history[:50]:
        cache_badge = '<span class="badge-cache">⚡ cache</span>' if h.get("from_cache") else ""
        dur = h['duration_ms']
        dur_str = "—" if dur == 0 else (f"{dur/1000:.1f}s" if dur >= 1000 else f"{dur}ms")
        dur_color = "#4ade80" if dur < 5000 else ("#facc15" if dur < 15000 else "#f87171")
        ip = h.get('client_ip', '—')
        card_type = h.get('card_type', '—')
        rows += f"""<tr>
          <td class="col-time">{h['time']}</td>
          <td class="col-topic"><span class="topic-text">{h['topic']}</span>{cache_badge}</td>
          <td class="col-type">{card_type}</td>
          <td class="col-lang"><span class="lang-pill">{h['language']}</span></td>
          <td class="col-cards"><span class="cards-num">{h['cards_count']}</span></td>
          <td class="col-dur" style="color:{dur_color}">{dur_str}</td>
          <td class="col-ip">{ip}</td>
        </tr>"""

    if not rows:
        rows = '<tr><td colspan="7" class="empty-row">No requests yet — generate some flashcards!</td></tr>'

    return f"""<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width,initial-scale=1">
  <meta http-equiv="refresh" content="10">
  <title>AI Flashcard Server</title>
  <style>
    *, *::before, *::after {{ box-sizing: border-box; margin: 0; padding: 0; }}
    body {{
      font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;
      background: #0a0f1e; color: #e2e8f0;
      padding: 28px 32px; min-height: 100vh;
    }}
    /* ── Header ── */
    .header {{ margin-bottom: 28px; }}
    .header h1 {{ font-size: 1.4rem; font-weight: 700; letter-spacing: -.3px; }}
    .header .sub {{ color: #475569; font-size: .82rem; margin-top: 4px; }}
    .header .sub a {{ color: #6366f1; text-decoration: none; }}
    .header .sub a:hover {{ text-decoration: underline; }}
    /* ── Status badge ── */
    .status-badge {{
      display: inline-flex; align-items: center; gap: 8px;
      padding: 6px 14px; border-radius: 99px; font-size: .82rem; font-weight: 600;
      border: 1px solid {status_color}40; color: {status_color};
      background: {status_bg}; margin-bottom: 24px;
    }}
    /* ── Stats grid ── */
    .stats {{ display: grid; grid-template-columns: repeat(5, 1fr); gap: 12px; margin-bottom: 20px; }}
    @media(max-width:900px) {{ .stats {{ grid-template-columns: repeat(3,1fr); }} }}
    .stat {{
      background: #111827; border: 1px solid #1f2937;
      border-radius: 12px; padding: 16px 18px;
    }}
    .stat-val {{ font-size: 1.7rem; font-weight: 700; color: #818cf8; line-height: 1; }}
    .stat-lbl {{ font-size: .72rem; color: #4b5563; margin-top: 5px; text-transform: uppercase; letter-spacing: .5px; }}
    /* ── Cards ── */
    .card {{
      background: #111827; border: 1px solid #1f2937;
      border-radius: 14px; overflow: hidden; margin-bottom: 16px;
    }}
    .card-head {{
      padding: 13px 20px; border-bottom: 1px solid #1f2937;
      font-size: .82rem; font-weight: 600; color: #6b7280;
      display: flex; align-items: center; gap: 8px;
    }}
    /* ── Connection info ── */
    .conn-grid {{ display: grid; grid-template-columns: 1fr 1fr; }}
    @media(max-width:700px) {{ .conn-grid {{ grid-template-columns: 1fr; }} }}
    .conn-row {{
      padding: 12px 20px; border-bottom: 1px solid #0d1117; font-size: .84rem;
      display: flex; justify-content: space-between; align-items: center; gap: 12px;
    }}
    .conn-row:last-child {{ border-bottom: none; }}
    .conn-key {{ color: #4b5563; font-size: .78rem; white-space: nowrap; }}
    .conn-val {{ color: #a5b4fc; font-family: 'SF Mono', monospace; font-size: .82rem; }}
    .conn-note {{ padding: 10px 20px; font-size: .76rem; color: #374151; border-top: 1px solid #1f2937; }}
    /* ── Table ── */
    .table-wrap {{ overflow-x: auto; }}
    table {{ width: 100%; border-collapse: collapse; font-size: .83rem; }}
    thead tr {{ background: #0d1117; }}
    th {{
      padding: 11px 16px; text-align: left;
      font-size: .72rem; font-weight: 600; color: #4b5563;
      text-transform: uppercase; letter-spacing: .6px;
      white-space: nowrap; border-bottom: 1px solid #1f2937;
    }}
    td {{ padding: 11px 16px; border-bottom: 1px solid #0d1117; vertical-align: middle; }}
    tbody tr:last-child td {{ border-bottom: none; }}
    tbody tr:hover td {{ background: #0d1117; }}
    /* Column widths */
    .col-time  {{ color: #4b5563; font-size: .78rem; white-space: nowrap; width: 140px; }}
    .col-topic {{ max-width: 260px; }}
    .col-type  {{ color: #6b7280; font-size: .78rem; white-space: nowrap; width: 110px; }}
    .col-lang  {{ width: 90px; }}
    .col-cards {{ width: 60px; text-align: center; }}
    .col-dur   {{ width: 80px; text-align: right; font-family: monospace; font-size: .8rem; }}
    .col-ip    {{ color: #374151; font-size: .76rem; font-family: monospace; width: 120px; }}
    /* Inline elements */
    .topic-text {{ color: #e2e8f0; font-weight: 500; display: block; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; max-width: 240px; }}
    .badge-cache {{ display: inline-block; padding: 1px 7px; border-radius: 99px; font-size: .68rem; font-weight: 600; background: #312e81; color: #a5b4fc; margin-left: 6px; vertical-align: middle; }}
    .lang-pill {{ display: inline-block; padding: 2px 9px; border-radius: 99px; font-size: .72rem; background: #1e293b; color: #94a3b8; border: 1px solid #334155; }}
    .cards-num {{ display: inline-block; min-width: 28px; text-align: center; padding: 2px 8px; border-radius: 6px; font-weight: 700; font-size: .85rem; background: #1e3a5f; color: #60a5fa; }}
    .empty-row {{ text-align: center; color: #374151; padding: 40px 16px !important; font-size: .85rem; }}
  </style>
</head>
<body>
  <div class="header">
    <h1>✦ AI Flashcard Server</h1>
    <p class="sub">
      Auto-refreshes every 10s &nbsp;·&nbsp;
      <a href="/docs">API Docs</a> &nbsp;·&nbsp;
      <a href="/history">History JSON</a> &nbsp;·&nbsp;
      Model: <strong style="color:#a5b4fc">{MODEL}</strong>
    </p>
  </div>

  <div class="status-badge">{status_text}</div>

  <div class="stats">
    <div class="stat"><div class="stat-val">{_stats['requests']}</div><div class="stat-lbl">Requests</div></div>
    <div class="stat"><div class="stat-val">{_stats['cards_generated']}</div><div class="stat-lbl">Cards made</div></div>
    <div class="stat"><div class="stat-val">{_stats['cache_hits']}</div><div class="stat-lbl">Cache hits</div></div>
    <div class="stat"><div class="stat-val">{len(_cache)}</div><div class="stat-lbl">Cached topics</div></div>
    <div class="stat"><div class="stat-val">{uptime_str}</div><div class="stat-lbl">Uptime</div></div>
  </div>

  <div class="card">
    <div class="card-head">🌐 Connection</div>
    <div class="conn-grid">
      <div class="conn-row"><span class="conn-key">Localhost</span><span class="conn-val">http://localhost:{PORT}</span></div>
      <div class="conn-row"><span class="conn-key">LAN · Real device</span><span class="conn-val">http://{local_ip}:{PORT}</span></div>
      <div class="conn-row"><span class="conn-key">Android Emulator</span><span class="conn-val">http://10.0.2.2:{PORT}</span></div>
      <div class="conn-row"><span class="conn-key">History file</span><span class="conn-val">ai_server/history.json</span></div>
    </div>
    <div class="conn-note">💡 Điện thoại thật dùng địa chỉ LAN. Nếu không kết nối được → Windows Firewall → Allow port {PORT}.</div>
  </div>

  <div class="card">
    <div class="card-head">📋 Request History <span style="color:#374151;font-weight:400">(last 50)</span></div>
    <div class="table-wrap">
      <table>
        <thead>
          <tr>
            <th>Time</th>
            <th>Topic</th>
            <th>Type</th>
            <th>Language</th>
            <th style="text-align:center">Cards</th>
            <th style="text-align:right">Duration</th>
            <th>Client IP</th>
          </tr>
        </thead>
        <tbody>{rows}</tbody>
      </table>
    </div>
  </div>
</body>
</html>"""

# ─────────────────────────────────────────────
#  Routes
# ─────────────────────────────────────────────

@app.get("/", response_class=HTMLResponse, include_in_schema=False)
def dashboard():
    return _dashboard_html()

@app.get("/history", include_in_schema=False)
def get_history():
    return _history

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
        message="Sẵn sàng generate flashcard!", stats=_stats, lan_ip=_local_ip())

@app.post("/generate", response_model=GenerateResponse)
def generate(req: GenerateRequest, request: "Request" = None):
    from fastapi import Request as Req
    client_ip = request.client.host if request else "unknown"

    running, ready = _ollama_status()
    if not running:
        raise HTTPException(503, detail="Ollama offline")
    if not ready:
        raise HTTPException(503, detail=f"Model chưa tải. Chạy: ollama pull {MODEL}")

    _stats["requests"] += 1

    # ── Cache check ──────────────────────────────────────────
    key = _cache_key(req.topic, req.count, req.language, req.card_type)
    if key in _cache:
        _stats["cache_hits"] += 1
        cached = _cache[key]
        _record_history(req.topic, req.count, req.language,
                        len(cached["cards"]), 0, True, client_ip, req.card_type)
        return GenerateResponse(
            cards=[Card(**c) for c in cached["cards"]],
            error_lines=cached["error_lines"],
            raw_output=cached["raw_output"],
            duration_ms=0,
            model=MODEL,
            from_cache=True,
        )

    # ── Generate ─────────────────────────────────────────────
    prompt = _build_prompt(req.topic, req.count, req.language, req.card_type)
    t0 = time.time()
    try:
        resp = requests.post(
            f"{OLLAMA_BASE}/api/generate",
            json={
                "model": MODEL,
                "prompt": prompt,
                "stream": False,
                "options": {
                    "temperature": 0.4,
                    "top_k": 40,
                    "top_p": 0.9,
                    "num_predict": MAX_TOKENS,
                    "stop": ["---", "\n\n\n"],
                },
            },
            timeout=300,  # 5 min — enough for 50 long cards
        )
        resp.raise_for_status()
        raw = resp.json().get("response", "").strip()
    except requests.Timeout:
        raise HTTPException(504, detail="Model timeout — giảm số lượng thẻ và thử lại")
    except Exception as e:
        raise HTTPException(500, detail=f"Generation failed: {e}")

    duration = int((time.time() - t0) * 1000)
    cards, errors = _parse(raw)
    _stats["cards_generated"] += len(cards)

    # ── Save to cache ─────────────────────────────────────────
    _cache[key] = {"cards": [c.dict() for c in cards],
                   "error_lines": errors, "raw_output": raw}
    _save_json(CACHE_FILE, _cache)

    _record_history(req.topic, req.count, req.language, len(cards), duration, False, client_ip, req.card_type)

    return GenerateResponse(cards=cards, error_lines=errors, raw_output=raw,
                            duration_ms=duration, model=MODEL, from_cache=False)

# ─────────────────────────────────────────────
#  Startup banner
# ─────────────────────────────────────────────

def _print_banner():
    running, ready = _ollama_status()
    local_ip = _local_ip()
    lines = [
        "",
        "  ╔══════════════════════════════════════════╗",
        "  ║      AI Flashcard Server  v3.0.0         ║",
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
        f"  📦  Cache: {len(_cache)} topics  |  History: {len(_history)} requests",
        "",
        "  💡  Điện thoại thật: vào app → Settings → đổi server URL",
        f"      thành  http://{local_ip}:{PORT}",
        "",
        "  Nhấn Ctrl+C để dừng",
        "",
    ]
    print("\n".join(lines))

if __name__ == "__main__":
    _print_banner()
    uvicorn.run(app, host=HOST, port=PORT, log_level="warning")
