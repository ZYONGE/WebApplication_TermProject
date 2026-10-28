# 트랙 1 — UI / 디자인 테마

> 소유 세션: **UI / 디자인 테마 세션** · 이 문서는 해당 세션이 직접 갱신한다.
> 최종 갱신: 2026-10-28 (PM, 초기 생성) · 상태: ⬜ 착수 전

---

## 1. 소유 파일 — 이 트랙만 수정한다

| 경로 | 내용 |
|---|---|
| `WebContents/css/base/variables.css` | 디자인 토큰 `:root` **단일 정의 지점** |
| `WebContents/css/base/reset.css` | 브라우저 기본 스타일 초기화 |
| `WebContents/css/base/layout.css` | header / nav / footer / container |
| `WebContents/css/components/*.css` | button, form, card, table, pagination, **popup** |
| `WebContents/css/pages/*.css` | main, member, product, cart, order, mypage, admin |
| `WebContents/mockup/*.html` | **정적 HTML 목업 전용 디렉터리** |
| `WebContents/js/ui.js` | 팝업·모달·탭·드롭다운 등 표현 인터랙션 |
| `WebContents/images/**` | 로고·배너·아이콘 등 정적 이미지 |

## 2. 읽기만 하는 파일

- `WebContents/**/*.jsp` — 마크업 구조 참고용. **수정 금지** (JSP 배선 트랙 소유)
- `WebContents/js/validate.js` — 폼 검증. **수정 금지** (JSP 배선 트랙 소유)
- `docs/tracks/JSP.md` — 배선 트랙이 요구하는 클래스명·구조 확인

## 3. 착수 조건

| 조건 | 상태 |
|---|---|
| 전체 컨셉 · 디자인 테마 · 인터랙션 방식 전달 (공지 항목 3) | ⬜ 대기 |
| 화면 목록 기록 | ⬜ **이 세션의 첫 산출물** → PM이 JSP 트랙 기록과 대조해 확정 |

> **컨셉 전달 전에 가능한 작업**: 디자인 토큰의 **변수명**과 CSS **클래스 네이밍** 확정, `reset.css` 작성.
> 색·폰트·간격의 **값**은 컨셉 전달 후에 채운다.

## 4. 작업 규칙

- 빌드 도구·전처리기·npm 금지. 순수 CSS만. (공지 항목 2)
- 외부 CDN·원격 폰트 금지 — localhost 오프라인에서 깨지면 안 된다. (공지 항목 10)
- 색·폰트·간격은 `variables.css`에서만 선언하고 나머지는 전부 `var()` 참조.
- 로드 순서: `reset.css` → `variables.css` → `layout.css` → `components/*` → `pages/*`
- **라우팅 페이지와 팝업을 구분해 개발한다.** (공지 항목 5)
  - 라우팅 페이지 → `css/pages/*.css`
  - 팝업 / 모달 / 토스트 → `css/components/popup.css` + 공통 마크업 패턴
- 목업은 `WebContents/mockup/`에만 둔다. CSS 참조는 `../css/...` 상대 경로로 동작한다.
- `common/header.jsp` · `footer.jsp`의 레이아웃 마크업은 `mockup/_layout.html`로 **제시**만 한다. 이식은 JSP 트랙이 수행한다.
- 상품 이미지 경로는 웹 경로 `/upload/<파일명>` 으로 고정. (업로드 처리는 JSP 트랙)

## 5. Done 기준

- [ ] 화면 목록(라우팅 페이지 + 팝업) 기록 — PM 계약 재개 트리거
- [ ] 디자인 토큰 변수명 + CSS 클래스 네이밍 확정
- [ ] `base/` 3개 파일 작성 완료
- [ ] `components/` 6개 파일 작성 완료 (팝업 포함)
- [ ] `pages/` 7개 파일 작성 완료
- [ ] 라우팅 페이지 목업 전체 + 팝업 목업 전체
- [ ] 브라우저에서 목업 직접 열어 레이아웃 깨짐 없음 확인

## 6. 진행 상황 로그

| 날짜 | 작업 | 산출물 |
|---|---|---|
| — | 착수 전 | — |

## 7. 다른 트랙에 넘길 요청

| 대상 | 요청 내용 | 상태 |
|---|---|---|
| — | — | — |

## 8. 미결 / 질문

- 컨셉 전달 대기 중.
- 화면 목록은 JSP 배선 세션도 각자 기록한다. 두 기록이 어긋나면 PM이 조정하므로, 이 문서에는 **UI 관점에서 필요한 화면**을 그대로 적는다.
