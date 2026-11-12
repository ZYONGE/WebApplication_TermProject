# 트랙 — UI / 디자인 테마

> 소유 세션: **UI 트랙** · 이 문서는 해당 세션이 직접 갱신한다.
> 최종 갱신: 2026-11-10 · 상태: 🟢 레이아웃 완성 직전 · 색/타이포 값만 컨셉 대기

소유 파일은 `css/**` (결정 기록 `css/DECISIONS.md`) · `mockup/**` · `images/**` · `js/ui.js` 다.
소유권 규칙은 `CLAUDE.md` 의 트랙 표를, 토큰·클래스·팝업 계약은 `css/DECISIONS.md` 를 따른다.

---

## 1. 현재 상태 (샌드박스)

| 항목 | 상태 |
|---|---|
| `css/` 17개 파일 | 🟢 1165줄 — 청사진(흰 바탕 + 객체 종류 구분색). 컨셉 값 미반영 |
| `mockup/` 8개 | 🟢 `_layout`(헤더 4종) · `main` · `product_view`(구매영역 3종) · `cart_list` · `admin_products` · `order_form` · `mypage_index` · `notice_view` |
| 실제 Tomcat 검증 | 🟢 CSS 10개 200 로드 · 팝업 여닫힘 · `c:forEach` 반복 · 한글 정상 |
| 디자인 컨셉 | ⬜ 미전달 — 색·타이포 **값** 대기 |
| `images/` | ⬜ 비어 있음 |

`main` 은 CSS 미착수(빈 뼈대)다. 여기 산출물은 `main` 에 병합하지 않는다.

## 1.1 영양제 컨셉 전환 (2026-11-10)

| 항목 | 내용 |
|---|---|
| 컴포넌트 신설 | `components/tab.css` (`tab*`) · `components/warnbox.css` (`warnbox*`) — IMPACT K12 접두어, `DECISIONS.md` § 2 · § 4.1 · § 4.2 에 기록 |
| 페이지 신설 | `pages/custom.css` (`custom_*`) — 조합 탭 3분할 격자 180 / 가변 / 340 |
| 기존 페이지 | `prod_track` · `prod_days*` (product), `cart_notice` (cart), `main_custom` · `main_track` (main), `admin_check` (admin), `my_combo_off` (mypage), `page_nav_custom` (layout) |
| `js/ui.js` | 탭 `data-tab-open` 패턴 (`openTab`) — 팝업과 같은 방식, 패널에 `hidden` 동시 적용 |
| 목업 | 신설 `mockup/custom.html`. `_layout` 내비 · `main` · `product_view`(트랙 구성 · 경고 · 일수) · `cart_list`(경고 · 일수) 수정, 전 목업 내비를 영양제 카테고리로 |
| 토큰 | 신설 없음. 경고는 `--color-danger` 로 시작 (`--color-warning` 은 컨셉 전달 때) |
| 확인 | 정적 서버로 `mockup/custom.html` 렌더 · 탭 전환 확인 |
| 이미지 없음 | `js/ui.js` 가 로드 실패한 `<img>` 를 "[이미지 없음]" 글자로 바꾼다 — 샘플 thumbnail 파일이 없어서(STATUS P5). 파일이 생기면 저절로 이미지가 보인다 |

## 1.2 레이아웃 완성 단계 (2026-11-10)

컨셉 없이 "완성 직전" 레이아웃으로 정리했다. 상세는 `css/DECISIONS.md` § 4.3.

| 항목 | 내용 |
|---|---|
| 토큰 | `--bp-*` 삭제, 역할색 토큰만 사용. 값은 중립 무채색 (컨셉 때 `variables.css` 만 교체) |
| CSS 전면 정리 | layout · button · form · table · card · pagination · popup · tab · warnbox, pages main · product · cart · order · mypage · admin · notice · member · custom |
| 마크업 변경 (JSP) | `product/view.jsp` 상단 2단(`prod_info`), 경고 박스를 담기 상자 아래로 · `member/login` · `join` 에 `member_box` |
| `js/ui.js` | 내비 현재 화면 `on` 표시 |
| 확인 | 1366px 화면에서 메인 · 목록 · 상세 · 로그인 · 장바구니 · 주문서 · 주문상세 · 조합 맞추기 · 내 조합 · 관리자(대시보드 · 상품 팝업 · 주문 · 금지 조합) · 공지 스크린샷 점검 |

## 1.3 UI 리뷰 반영 (2026-11-12)

- 팝업: 입력 레이블은 칸 위 한 줄(`label[for]`), 답변 팝업 인용문 들여쓰기 제거
- 회원가입: 아이디 + 중복확인 한 줄(`member_idrow`), 결과 색 구분(`member_ok` · `member_ng`)
- 페이지 안내 `paging_info` (1 / 5 표기), 조합 탭 재고 경고 `custom_picked_warn`, 트랙 불러오기 표 구성 칸 폭
- 마이페이지 정보표 전폭, 상품 카드 이미지 대체 텍스트 비움(이름 중복 읽기 방지)

## 2. 샌드박스 실측 — PM 환원 대기

| 항목 | 실측 결과 |
|---|---|
| 팝업 방식 | `<dialog>` + `showModal()` 인페이지 모달. `window.open` 0건 (STATUS C2) |
| 버튼 | 텍스트 + CSS. 이미지 슬라이싱 0건 |
| doctype | HTML5 (STATUS C3) |
| `.prodcard.soldout` | 어느 JSP도 붙인 적 없음 — 탐색 화면 품절 표시는 처음부터 미사용 (STATUS C6) |
| 상품 이미지 | `/upload/*.png` 전부 404. 데이터·파일 불일치 (STATUS P5) |

## 3. 다른 트랙에 넘길 요청

| 대상 | 내용 | 상태 |
|---|---|---|
| JSP | 역할별 헤더 4종 · 상품상세 구매영역 3종 이식 — `mockup/_layout.html` · `mockup/product_view.html` 로 제시 | 전달함 |
| JSP | `common/paging.jsp` · `product/view.jsp` 주석이 `DECISIONS.md` "부록 B" 를 인용하는데 압축으로 사라졌다. `DECISIONS.md` §2 접두어 표로 참조를 바꿔 달라 | **미전달** |
| JSP | `common/error404.jsp` 가 공통 레이아웃 없이 맨몸이다. 다만 헤더가 DB 조회를 쓰므로 의도적일 수 있어 확인이 먼저다 | PM 경유 |
| ERD | 관리자 회원관리 역할 토글은 `admin/members.jsp:60-62` — JSP 소유다 | 반송함 |

## 4. 미결

- 컨셉 전달 대기. 토큰 **이름**은 확정, **값**만 비어 있다.
- 계약 미결(STATUS C5 · C6 · D4 · P5)은 `docs/STATUS.md` 를 따른다.
- ADMIN·DEV 구매 불가 안내에 `empty` 를 재사용 중이다. 전용 클래스 신설 여부는 C5 에 포함.
