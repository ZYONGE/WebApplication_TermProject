# 트랙 — UI / 디자인 테마

> 소유 세션: **UI 트랙** · 이 문서는 해당 세션이 직접 갱신한다.
> 최종 갱신: 2026-10-30 · 상태: ⬜ 착수 전 · 계약만 확정

소유 파일은 `css/**` (결정 기록 `css/DECISIONS.md`) · `mockup/**` · `images/**` · `js/ui.js` 다.
소유권 규칙은 `CLAUDE.md` 의 트랙 표를, 토큰·클래스·팝업 계약은 `css/DECISIONS.md` 를 따른다.

---

## 1. 현재 상태

| 항목 | 상태 |
|---|---|
| 토큰 이름 · 클래스 네이밍 · 팝업 패턴 | 🟢 확정 — `css/DECISIONS.md` |
| `css/` 17개 파일 | ⬜ 빈 뼈대 (주석 한 줄씩) |
| `mockup/` | ⬜ 없음 |
| `images/` | ⬜ 비어 있음 |
| 디자인 컨셉 | ⬜ 미전달 — 색·타이포 **값** 대기 |

컨셉 전이라도 토큰 **이름**과 클래스명은 확정돼 있으므로, JSP 배선은 `css/DECISIONS.md` 를 읽고 마크업을 먼저 쓸 수 있다.

## 2. 착수 시 순서

`base/reset.css` → `base/variables.css` → `base/layout.css` → `components/*` 6개 → `pages/*` 8개 → `mockup/*.html`.
페이지 CSS 는 main · member · product · cart · order · mypage · admin · notice 8개다 (`notice_*` 는 STATUS C5 확정 대기).

## 3. 다른 트랙에 넘길 요청

| 대상 | 내용 | 상태 |
|---|---|---|
| — | 없음 | — |

## 4. 미결

- 컨셉 전달 대기. 토큰 **이름**은 확정, **값**만 비어 있다.
- 계약 미결(STATUS C5 · C6 · D4 · P5)은 `docs/STATUS.md` 를 따른다.
