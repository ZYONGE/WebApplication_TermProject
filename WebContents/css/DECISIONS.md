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
| 청사진 표식 | `--bp-{image,input,action,data,popup,nav,state}` 와 각 `-line` — **컨셉 전달 시 통째로 삭제** |
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
| `main_*` `member_*` `prod_*` `cart_*` `order_*` `my_*` `admin_*` `notice_*` | 해당 `pages/*.css` |

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

## 5. 미결

- **색·타이포 값** — 컨셉 전달 대기. 현재 무채색 임시값과 `--bp-*` 표식색이 들어가 있다.
- 그 밖의 계약 미결(`notice_*` 추인, `qna_answer` 귀속, `popup_sub` 신설, `soldout` 의미, 반응형)은 `docs/STATUS.md` 의 PM 결정 대기 표를 따른다.
