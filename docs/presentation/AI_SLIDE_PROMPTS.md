# RecycleAI 발표 PPT — 다른 AI 도구용 프롬프트

아래 블록을 **Gamma**, **Canva Magic Design**, **Beautiful.ai**, **Copilot in PowerPoint**, **Gemini** 등에 그대로 붙여 넣으면 됩니다.  
스크린샷은 `docs/presentation/assets/` 폴더 이미지를 업로드하거나, `[캡처 삽입]` 자리에 실기기 사진을 넣으세요.

---

## 마스터 프롬프트 (한 번에 전체 덱)

```
RecycleAI(리사이클AI) 투자자·파트너 발표용 PowerPoint 25장, 16:9, 한국어.

톤: 전문적이지만 어려운 IT 용어는 피하고, 투자자가 10분 안에 이해할 수 있게.
색상: 숲녹색 #2D5A27 포인트, 배경 밝은 회색, 제목 굵게.

슬라이드 구성:
1. 타이틀 — RecycleAI, 카메라만 비추면 우리 동네 분리배출 안내
2. Problem — 60대 이사자(강남→일산), 경고 스티커, 지역마다 다른 규칙, 검색·PDF의 한계
3. 사용자 가치 — 자존심·시간·과태료 리스크·이웃 부담 감소
4. Solution — GPS + 카메라 ML + 로컬 DB + (필요시) Gemini, 한 화면 카드
5. 앱 개발 프로세스 표 — 문제정의→PRD→UX→데이터크롤→Android구현→스토어출시
6. 개발 방식 — 문서 먼저, AI 협업, 카드문구는 공공DB만, 오프라인 우선
7. 기술 구조 — CameraX, ML Kit, SQLite 730품목, MOIS 배출일정, Gemini는 키워드만
8. 경쟁앱 소개 — 「내 손안의 분리배출」 환경부 공식앱 10만+다운로드
9. 비교표 — 입력(검색 vs 카메라), 지역(GPS맞춤), 오프라인, 시각(픽토그램), 접근성(핀치줌)
10. 좌: 경쟁앱 카테고리 스크린샷 / 우: RecycleAI 카메라 스캔 스크린샷
11. 좌: 경쟁앱 품목설명 / 우: RecycleAI 결과카드·배출요일
12. RecycleAI 5대 장점 — 지금 이 동네, 손에 든 물건, 원스톱, 신뢰(DB), 시니어 UX
13. 데모 시나리오 5단계
14. 현재상태 Production Ready, MOIS 확장중
15. Play Store 배포 단계 — 개발자계정, 권한고지, 내부테스트→출시, ASO키워드
16. SNS 홍보 — 숏폼(강남vs일산), 이사시즌, 아파트커뮤니티, 박람회
17. 수익화 표 — 무료→프리미엄→B2G화이트라벨→제휴(신중)
18. 12개월 로드맵 Q1~Q4
19. 투자 포인트 — 지역이동×고령화, 공공데이터, 공식앱과 차별 레이어
20. 중간발표 정정 — Supabase는 앱 런타임 실시간 검색 DB가 아님. 현재 앱은 Supabase·PostgREST·백엔드 없이 로컬 SQLite를 조회하며, Gemini는 애매한 사진을 한글 키워드로 바꾸는 보조 역할만 함. 행안부 DB가 730여 개에서 1500개로 확대될 때 Supabase는 앱 자체 SQLite 업데이트 작업에만 임시 활용 가능하다는 점을 하이라이트
21. Eval 테스트 케이스 5개 — 핀치 줌, Supabase 배제/로컬 SQLite, DB 통합, 반복 안내문 1회 단순화, 오터치 방어. 각 항목에 테스트 기준과 낮은 점수 원인, 수정 내용을 표로 표현
22. 앱 실행 흐름 — 아이콘 클릭 → 카메라에 사물 비추기 → ML Kit 후보 감지 → 애매할 때 Gemini API로 키워드 변환 → 사물 클릭 → 품목 DB와 지역 DB 매칭 → 결과 카드 표시. 아이콘/이미지를 써서 쉽게 설명
23. 초기 비용 0원 마케팅 실행법 — 스토어 ASO, 지인 20명 테스트, 15초 시연 영상, 지역 커뮤니티 공유, 블로그 글 3개, 피드백 루프처럼 돈 들이지 않고 바로 할 수 있는 실제 행동을 간단히 설명
24. 배포 후 광고 및 관리 — AdMob 배너, Remote Config 횟수 제한, 보상형 광고, 출시 전 정책 검수
25. Q&A 감사합니다

각 슬라이드 하단에 발표자 메모 2문장 포함.
```

---

## 슬라이드별 짧은 프롬프트 (수정·추가용)

### Slide 2 — Problem

```
발표 슬라이드 1장. 제목 "왜 만들었나".
내용: 63세가 강남 35년 살다 일산 이사, 고무장갑·즉석밥 용기로 경고 스티커.
지역마다 규칙 반대, 하루 여러 번 헷갈림, PDF·검색앱·이웃질문의 한계.
일러스트: confused senior with trash bags. 색 #2D5A27.
```

### Slide 9 — 비교표

```
비교표 슬라이드. 열: 항목 | 내 손안의 분리배출 | RecycleAI.
행: 입력방식, 지역맞춤, 오프라인, 시각안내, 접근성, 폐가전.
RecycleAI 열에 체크 강조. 말은 쉽게.
```

### Slide 15 — Play Store

```
Google Play 출시 체크리스트 슬라이드 5 bullet:
개발자계정, 스크린샷·개인정보처리방침, 카메라·위치권한, 내부테스트→정식출시, ASO 키워드 분리수거 이사.
```

### Slide 16 — SNS

```
SNS 마케팅 슬라이드. 릴스 15초 "강남 vs 일산 고무장갑",
이사철 카카오 오픈채팅, 네이버 SEO, 환경박람회 부스, 시니어 유튜버 체험.
```

### Slide 17 — 수익화

```
수익화 4단계 표: 1 무료브랜드 2 프리미엄(광고제거·이사모드) 3 B2G아파트 4 제휴(신중).
각주: 환경교육 앱은 신뢰 우선, 과광고 지양.
```

---

## 이미지 생성 AI용 (배경·커버만)

```
Minimal flat illustration, recycling mobile app pitch deck cover,
smartphone with green neon bounding box on plastic bottle,
Korean apartment waste collection area, forest green #2D5A27,
clean white background, professional startup style, no text
```

---

## Copilot / PowerPoint Designer용

```
이 덱은 RecycleAI 분리배출 Android 앱 투자 피치입니다.
슬라이드 10·11에 업로드한 스크린샷을 좌우 배치하고,
표 슬라이드는 녹색 헤더로 통일해 주세요.
용어: ML Kit→"가벼운 AI 인식", SQLite→"앱 안 데이터베이스".
```

---

## 발표 대본 힌트 (1분 버전)

```
"분리수거를 못해서가 아니라, 동네가 바뀌어서 헷갈립니다.
환경부 공식 앱은 훌륭하지만 검색 중심이고, 우리는 카메라와 GPS로
지금 서 있는 곳의 규칙을 한 카드에 담습니다. 공공 데이터만 보여 줘서
신뢰를 지키고, Play와 SNS로 이사·시니어 시장부터 키울 계획입니다."
```

---

## 로컬 PPT 재생성

```bash
cd d:\MrSure\RecycleAi
python docs/presentation/build_presentation.py
```

출력: `docs/presentation/RecycleAI_Investor_Pitch.pptx`
