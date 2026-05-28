#!/usr/bin/env python3
"""RecycleAI 투자·발표용 PPTX 생성. 실행: python docs/presentation/build_presentation.py"""

from __future__ import annotations

from pathlib import Path

from pptx import Presentation
from pptx.dml.color import RGBColor
from pptx.enum.text import MSO_ANCHOR, PP_ALIGN
from pptx.util import Inches, Pt

ROOT = Path(__file__).resolve().parent
ASSETS = ROOT / "assets"
OUT = ROOT / "RecycleAI_Investor_Pitch.pptx"

# 브랜드
GREEN = RGBColor(0x2D, 0x5A, 0x27)
GREEN_LIGHT = RGBColor(0x10, 0xB9, 0x81)
DARK = RGBColor(0x1E, 0x29, 0x3B)
GRAY = RGBColor(0x64, 0x74, 0x8B)
WHITE = RGBColor(0xFF, 0xFF, 0xFF)
BG = RGBColor(0xF8, 0xFA, 0xFC)


def _blank(prs: Presentation):
    layout = prs.slide_layouts[6]
    return prs.slides.add_slide(layout)


def _bar(slide, title: str, subtitle: str | None = None):
    box = slide.shapes.add_shape(1, Inches(0), Inches(0), Inches(13.333), Inches(1.05))
    box.fill.solid()
    box.fill.fore_color.rgb = GREEN
    box.line.fill.background()
    tf = box.text_frame
    tf.text = title
    p = tf.paragraphs[0]
    p.font.size = Pt(28)
    p.font.bold = True
    p.font.color.rgb = WHITE
    if subtitle:
        p2 = tf.add_paragraph()
        p2.text = subtitle
        p2.font.size = Pt(14)
        p2.font.color.rgb = RGBColor(0xE2, 0xE8, 0xF0)


def _bullets(slide, items: list[str], top=1.35, left=0.7, width=12.0, size=20):
    box = slide.shapes.add_textbox(Inches(left), Inches(top), Inches(width), Inches(5.5))
    tf = box.text_frame
    tf.word_wrap = True
    for i, line in enumerate(items):
        p = tf.paragraphs[0] if i == 0 else tf.add_paragraph()
        p.text = line
        p.level = 0
        p.font.size = Pt(size)
        p.font.color.rgb = DARK
        p.space_after = Pt(10)


def _center_title(slide, title: str, sub: str | None = None):
    slide.background.fill.solid()
    slide.background.fill.fore_color.rgb = BG
    t = slide.shapes.add_textbox(Inches(0.8), Inches(2.4), Inches(11.7), Inches(1.2))
    tf = t.text_frame
    tf.text = title
    p = tf.paragraphs[0]
    p.alignment = PP_ALIGN.CENTER
    p.font.size = Pt(40)
    p.font.bold = True
    p.font.color.rgb = GREEN
    if sub:
        s = slide.shapes.add_textbox(Inches(1.2), Inches(3.6), Inches(10.9), Inches(1.5))
        sf = s.text_frame
        sf.text = sub
        sp = sf.paragraphs[0]
        sp.alignment = PP_ALIGN.CENTER
        sp.font.size = Pt(18)
        sp.font.color.rgb = GRAY


def _table(slide, headers: list[str], rows: list[list[str]], top=1.5):
    n_rows, n_cols = len(rows) + 1, len(headers)
    shape = slide.shapes.add_table(n_rows, n_cols, Inches(0.5), Inches(top), Inches(12.3), Inches(0.55 * n_rows))
    tbl = shape.table
    for c, h in enumerate(headers):
        cell = tbl.cell(0, c)
        cell.text = h
        for p in cell.text_frame.paragraphs:
            p.font.bold = True
            p.font.size = Pt(14)
            p.font.color.rgb = WHITE
        cell.fill.solid()
        cell.fill.fore_color.rgb = GREEN
    for r, row in enumerate(rows, 1):
        for c, val in enumerate(row):
            cell = tbl.cell(r, c)
            cell.text = val
            for p in cell.text_frame.paragraphs:
                p.font.size = Pt(12)
                p.font.color.rgb = DARK
            if r % 2 == 0:
                cell.fill.solid()
                cell.fill.fore_color.rgb = BG


def _image_or_placeholder(slide, path: Path | None, left, top, w, h, caption: str, placeholder: str):
    if path and path.exists():
        slide.shapes.add_picture(str(path), Inches(left), Inches(top), width=Inches(w))
    else:
        ph = slide.shapes.add_shape(1, Inches(left), Inches(top), Inches(w), Inches(h))
        ph.fill.solid()
        ph.fill.fore_color.rgb = RGBColor(0xE2, 0xE8, 0xF0)
        ph.line.color.rgb = GRAY
        tf = ph.text_frame
        tf.text = placeholder
        tf.paragraphs[0].alignment = PP_ALIGN.CENTER
        tf.paragraphs[0].font.size = Pt(14)
        tf.paragraphs[0].font.color.rgb = GRAY
        tf.vertical_anchor = MSO_ANCHOR.MIDDLE
    cap = slide.shapes.add_textbox(Inches(left), Inches(top + h + 0.05), Inches(w), Inches(0.45))
    cap.text_frame.text = caption
    cap.text_frame.paragraphs[0].font.size = Pt(11)
    cap.text_frame.paragraphs[0].font.color.rgb = GRAY
    cap.text_frame.paragraphs[0].alignment = PP_ALIGN.CENTER


def build() -> Path:
    prs = Presentation()
    prs.slide_width = Inches(13.333)
    prs.slide_height = Inches(7.5)

    # 1 타이틀
    s = _blank(prs)
    _center_title(
        s,
        "RecycleAI (리사이클AI)",
        "카메라만 비추면, 우리 동네 기준 분리배출을 안내합니다\n투자·파트너 소개 | 2026",
    )
    foot = s.shapes.add_textbox(Inches(0.8), Inches(6.2), Inches(11.7), Inches(0.5))
    foot.text_frame.text = "발표자: _________  |  연락: _________"
    foot.text_frame.paragraphs[0].alignment = PP_ALIGN.CENTER
    foot.text_frame.paragraphs[0].font.size = Pt(12)
    foot.text_frame.paragraphs[0].font.color.rgb = GRAY

    # 2 Problem
    s = _blank(prs)
    _bar(s, "왜 만들었나 — Problem", "이사·출장·여행으로 ‘동네 규칙’이 바뀔 때")
    _bullets(
        s,
        [
            "페르소나: 35년 강남 생활 후 일산 이사한 60대 — 분리수거 경고 스티커를 두 번 받음",
            "고무장갑·즉석밥 용기 등 ‘예전 동네 습관’이 오히려 실수를 만듦",
            "지자체마다 분류가 반대인 경우도 있음 → 과태료·수거 거부·수치심",
            "하루 6~12회, 분리수거일엔 15~25개 — 의심 케이스만 1~3분 × 주당 10~15회",
            "기존 대안: PDF(글자 작음), 이웃·경비실 문의(부담), 검색 앱(타 지역 기준)",
        ],
        size=19,
    )

    # 3 Value
    s = _blank(prs)
    _bar(s, "사용자가 되찾는 가치")
    _bullets(
        s,
        [
            "자존심 회복 — “모르는 게 아니라 동네가 다르다”는 안심",
            "과태료·경고 리스크 감소 (건당 수만 원대 가능)",
            "분리수거 시간 30분 → 10분 수준 단축 (가정)",
            "이웃·경비실 반복 질문 없이 스스로 판단",
            "이사·출장 시에도 GPS 기준으로 즉시 현지 규칙 적용",
        ],
    )

    # 4 Solution
    s = _blank(prs)
    _bar(s, "Solution — RecycleAI 한 줄")
    _center_title(s, "버리려는 물건을 카메라로 비추면", "AI 인식 + 우리 동네 SQLite 규칙 → 한 화면 카드에 배출법·요일·문의처")
    steps = s.shapes.add_textbox(Inches(1.0), Inches(4.5), Inches(11.3), Inches(2.2))
    tf = steps.text_frame
    for i, t in enumerate(
        [
            "① GPS로 지자체 확인",
            "② ML Kit 초록 박스 / 손가락 주황 박스로 물건 지정",
            "③ 로컬 DB + (필요 시) Gemini → 픽토그램·배출 요일·E-순환 안내",
        ]
    ):
        p = tf.paragraphs[0] if i == 0 else tf.add_paragraph()
        p.text = t
        p.font.size = Pt(20)
        p.font.color.rgb = DARK
        p.space_after = Pt(8)

    # 5 개발 프로세스
    s = _blank(prs)
    _bar(s, "일반적인 앱 개발 프로세스 (본 프로젝트 적용)")
    _table(
        s,
        ["단계", "우리가 한 일", "산출물"],
        [
            ["1. 문제·페르소나", "박정호 케이스 구체화", "problem.md"],
            ["2. 기획·스펙", "화면별 P0 기능 정의", "PRD.md, vision.md"],
            ["3. UX·디자인", "글래스모피즘·네온 박스·핀치 줌", "ui/ 프로토타입, Tokens.kt"],
            ["4. 데이터", "공공 품목·조례·MOIS 크롤", "wasteguide.sqlite3"],
            ["5. 구현", "Compose + ML Kit + Gemini", "Android APK"],
            ["6. 검증·출시", "실기기 테스트 → 스토어", "Play Console, 홍보"],
        ],
        top=1.25,
    )

    # 6 AI 개발 방식
    s = _blank(prs)
    _bar(s, "개발 방식 — 문서 먼저, AI와 함께 구현")
    _bullets(
        s,
        [
            "problem / PRD / TRD를 먼저 써서 AI·개발자가 같은 ‘제품 언어’를 쓰게 함",
            "카드 문구는 공공 DB(SQLite)만 사용 — AI 답변을 그대로 노출하지 않음 (신뢰)",
            "로컬 730품목 1차 매칭 → 애매할 때만 Gemini (비용·발열 절감)",
            "오프라인 우선: 지하철·와이파이 없어도 조례·배출 요일 조회 가능",
        ],
    )

    # 7 아키텍처
    s = _blank(prs)
    _bar(s, "기술 구조 (쉬운 말로)")
    _bullets(
        s,
        [
            "카메라: Android CameraX + Google ML Kit (가벼운 객체 감지)",
            "데이터: 앱 안 SQLite — 품목 730+, 지자체 조례, MOIS 배출 일정",
            "AI: Gemini는 ‘사진→한글 키워드’만, 안내 문장은 DB에서 조합",
            "위치: GPS + Geocoder → 해당 구·시 규칙 카드",
            "폐가전: E-순환 무상 수거 안내·전화 연결",
        ],
    )

    # 8 경쟁 앱 소개
    s = _blank(prs)
    _bar(s, "비교 대상 — 「내 손안의 분리배출」", "환경부·한국환경공단 등 공식 앱 | Play 10만+ 다운로드")
    _bullets(
        s,
        [
            "강점: 공신력, 재질 8분류, 품목 검색, 전국 기본 가이드",
            "한계(우리 관찰): 텍스트·카테고리 중심, 실시간 카메라 인식 없음",
            "지역: 전국 ‘표준’ 안내 — 거주지 GPS·조례·배출 요일 일체형은 아님",
            "네트워크: 온라인 환경 권장(보도·리뷰 기준)",
        ],
        top=1.2,
        size=18,
    )
    _image_or_placeholder(
        s,
        ASSETS / "competitor_home.png",
        8.2,
        1.35,
        4.6,
        4.8,
        "공식 앱 첫 화면 (대한민국 정책브리핑 캡처)",
        "",
    )

    # 9 비교 표
    s = _blank(prs)
    _bar(s, "기능 비교 한눈에")
    _table(
        s,
        ["항목", "내 손안의 분리배출", "RecycleAI"],
        [
            ["입력 방식", "검색·카테고리 선택", "카메라 실시간 + 드래그 영역"],
            ["지역 맞춤", "전국 공통 기준", "GPS → 해당 지자체 조례·배출일"],
            ["오프라인", "제한적(온라인 권장)", "품목·조례·일정 로컬 DB"],
            ["시각 안내", "정적 사진·텍스트", "네온 박스 + 픽토그램 카드"],
            ["접근성", "일반 UI", "핀치 줌·큰 카드·전화 바로걸기"],
            ["폐가전", "별도 안내", "스캔 시 E-순환·문의처 연동"],
        ],
        top=1.2,
    )

    # 10 경쟁 스크린샷 1
    s = _blank(prs)
    _bar(s, "경쟁 앱 화면 — 카테고리·품목 안내")
    _image_or_placeholder(s, ASSETS / "competitor_categories.png", 0.6, 1.3, 5.8, 5.2, "재질별 분류 (정책브리핑)", "")
    _image_or_placeholder(
        s,
        ASSETS / "recycleai_scan.png",
        6.9,
        1.3,
        5.8,
        5.2,
        "RecycleAI — 카메라 스캔·초록 박스",
        "[캡처 삽입]\n실기기 스크린샷",
    )

    # 11 경쟁 vs 우리 상세
    s = _blank(prs)
    _bar(s, "경쟁 앱 vs RecycleAI — 상세 안내 화면")
    _image_or_placeholder(s, ASSETS / "competitor_detail.png", 0.6, 1.3, 5.8, 5.2, "품목별 텍스트 설명", "")
    _image_or_placeholder(
        s,
        ASSETS / "recycleai_card.png",
        6.9,
        1.3,
        5.8,
        5.2,
        "RecycleAI — 하단 결과 카드·픽토그램",
        "[캡처 삽입]\n배출 요일·핀치 줌",
    )

    # 12 우리만의 장점
    s = _blank(prs)
    _bar(s, "RecycleAI가 어필할 5가지")
    _bullets(
        s,
        [
            "「지금 이 동네」 — 이사·출장 사용자에게 가장 큰 차별점",
            "손에 든 물건 그대로 — 검색어를 고민할 필요 없음",
            "한 화면 원스톱 — 분류·세척·라벨·배출 요일·지자체 전화",
            "신뢰 설계 — 공공 DB 문구만 표시, AI는 보조 매칭만",
            "시니어 UX — 핀치 줌, 네온 피드백, 다시 스캔",
        ],
        size=22,
    )

    # 13 시나리오
    s = _blank(prs)
    _bar(s, "데모 시나리오 (발표 시 연출)")
    _bullets(
        s,
        [
            "앱 실행 → 상단 「고양시 일산서구」 확인",
            "마우스 터치 → 초록 박스 → 카드에 배출법·요일",
            "플라스틱 컵 스캔 → 비우기·헹구기 픽토그램",
            "여러 물건 겹침 → 주황 드래그 박스로 한 개만 분석",
            "폐가전 스캔 → E-순환 무상 수거·1599-0903",
        ],
    )

    # 14 현황
    s = _blank(prs)
    _bar(s, "현재 상태 (2026)")
    _bullets(
        s,
        [
            "Android Compose 앱 — Production Ready 수준 문서·DB 파이프라인",
            "품목·조례: wasteguide 기반 SQLite 번들",
            "배출 일정: MOIS 데이터 일부 지자체 적재(확장 중)",
            "다음: Play 스토어 등록, 실사용 피드백, iOS·전국 커버리지",
        ],
    )

    # 15 Play Store
    s = _blank(prs)
    _bar(s, "Google Play 배포 로드맵")
    _bullets(
        s,
        [
            "개발자 계정 등록(일회성 등록비) · 앱 서명 키 안전 보관",
            "스토어 등록정보: 앱명 RecycleAI, 짧은 설명·스크린샷 6~8장·정책 URL",
            "개인정보 처리방침·카메라·위치 권한 고지 (Play 정책 필수)",
            "내부 테스트 → 클로즈드 테스트(지인 20명) → 프로덕션 단계 출시",
            "ASO 키워드: 분리수거, 분리배출, 재활용, 이사, 지역, 카메라",
        ],
        size=18,
    )

    # 16 SNS 홍보
    s = _blank(prs)
    _bar(s, "SNS·콘텐츠 홍보")
    _bullets(
        s,
        [
            "숏폼(릴스·틱톡·쇼츠): 「고무장갑 강남 vs 일산」 15초 비교",
            "카카오 오픈채팅·아파트 커뮤니티: 이사 시즌(2~3월, 9월) 집중",
            "블로그·네이버: 「분리수거 경고 스티커」 검색어 SEO",
            "지자체·환경 박람회·대학 EC 동아리 시연 부스",
            "인플루언서: 육아·생활·시니어 IT 유튜버 체험 영상",
        ],
        size=18,
    )

    # 17 수익화
    s = _blank(prs)
    _bar(s, "수익화 방향 (단계별)")
    _table(
        s,
        ["단계", "모델", "설명"],
        [
            ["1단계", "무료 + 브랜드", "사용자·리뷰 확보, 공공 미션 이미지"],
            ["2단계", "프리미엄", "광고 제거, 이사 모드(출신지↔현재지 비교), 가족 공유"],
            ["3단계", "B2B·B2G", "아파트·지자체 화이트라벨, 데이터 리포트(익명 통계)"],
            ["4단계", "제휴", "대형폐기물·리사이클 업체 연계(추천 수수료, 신중히)"],
        ],
        top=1.25,
    )
    note = s.shapes.add_textbox(Inches(0.7), Inches(5.8), Inches(12), Inches(0.8))
    note.text_frame.text = "※ 공공 데이터·환경 교육 앱 특성상, 과도한 광고보다 신뢰·파트너십 우선 권장"
    note.text_frame.paragraphs[0].font.size = Pt(13)
    note.text_frame.paragraphs[0].font.color.rgb = GRAY

    # 18 로드맵
    s = _blank(prs)
    _bar(s, "12개월 로드맵")
    _bullets(
        s,
        [
            "Q1: Play 출시, MOIS·조례 커버리지 확대, 스토어 리뷰 대응",
            "Q2: 이사 모드·품목 Clarification 고도화, iOS 검토",
            "Q3: 지자체·아파트 파일럿, B2G 제안서",
            "Q4: 전국 커버리지 목표, 프리미엄·제휴 파일럿",
        ],
        size=20,
    )

    # 19 투자 포인트
    s = _blank(prs)
    _bar(s, "투자자에게 드리는 메시지")
    _bullets(
        s,
        [
            "문제는 ‘분류 지식’이 아니라 ‘지역 이동’ — 인구 이동·고령화와 맞물림",
            "공공 데이터 + 모바일 AI의 조합 — 이미 구현된 파이프라인",
            "공식 앱과 공존·차별 — 검색 앱을 대체가 아닌 ‘현장 카메라 레이어’",
            "확장: 지자체 계약, 교육·ESG, 해외 교포·유학생 시장 검토",
        ],
        size=20,
    )

    # 20 Q&A
    s = _blank(prs)
    _center_title(s, "감사합니다", "질문을 환영합니다")
    qa = s.shapes.add_textbox(Inches(1.5), Inches(4.2), Inches(10.3), Inches(2.5))
    tf = qa.text_frame
    for i, t in enumerate(
        [
            "데모 APK / 스토어 링크: _________",
            "연락처: _________",
            "※ 슬라이드 10~11: RecycleAI 실기기 캡처를 assets/ 에 넣고 스크립트 재실행",
        ]
    ):
        p = tf.paragraphs[0] if i == 0 else tf.add_paragraph()
        p.text = t
        p.alignment = PP_ALIGN.CENTER
        p.font.size = Pt(16)
        p.font.color.rgb = GRAY

    prs.save(OUT)
    return OUT


if __name__ == "__main__":
    path = build()
    print(f"생성 완료: {path}")
