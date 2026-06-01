# CLAUDE.md

Guidance for working in this repository.

## Project Overview

**RecycleAI (TrashAI)** — Android Kotlin/Compose recycling guide app.

| Area | Location |
|------|----------|
| Product docs | `PRD.md`, `trd.md`, `problem.md`, `docs/` |
| Android app | `app/src/main/java/app/trashai/` |
| Data pipeline | `scripts/`, `data/` → `app/src/main/assets/wasteguide.sqlite3` |
| UI prototype (static) | `ui/` — reference only, not production |

## Runtime architecture (app)

- **Card copy SSOT**: SQLite `app_item_rule`, `app_region_ordinance`, `app_common_guide` only. Never render raw LLM text as authoritative copy.
- **Vision**: ML Kit → local DB / keyword search; fallback **Gemini** via `GeminiClient` (`SupabaseVectorClient` is legacy naming only — no cloud DB).
- **Region schedules**: `app_mois_disposal` keyed by wasteguide **`region_code` (5-digit)**. `RegionExtrasLoader` reads bundled SQLite only.
- **Region contacts**: `app_region_contact` from `data/region_contacts.json` + import.
- **Location**: Geocoder → `ordinanceByRegion(sido, sigungu)`.
- **No backend**: No Supabase, PostgREST, or custom API at runtime.

## Data pipeline

```bash
pip install -r requirements.txt
python scripts/complete_wasteguide_db.py [--skip-crawl]
python scripts/build_region_mois_map.py
python scripts/import_region_extras.py   # WASTE_OPEN_API_KEY in local.properties
```

### SQLite tiers

1. **Raw crawled** (`dictionary_item`, `region_ordinance`, …) — `wasteguide_*_crawler.py`
2. **App-ready** (`app_item_rule`, `app_region_ordinance`, `app_search_keyword`, …) — `finalize_app_db.py`
3. **Region extras** — `import_region_extras.py`:
   - `app_region_mois_map` — 5↔7 code map snapshot
   - `app_mois_disposal` — MOIS disposal_time/method (5-digit `sigungu_code` = `region_code`)
   - `app_region_contact` — from `data/region_contacts.json`

Mapping: `data/region_mois_code_map.json` via `build_region_mois_map.py` (excel + heuristic `region_code + "00"` + `data/region_mois_code_overrides.json`).

## Android keys (`local.properties`)

- **Required for scan fallback**: `GEMINI_API_KEY`
- **Pipeline only**: `WASTE_OPEN_API_KEY`

## Windows terminal (UTF-8)

Integrated terminal in this repo uses `.vscode/settings.json` (profile **RecycleAI UTF-8 PowerShell**).  
For one-off shells: `. scripts/utf8_env.ps1` before `python` commands. See `docs/terminal_utf8.md`.

## Crawler etiquette

- Default delay 4–8s for wasteguide.or.kr crawlers; keep identifiable User-Agent.
- Do not crawl `/front/search`, `/front/support`.

## Language

Preserve Korean user-facing strings verbatim when editing copy sourced from official text.
