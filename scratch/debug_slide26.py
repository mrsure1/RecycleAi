#!/usr/bin/env python3
import http.server
import threading
from pathlib import Path

from playwright.sync_api import sync_playwright

ROOT = Path(__file__).resolve().parents[1] / "docs" / "presentation"
PORT = 9876


class Handler(http.server.SimpleHTTPRequestHandler):
    def __init__(self, *args, **kwargs):
        super().__init__(*args, directory=str(ROOT), **kwargs)


srv = http.server.ThreadingHTTPServer(("127.0.0.1", PORT), Handler)
threading.Thread(target=srv.serve_forever, daemon=True).start()

with sync_playwright() as p:
    page = p.chromium.launch(headless=True).new_page(viewport={"width": 1280, "height": 720})
    page.goto(f"http://127.0.0.1:{PORT}/web/index.html", wait_until="domcontentloaded")
    page.wait_for_timeout(2000)
    page.wait_for_function("() => typeof window.goToSlide === 'function'")
    for _ in range(30):
        page.keyboard.press("End")
        page.wait_for_timeout(250)
        if page.evaluate("() => document.querySelector('#presentation > .slide.active')?.id") == "slide-closing":
            break
    page.wait_for_timeout(600)
    active = page.evaluate("() => document.querySelector('#presentation > .slide.active')?.id")
    overlay = page.evaluate(
        "() => ({ visible: document.getElementById('closing-overlay')?.classList.contains('is-visible'), text: document.querySelector('#closing-overlay .closing-title')?.innerText })"
    )
    print("active:", active, "overlay:", overlay)
    for sel in [".closing-title", ".closing-sub", ".closing-card", ".closing-footer", ".closing-logo"]:
        el = page.locator(f"#closing-overlay {sel}")
        if not el.count():
            print(sel, "MISSING")
            continue
        info = el.evaluate(
            """e => ({
                opacity: getComputedStyle(e).opacity,
                color: getComputedStyle(e).color,
                fill: getComputedStyle(e).webkitTextFillColor,
                display: getComputedStyle(e).display,
                visibility: getComputedStyle(e).visibility,
                text: e.innerText.slice(0, 60)
            })"""
        )
        print(sel, info)
    out = ROOT / "captures" / "debug_slide26.png"
    page.locator("#presentation").screenshot(path=str(out))
    print("screenshot:", out)

srv.shutdown()
