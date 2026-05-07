def _dashboard_html() -> str:
    import html as _html

    running, ready = _ollama_status()
    local_ip  = _local_ip()
    uptime    = int((datetime.now() - datetime.fromisoformat(_stats["started_at"])).total_seconds())
    uptime_str = f"{uptime//3600}h {(uptime%3600)//60}m {uptime%60}s" if uptime >= 3600 else f"{uptime//60}m {uptime%60}s"

    status_color  = "#16a34a" if ready else ("#d97706" if running else "#dc2626")
    status_bg     = "#f0fdf4" if ready else ("#fffbeb" if running else "#fef2f2")
    status_border = "#bbf7d0" if ready else ("#fde68a" if running else "#fecaca")
    status_text   = "🟢 Model sẵn sàng" if ready else ("🟡 Model chưa load" if running else "🔴 Ollama offline")

    CARD_TYPE_VI = {
        "term_def": ("Thuật ngữ",       "#dbeafe", "#1d4ed8"),
        "en_vi":    ("Anh → Việt",      "#dcfce7", "#15803d"),
        "vi_en":    ("Việt → Anh",      "#dcfce7", "#15803d"),
        "qa":       ("Hỏi & Đáp",       "#fef9c3", "#a16207"),
        "antonym":  ("Trái/Đồng nghĩa", "#fce7f3", "#be185d"),
        "code":     ("Lập trình",       "#ede9fe", "#7c3aed"),
    }

    # ── Flashcard history rows ────────────────────────────────────────────────
    fc_rows = ""
    fc_modals = ""
    for idx, h in enumerate(_history[:50]):
        cache_badge = '<span class="badge-cache">⚡ cache</span>' if h.get("from_cache") else ""
        dur   = h["duration_ms"]
        dur_str   = "—" if dur == 0 else (f"{dur/1000:.1f}s" if dur >= 1000 else f"{dur}ms")
        dur_color = "#16a34a" if dur < 5000 else ("#d97706" if dur < 15000 else "#dc2626")
        ct    = h.get("card_type", "term_def")
        ct_vi, ct_bg, ct_fg = CARD_TYPE_VI.get(ct, (ct, "#f1f5f9", "#475569"))
        topic_safe = _html.escape(h["topic"])
        mid   = f"fc{idx}"

        fc_rows += f"""<tr class="clickable-row" onclick="openFcModal('{mid}')">
          <td class="col-time">{h['time']}</td>
          <td class="col-topic">{topic_safe}{cache_badge}</td>
          