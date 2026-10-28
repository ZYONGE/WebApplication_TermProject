# 프론트엔드 결정 기록 (Frontend Decisions)

> 관리 주체: **UI / 디자인 테마 세션** — `WebContents/css/**` 소유 (CLAUDE.md § 4.1)
> 최종 갱신: 2026-10-28 — 네이밍 계약 1차 확정, 스타일 코드 착수 전
> 상위 규약: `CLAUDE.md` / 현황 보드: `docs/STATUS.md`

---

## 0. 이 문서의 범위

CLAUDE.md § 4.2 계약 3번 — "색·폰트 **값**은 컨셉 전달 후 확정하되, **변수명과 클래스명은 먼저 고정**한다" — 를 이행한 기록이다.

| 표기 | 뜻 |
|---|---|
| **D-nn** | 확정. UI 트랙 소유 결정. 변경 시 PM 경유 후 JSP 배선 세션에 전파한다. |
| **O-nn** | 미결. 결정권이 PM 또는 선행 조건에 있다. 확정 전까지 해당 영역 코드를 쓰지 않는다. |

### 전제로 승계하는 상위 규약 (이 문서가 정하지 않음)

- CSS 로드 순서: `base/reset.css` → `base/variables.css` → `base/layout.css` → `components/*` → `pages/*`
- 빌드 도구 · 전처리기 · npm 미도입 (CLAUDE.md § 2.2)
- 외부 CDN · 원격 폰트 로드 금지 (CLAUDE.md § 5)
- 라우팅 페이지와 팝업을 분리해 개발 (CLAUDE.md § 3 Stage 1)

### 관례의 출처

`2학년/2학기/웹프로그래밍응용` 실습코드 — HTML 78개 · CSS 55개 · JSP 13개를 조사했다.
가장 최신 도달점은 `midterm/bookdesign.css` (flex 레이아웃, `box-sizing: border-box`, `border-radius`)이며, 이 문서의 치수 · 폰트 기준값은 모두 여기서 끌어왔다.

---

## 1. 토큰 설계 (`base/variables.css`)

### D-01. 토큰 이름은 **값이 아니라 역할**로 짓는다

`--color-blue` (X) → `--color-primary` (O)

**근거**: 컨셉이 "블루 → 올리브"로 바뀌어도 이름이 살아남아야 한다. 이름이 값을 가리키면 컨셉 전달 때 전 파일을 고쳐야 하고, 계약 3번의 전제가 무너진다.

### D-02. `:root` 선언 지점은 `base/variables.css` **단 하나**

다른 파일에서 `:root` 재선언을 금지한다. 토큰을 쓰는 쪽은 `var()` 로만 참조한다.

**근거**: CLAUDE.md § 3 Stage 1 "단일 정의 지점". 두 파일에서 같은 토큰을 재정의하면 로드 순서에 따라 값이 바뀌어 디버깅이 불가능해진다.

### D-03. 색 · 간격 · 폰트 하드코딩 금지

예외는 네 가지만 — `0`, `1px`(헤어라인 선), `100%`, `auto`.

### D-04. 값 미정 구간은 **임시 중립값 + `/* TODO: 컨셉 */`** 로 둔다. 빈 값으로 두지 않는다

**근거**: 임시값이라도 들어 있어야 지금부터 화면이 렌더되고 검토가 가능하다. 컨셉이 도착하면 이 파일 하나만 고쳐도 전 페이지가 따라온다 — 그것이 토큰을 도입하는 유일한 이유다.

### D-05. 간격은 **4px 배수 스케일** (`--space-xs` ~ `--space-xl`)

**근거**: 새 규칙이 아니다. `midterm/bookdesign.css` 의 실측 간격값(6 · 8 · 14 · 16 · 22 · 24 · 36 · 44)이 이미 거의 4의 배수다. 쓰고 있던 리듬을 명문화하는 것이다.

### D-06. 레이아웃은 **고정폭 1200px 중앙정렬**. 반응형은 범위 외

**근거**: 실습코드 전체가 고정폭(989px / 1200px)이고 반응형 전례가 없다. 평가 기준(CLAUDE.md § 2 항목 11)은 프론트 → 배선 → DB 의 연결성이지 뷰포트 대응이 아니다.
→ PM 컨셉이 반응형을 요구하면 § 5 에 따라 재검토한다.

### D-07. CSS 포맷 규약 — 실습 관례를 그대로 승계

| 항목 | 규약 |
|---|---|
| 첫 줄 | `@charset "utf-8";` 항상 |
| 들여쓰기 | **탭** |
| 속성 | 롱핸드 전개 — `padding-top/right/bottom/left`, `border-top-width/style/color` 를 각각 |
| 주석 | 한글 블록 주석 (`/*상단*/`, `/*메인페이지*/`) |

**근거**: CLAUDE.md § 2.2 — "CSS/HTML은 손으로 짠 것처럼 읽기 쉽고 일관된 코드여야 한다". 롱핸드 전개는 DreamWeaver CSS 패널이 만드는 형태이고 실습코드 55개 CSS 전부가 이 형태다. 축약형으로 쓰면 도구가 바뀐 흔적이 남는다.

---

## 2. 클래스 네이밍 계약

### D-08. 소문자 · 하이픈 없음 · BEM 아님. 형태는 **`블록_요소`** 1단 언더스코어

```
prodcard        prodcard_thumb      prodcard_name       prodcard_price
```

**근거**: 실습코드는 `page_header` · `leftmenu` · `tablestyle` · `btnstyle` · `s_banner` 처럼 소문자 + 접두어 언더스코어를 쓰고 하이픈과 BEM 전례가 없다. 외형은 승계하되, 실습의 즉흥 작명(`rightcon` · `rban1` · `sb1`)은 7개 페이지 × 3인 작업에서 버티지 못하므로 체계만 넣는다.

### D-09. **접두어가 파일 소유권을 결정한다** — 이 계약의 핵심

| 접두어 | 정의 파일 |
|---|---|
| `wrap`, `container`, `page_*` | `base/layout.css` |
| `btn*` | `components/button.css` |
| `formtable`, `field*` | `components/form.css` |
| `prodcard*`, `prodlist` | `components/card.css` |
| `listtable*`, `col_*` | `components/table.css` |
| `paging*` | `components/pagination.css` |
| `popup*`, `toast*` | `components/popup.css` |
| `main_*` `member_*` `prod_*` `cart_*` `order_*` `my_*` `admin_*` | 해당 `pages/*.css` |

**근거**: 클래스명만 보면 정의 위치가 자동으로 정해지므로, 두 세션이 같은 클래스를 서로 다른 파일에 중복 정의하는 사고가 **구조적으로** 불가능해진다. CLAUDE.md § 4.1 파일 소유권의 CSS 내부 버전이다.
`page_*` 는 실습의 `page_header` / `page_content` / `page_footer` 계보를 그대로 가져온다.

### D-10. 상태는 별도 클래스 + 한정 선택자

마크업은 `class="prodcard soldout"`, CSS는 `.prodcard.soldout { }` 로 한정한다. 상태 클래스를 단독으로 선택하지 않는다.

**상태 어휘**: `active` · `on` · `soldout` · `disabled`

**근거**: 마크업은 깔끔하게 두면서 전역 충돌을 막는다. `.soldout` 을 단독 선택하면 다른 블록의 같은 상태와 스타일이 섞인다.

### D-11. 폼과 목록은 **테이블 마크업**을 유지한다

- 폼: `<table class="formtable">` + `th`(레이블) / `td`(입력) 쌍
- 목록: `<table class="listtable">`

**근거**: 실습코드의 `#registerform` · `#mylist` · `#qaboard` 가 모두 이 형태이고, JSP 의 `c:forEach` 배선과도 그대로 맞물린다. ID 선택자만 클래스로 바꿔 재사용 가능하게 한다.

### D-12. 계약 기록 위치 = **이 문서 + 각 CSS 파일 주석 헤더**

각 CSS 파일 상단에 그 파일이 소유한 접두어와 클래스 목록을 한글 주석으로 박는다.

**근거**: `docs/` 는 PM · ERD 트랙 소유라 새 문서를 만들지 않는다. `docs/STATUS.md` § 2 가 확정 위치를 이미 `variables.css` / `popup.css` 로 지정해 뒀다. JSP 세션용 단일 참조 목록이 필요하면 PM 경유로 `README.md` UI 섹션에 취합한다.

---

## 3. 미결 항목 — 확정 전까지 해당 코드 착수 금지

| # | 항목 | 결정권 | 막히는 작업 |
|---|---|---|---|
| **O-01** | 네비 · 버튼을 **이미지 슬라이싱**으로 갈지 **텍스트 + CSS** 로 갈지 | PM | `components/button.css` 전체. 이미지로 가면 `btn*` 7개가 무의미해지고 이 파일이 사실상 빈 파일이 된다. 로그인 상태별 헤더 분기 · 장바구니 수량 · 페이지네이션 · 폼 검증 메시지는 이미지로는 배선 불가. |
| **O-02** | 팝업을 **`window.open()` 별창**으로 갈지 **인페이지 모달**로 갈지 | PM | `components/popup.css` 전체. 별창이면 실습 `popstyle.css` 처럼 별창 전용 `body` 스타일이 되고, 인페이지면 `popup_backdrop` + z-index 체계가 필요하다. 두 설계가 공유하는 클래스가 거의 없다. |
| **O-03** | doctype — `<!DOCTYPE html>` 인지 실습 관례(`HTML 4.01 Transitional`) 유지인지 | PM | 공통 JSP 헤더 (JSP 트랙 소유). 실습 JSP 13개는 전부 4.01 Transitional. |
| **O-04** | 색상 토큰 **값** 13개 | PM 컨셉 | 모든 페이지 스타일. D-04 에 따라 임시 무채색으로 채워 두고 대기. |
| **O-05** | 타이포 **값** — 컨셉이 폰트 · 크기를 지정하면 덮어쓴다 | PM 컨셉 | 없음. 실습 기준값으로 선행 가능. |

> **O-01 · O-02 는 § 4 부록 B 의 클래스 목록을 확정하기 위한 선행 조건이다.**

---

## 4. 부록 — 확정 예정 목록

### A. 토큰 (`base/variables.css`)

**색상** — 전부 O-04 (컨셉 대기, 임시 무채색)

| 토큰 | 역할 |
|---|---|
| `--color-primary` | 브랜드 주색 — 로고, 주요 버튼, 활성 상태 |
| `--color-primary-dark` | hover / active |
| `--color-primary-light` | 배경 틴트 |
| `--color-accent` | 강조 — 할인가, 배지 |
| `--color-text` | 본문 |
| `--color-text-weak` | 보조 설명, 도움말 |
| `--color-text-inverse` | 어두운 배경 위 글자 |
| `--color-bg` | 페이지 배경 |
| `--color-surface` | 카드 · 패널 배경 |
| `--color-border` | 기본 선 |
| `--color-border-strong` | 테이블 상 / 하단 굵은 선 |
| `--color-danger` | 삭제, 검증 실패 |
| `--color-success` | 주문완료, 검증 통과 |

`--color-border-strong` 은 실습 관례를 반영한 것이다. `CHAPTER13/Homepage/style.css` 의 `#qaboard` 가 `border-top: 3px` + `border-bottom: 2px` 로 목록 테이블을 감싸는데, 이것이 게시판 테이블의 고정 관용구라 별도 토큰이 필요하다.

**타이포 · 치수 · 레이아웃** — 실습 기준값으로 선행 (O-05)

| 토큰 | 값 | 근거 |
|---|---|---|
| `--font-base` | `"Apple SD Gothic Neo", "맑은 고딕", "나눔고딕", "돋움", Dotum, sans-serif` | `midterm/bookdesign.css` |
| `--font-size-base` | `15px` | 같음 (초기 챕터의 `9pt` 는 폐기된 값) |
| `--font-size-small` | `12px` | 같음 — `.help` 텍스트 |
| `--font-size-title` | TODO | — |
| `--line-height-base` | `160%` | 실습 150~170% 대역의 중간 |
| `--space-xs` ~ `--space-xl` | 4px 배수 | D-05 |
| `--layout-width` | `1200px` | D-06 |
| `--layout-side-width` | `228px` | `bookdesign.css` `.leftmenu` |
| `--radius-sm` / `--radius-md` | TODO | `bookdesign.css` 는 `6px` |
| `--z-popup-backdrop` / `--z-popup` | TODO | O-02 결정 후 |

### B. 컴포넌트 클래스

```
button.css      btn  btn_primary  btn_sub  btn_danger  btn_small  btn_block
form.css        formtable  field  field_help  field_error  required  btnstyle
card.css        prodlist  prodcard  prodcard_thumb  prodcard_name
                prodcard_price  prodcard_price_was  prodcard_badge
table.css       listtable  listtable_head  col_*  empty
pagination.css  paging  paging_num  paging_prev  paging_next  on
popup.css       popup_backdrop  popup  popup_head  popup_body
                popup_foot  popup_close  toast
```

`formtable` 과 `btnstyle` 은 실습 `#registerform` 의 `th`/`td` 폼 + `.btnstyle` 버튼 행 패턴을 승계한 이름이다.
`btn*` 은 O-01, `popup*` 은 O-02 결정에 따라 달라진다.

---

## 5. 재검토 트리거

아래 중 하나가 발생하면 해당 결정을 다시 올린다. 임의로 바꾸지 않는다.

| 트리거 | 재검토 대상 |
|---|---|
| PM 컨셉이 반응형을 요구 | D-06 |
| PM 컨셉이 폰트 · 타이포를 지정 | O-05, `--font-*` 전체 |
| O-01 이 "이미지 슬라이싱"으로 결정 | 부록 B 의 `btn*` 전체, D-11 폼 버튼 행 |
| O-02 가 "별창"으로 결정 | D-09 의 `popup*` 소유 행, 부록 B 의 `popup*` |
| JSP 배선 세션이 계약에 없는 클래스를 요청 | D-09 접두어 표 (PM 경유로 추가) |

---

## 6. 변경 이력

| 날짜 | 내용 |
|---|---|
| 2026-10-28 | 최초 작성 — D-01 ~ D-12 확정, O-01 ~ O-05 미결로 기록 |
