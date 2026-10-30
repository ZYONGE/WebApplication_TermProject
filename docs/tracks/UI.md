# 트랙 — UI / 디자인 테마

> 소유 세션: **UI 트랙** · 이 문서는 해당 세션이 직접 갱신한다.
> 최종 갱신: 2026-10-30 · 상태: 🟡 청사진 완료 · 컨셉 대기

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
