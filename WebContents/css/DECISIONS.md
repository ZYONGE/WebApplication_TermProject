# 프론트엔드 결정 기록

> 소유: **UI 트랙** · 타 트랙은 읽기만 한다. **JSP 배선은 마크업을 쓰기 전 이 문서를 읽는다.**
> 색·폰트 값은 컨셉 전달 후 확정한다. 이름은 이미 확정이다.
> PM 결정 대기 항목은 `docs/STATUS.md` 에 있다 — 여기서 중복하지 않는다.

---

## 1. 디자인 토큰 이름 (D-01 ~ D-06)

`:root` 선언은 `css/base/variables.css` **한 곳**뿐이다. 나머지는 `var()` 로만 참조한다.
이름은 값이 아니라 **역할**로 짓는다 (`--color-primary`, `--color-blue` 아님).
하드코딩 금지. 예외는 `0` · `1px` · `100%` · `auto`.

| 군 | 이름 |
|---|---|
| 바탕·글자·선 | `--color-bg` `--color-surface` `--color-text` `--color-text-weak` `--color-text-inverse` `--color-border` `--color-border-strong` |
| 역할색 | `--color-primary` `--color-primary-dark` `--color-primary-light` `--color-accent` `--color-danger` `--color-success` |
| ~~청사진 표식~~ | `--bp-*` — **2026-11-10 레이아웃 완성 단계에서 삭제.** 모든 CSS 가 역할색 토큰만 쓴다 |
| 타이포 | `--font-base` `--font-size-base` `--font-size-small` `--font-size-title` `--line-height-base` |
| 간격 | `--space-{xs,sm,md,lg,xl}` — 4px 배수 |
| 레이아웃 | `--layout-width` (1200px 고정) `--layout-side-width` `--radius-sm` `--radius-md` `--z-popup-backdrop` `--z-popup` |

## 2. 클래스 네이밍 (D-08 ~ D-11)

- 소문자, 하이픈 없음, BEM 아님. 형태는 **`블록_요소`** 1단 언더스코어 (`prodcard_thumb`).
- **접두어가 정의 파일을 결정한다** (아래 표). 같은 클래스를 두 파일에 두지 않는다.
- 상태는 별도 클래스를 함께 붙이고 CSS 는 한정 선택한다 — `class="prodcard soldout"` → `.prodcard.soldout`. 상태 어휘: `active` `on` `soldout` `disabled`.
- 폼과 목록은 테이블 마크업을 쓴다. `formtable` 은 `th`=레이블 / `td`=입력.
- 각 CSS 파일 첫머리 주석에 그 파일이 소유한 클래스를 적어 둔다. **클래스 목록의 정본은 CSS 파일이다.**

| 접두어 | 정의 파일 |
|---|---|
| `wrap` `container` `page_*` | `base/layout.css` |
| `btn*` | `components/button.css` |
| `formtable*` `field*` `required` `btnstyle` | `components/form.css` |
| `prodcard*` `prodlist` | `components/card.css` |
| `listtable*` `col_*` `empty` | `components/table.css` |
| `paging*` | `components/pagination.css` |
| `popup*` `toast*` | `components/popup.css` |
| `tab*` | `components/tab.css` — 조합 탭 분류 탭 (2026-11-10 신설, IMPACT K12) |
| `warnbox*` | `components/warnbox.css` — 동시 섭취 경고 박스. 상품상세 · 장바구니 · 주문서 · 조합 탭 공용 (2026-11-10 신설, IMPACT K12) |
| `main_*` `member_*` `prod_*` `cart_*` `order_*` `my_*` `admin_*` `notice_*` `custom_*` | 해당 `pages/*.css` (`custom_*` 은 `pages/custom.css`, 2026-11-10 신설) |

- `dev/*.jsp` 는 `admin_*` 을 재사용한다. dev 전용 접두어를 만들지 않는다.
- `qna_answer` 는 접두어 규칙 밖이다. 임시로 `components/table.css` 에 둔다 (STATUS.md C5).
- `pages/member.css` 는 비어 있는 것이 맞다. 회원 화면은 레이아웃과 폼 컴포넌트만 쓴다.

## 3. 로드 순서

`base/reset.css` → `base/variables.css` → `base/layout.css` → `components/*` → `pages/*`
페이지 CSS 는 `pageCss` 변수로 건다.

## 4. 팝업 패턴

- 라우팅 페이지와 팝업을 구분해 만든다. 팝업 스타일은 `components/popup.css` 에만 둔다.
- 마크업은 HTML 표준 `<dialog class="popup">` + `showModal()` (`js/ui.js`). 여닫기는 `data-popup-open` / `data-popup-close` 속성으로 건다.
- 구조: `popup_form` > `popup_head` · `popup_body` · `popup_foot`. 닫기 버튼은 `btn btn_sub popup_close`.
- 토스트는 `toast` / `toast toast_error`.

## 4.1 탭 패턴 (2026-11-10)

- 팝업과 같은 속성 패턴이다. 묶음 `data-tab` 안의 버튼 `data-tab-open="패널id"` 를 누르면 `js/ui.js` 의 `openTab` 이 그 패널만 연다.
- 구조: `tab` > `tab_list` > `tab_btn` (+`on`) / 패널 `tab_panel` (+`on`). 닫힌 패널에는 `hidden` 속성이 함께 걸려 CSS 없이도 한 패널만 보인다.

## 4.2 영양제 컨셉 마크업 (2026-11-10 · `docs/CONCEPT.md`)

| 자리 | 마크업 |
|---|---|
| 경고 박스 | `section.warnbox` > `h3.warnbox_title` · `ul.warnbox_list` > `li.warnbox_conflict` (금지 조합) / `li.warnbox_dup` (중복 섭취) > `strong.warnbox_label` · `span.warnbox_msg` · `p.warnbox_note` (의학적 조언 아님 고지 — 항상 함께). 경고 없음은 `p.warnbox_ok`. **버튼을 가리거나 흐리게 하지 않는다** — 경고는 구매를 막지 않는다 |
| 섭취 일수 선택 | 상품상세 `prod_days` · 조합 탭 `custom_days`. 빠른 선택 `btn btn_small` (+`on`) + 숫자 입력. 동작은 `js/custom.js` 의 `data-days-value` · `data-days-target` |
| 트랙 구성표 | `section.prod_track` > `listtable` |
| 조합 탭 | `form.custom` 3분할 격자 = `custom_tabs`(탭) · `custom_list`(패널) · `custom_summary`(내 조합) |

- 경고 색은 기존 `--color-danger` 로 시작한다. `--color-warning` 신설은 컨셉 전달 때 정한다.

## 4.3 레이아웃 완성 단계 (2026-11-10)

컨셉(색 · 글꼴 값) 없이 구조를 완성했다. 컨셉이 오면 `variables.css` 값만 바꾼다.

| 자리 | 구조 |
|---|---|
| 헤더 | 격자 3열: 로고 · 검색 · 유틸 / 안내 띠 / 내비. 소분류는 펼침 메뉴(`:hover` · `:focus-within`). `조합 맞추기` 는 채운 탭으로 오른쪽 |
| 지금 화면 | `js/ui.js` 가 `page_nav` · `admin_nav` · `my_nav` 링크 중 현재 주소와 같은 것에 `on` |
| 상품 카드 | `prodlist` 4열 격자, 썸네일 정사각형 |
| 상품상세 | `prod_detail` 2단 격자 = `prod_gallery`(480px) · `prod_info`(`prod_cate` · h2 · `prod_price` · `prod_spec` · `prod_buy` · `prod_wish` · 경고). 설명 · 구성 · 리뷰 · 문의는 아래 전체 폭 |
| 회원 화면 | `formtable_form member_box` — 520px 가운데 |
| 관리자 · 마이페이지 | `admin_nav` · `my_nav` 는 탭 줄, `admin_filter` · `prod_filter` 는 한 줄 필터 띠 |
| 팝업 | 머리 · 발 고정(sticky), 몸만 스크롤 |
| 이미지 없음 | `ui.js` 가 실패한 `<img>` 를 "[이미지 없음]" 글자로 바꾼다 (STATUS P5) |

새 클래스: `prod_info` · `prod_cate` · `prod_price` · `prod_spec` (pages/product.css), `member_box` (pages/member.css). 기존 접두어 안이라 표 변경은 없다.
하드코딩 예외(0 · 1px · 100% · auto) 밖의 px 는 컴포넌트 치수(버튼 높이 · 썸네일 폭 등)에만 남았다 — 컨셉 때 토큰화 여부를 정한다.

## 5. 미결

- **색·타이포 값** — 컨셉 전달 대기. 현재 중립 무채색 임시값이다 (`--bp-*` 표식색은 삭제됨).
- 그 밖의 계약 미결(`notice_*` 추인, `qna_answer` 귀속, `popup_sub` 신설, `soldout` 의미, 반응형)은 `docs/STATUS.md` 의 PM 결정 대기 표를 따른다.
