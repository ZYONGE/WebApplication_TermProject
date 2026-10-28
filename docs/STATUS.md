# 개발 현황 보드

> 관리 주체: **PM 세션** (이 파일은 PM만 수정한다)
> 각 트랙의 상세 진행 로그는 `docs/tracks/*.md` 에 해당 세션이 직접 기록한다.
> 최종 갱신: 2026-10-28 — 트랙 문서·디렉터리 확정, 모든 트랙 착수 전

---

## 1. 트랙별 현황

| # | 트랙 | 트랙 문서 | 상태 | 차단 요인 |
|---|---|---|---|---|
| 1 | UI / 디자인 테마 | `docs/tracks/UI.md` | ⬜ 착수 전 | 컨셉·디자인 테마 미전달 (공지 항목 3) |
| 2 | JSP 배선 | `docs/tracks/JSP.md` | ⬜ 착수 전 | 없음 — 화면 목록·URL 매핑 **기록이 이 트랙의 첫 산출물** |
| 3 | ERD | `docs/tracks/ERD.md` | ⬜ 착수 전 | 없음 — **즉시 착수 가능** |
| 4 | DB Schema | `docs/tracks/SCHEMA.md` | ⬜ 착수 전 | ERD 초안 대기 |

상태 표기: ⬜ 착수 전 / 🟡 진행 중 / 🟢 1차 완료 / 🔵 통합 검증 완료

---

## 2. 계약(Contract) 확정 현황

병렬 개발이 충돌 없이 수렴하기 위해 고정해야 하는 항목. (CLAUDE.md § 4.2)

| 계약 | 접점 | 상태 | 기록 주체 → 확정 위치 |
|---|---|---|---|
| 화면 목록 + URL 매핑 | UI ↔ 배선 | ⏸ **보류** | UI · JSP 배선 세션이 기록 완료 → **PM이 취합해 재개** |
| DTO 필드 계약 | 배선 ↔ ERD | ⬜ 미확정 | ERD 세션 → `docs/ERD.md` |
| CSS 클래스 네이밍 + 토큰 변수명 | UI ↔ 배선 | ⬜ 미확정 | UI 세션 → `css/base/variables.css` |
| 팝업 공통 마크업 패턴 | UI ↔ 배선 | ⬜ 미확정 | UI 세션 → `css/components/popup.css` |

**화면 목록 · URL 매핑 보류 조건**
PM이 선행 확정하지 않는다. 프론트엔드 세션과 JSP 배선 세션이 각자 트랙 문서에 화면·라우트를 기록한 뒤, PM이 두 기록을 대조해 `README.md` URL 매핑 명세로 확정한다. 불일치가 나오면 PM이 조정한다.

**이미 고정된 공통 계약** (CLAUDE.md § 4.1 공유 지점 해소 규칙)

| 계약 | 값 |
|---|---|
| 업로드 저장 경로 | `WebContents/upload/` |
| 업로드 웹 참조 경로 | `/upload/<파일명>` |
| 목업 위치 | `WebContents/mockup/*.html` |
| 레이아웃 인계 | UI `mockup/_layout.html` → JSP `common/header.jsp`·`footer.jsp` |
| JS 분담 | `js/ui.js`(UI) / `js/validate.js`(배선) |
| DB 접속 | `META-INF/context.xml` ← `context.xml.example` 복사 |

---

## 3. 실측 진행도 (2026-10-28)

| 영역 | 측정 | 비고 |
|---|---|---|
| CSS | 16개 파일 모두 주석 1줄 | 뼈대만 존재 |
| 목업 HTML | 0개 | `WebContents/mockup/` 신설, 비어 있음 |
| JSP | 0개 | — |
| Java (`shop/**`) | 0개 | 패키지 디렉터리만 존재 |
| `sql/schema.sql`, `sql/data.sql` | 0 byte | — |
| `docs/ERD.md`, `docs/CRUD.md` | 0 byte | — |
| `README.md` 본문 섹션 | 미작성 (템플릿) | PM이 취합 |
| 라이브러리 | 6개 확보 완료 | `WEB-INF/lib` |
| `build/` git 추적 | 비추적 확인 완료 | 문제 없음 |

---

## 4. 구조 변경 이력

| 날짜 | 변경 | 사유 |
|---|---|---|
| 2026-10-28 | `WebContents/mockup/` 신설 | UI의 `.html` 목업과 배선의 `.jsp`가 `product/` 등 같은 디렉터리를 공유해 소유권 충돌 발생 → 분리 |
| 2026-10-28 | `META-INF/context.xml.example` 신설 | `.gitignore`가 참조하는 템플릿 파일이 실제로 없어, 각 세션이 DB 접속 설정을 만들 수 없었음 |
| 2026-10-28 | `docs/tracks/` 신설 | 트랙별 작업 지시·진행 로그·인계 사항을 세션 간에 전달 |
| 2026-10-28 | `js/` 파일 단위 소유권 분리 | `ui.js`(UI) / `validate.js`(배선) — 디렉터리 공유 충돌 제거 |
| 2026-10-28 | `WebContents/css/DECISIONS.md` 신설 | UI 트랙의 결정(토큰 변수명·클래스명·팝업 패턴)을 작업 디렉터리에서 기록. JSP 배선 트랙이 읽어가는 단일 창구 |
| 2026-10-28 | 작업 디렉터리별 중첩 `CLAUDE.md` 3개 신설 (`src/main/java/shop/`, `docs/`, `sql/`) | 새 세션이 생성 직후 자기 역할·쓰기 권한·읽기 전용 경계를 자동으로 인식. 병렬 세션 간 동시 편집 충돌 방지 (UI/UX 제외) |

> Dynamic Web Project 구성(웹 루트 · 소스 폴더 · 출력 경로 · `WEB-INF` / `META-INF`)은 변경하지 않았다.

---

## 5. 세션 ↔ PM 보고 규약

각 트랙 세션은 작업을 마칠 때 **자기 트랙 문서**(`docs/tracks/*.md`)의 아래 항목을 직접 갱신한다. PM은 이를 읽고 이 보드와 `README.md`를 갱신한다.

1. **§6 진행 상황 로그** — 날짜 / 작업 / 산출물 파일 경로
2. **§7 다른 트랙에 넘길 요청** — 자기 소유가 아닌 파일의 수정 필요 사항
3. **§8 미결 / 질문** — 막힌 지점, PM 판단이 필요한 사항
4. 계약에 영향을 주는 변경(필드명·URL·클래스명·변수명)은 §7에 **반드시** 적는다

> **모든 세션은 `main` 브랜치에서만 작업한다.** 추가 브랜치·worktree를 만들지 않으므로 병합 단계가 없고, 충돌 방지는 § 2 파일 소유권과 `CLAUDE.md` § 4.4 동시 작업 수칙에 전적으로 의존한다. `git push` 는 PM만 수행한다.
