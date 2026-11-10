# JSP 배선 트랙 — 샌드박스

> 소유: JSP 배선 세션 · 상태: 🟢 영양제 컨셉 배선 완료 · 샘플 데이터로 실행 검증 · 최종 갱신 2026-11-10
> 이 문서는 `sandbox` 전용이다. 여기 산출물은 `main` 으로 가지 않는다.
> 파일 소유 범위는 루트 `CLAUDE.md` 의 트랙 표를 따른다.

---

## 1. 현재 구현 상태

| 영역 | 상태 |
|---|---|
| 계층 | `shop/{dto,dao,service,controller,filter,util}` — 84 클래스 · JSP 39 · JS `validate.js` |
| 필터 체인 | Encoding → JspDirectAccess → Shop → User → Admin → Dev (`web.xml`) |
| 역할 | USER · ADMIN · DEV 완전 격리 (ERD § 5). 거부 시 각 역할 홈으로 |
| 트랜잭션 | 결제(7테이블) · 주문 취소 · 배송 전이 · 배송지 기본값 · 관리자/dev 조작 + `admin_log` |
| 계정 전환 | 세션 교체 + `impersonatorId`, 전환 중 로그에 `impersonated_by` 자동 부착 |
| 팝업 | `<dialog>` + `ui.js` 임시본 (`openPopup` · `closePopup`) |
| ERD 그림 (`/dev/erd`) | README 의 mermaid 블록을 서버가 SVG 로 그림 (`MermaidErd`). 페이지에는 폭 맞춤 SVG 를 직접 넣고, 누르면 원본 크기 `/dev/erd/diagram.svg`. README 를 못 읽는 배포(Eclipse)는 사본 `shop/util/erd.mmd` 사용 — README ERD 수정 시 함께 맞춘다 |
| 검증 | javac 0 오류 · JspC 39 0 오류 · Tomcat 실행 (비로그인/USER/ADMIN/DEV × 주요 경로) |
| 디자인 | CSS 미작성. 마크업은 `css/DECISIONS.md` 클래스명만 사용 |

## 1.1 영양제 컨셉 전환 (2026-11-10)

기준: `docs/CONCEPT.md` · `docs/IMPACT.md` § 4 · `docs/ERD.md` § 7.1 · § 8-9~15 · § 9 · `sql/schema.sql` (커밋 `e1d130b`).

| 계층 | 바뀐 것 |
|---|---|
| DTO | 신설 `TrackItemDTO` · `ConflictDTO` · `IntakeWarningDTO` · `ComboDTO` · `ComboItemDTO`. `ProductDTO.productType` · `trackItems` · `isTrack()` · `typeLabel`, `CartItemDTO` · `WishItemDTO.productType`, `OrderItemDTO.trackSnapshot` |
| DAO | 신설 `TrackItemDAO` · `ConflictDAO` · `ComboDAO`. `ProductDAO` 구분 필터 · 조합 탭 조회 · `findAnyByIds` · `countOrderItems`. `CartDAO` 담기 = 일수 **덮어쓰기**(K4), 배지 `countLines`(줄 수). `OrderDAO` `track_snapshot`. `DevDAO` 화이트리스트 21 + 5, TRUNCATE 18. `AdminLogDAO` `TRACK_ITEM_SET` · `CONFLICT_*` |
| Service | 신설 `IntakeService`(트랙 펼치기 → 금지 조합 · 중복 섭취) · `ComboService`(저장 = 한 트랜잭션, 같은 이름은 확인 후 덮어쓰기). `CartService` 일수 상한 `MAX_INTAKE_DAYS` · `addAll`(한 트랜잭션, 담을 수 없는 줄은 건너뜀). `OrderService.pay` 트랙 줄 구성 스냅샷 · 트랙은 자기 재고만(K5). `AdminService` 구분 ↔ 대분류 검사(§ 8-12) · 소분류만 등록 · 구분 변경 금지(§ 8-15) · 트랙 구성 저장(§ 8-9 · 8-14) · 금지 조합 CRUD(§ 8-10 · 8-14) |
| Servlet | 신설 `CustomServlet` `/custom` · `ComboServlet` `/mypage/combo` (`/save` · `/delete`) · `AdminConflictServlet` `/admin/conflicts`. `CartServlet` `/cart/addAll` + 경고 · 일수 다른 줄 안내. `OrderServlet` 주문서 경고. `ProductServlet` 구분 필터 · 금지 조합 · 빠른 일수. `MainServlet` 트랙 영역. `web.xml` 수정 없음 (전부 기존 필터 경로) |
| JSP | 신설 `custom/index.jsp` · `mypage/combo.jsp` · `admin/conflicts.jsp` · `common/warnbox.jsp`(장바구니 · 주문서 공용 include). 수정: header(조합 맞추기 · 내 조합 · 탭/경고 CSS), footer(고지), main, product list/view, cart, order form/complete/view, admin products/orders/dashboard, mypage 내비 · 찜, dev index/seed/state |
| JS | 신설 `js/custom.js` (K11 — JSP 소유): 섭취 일수 빠른 선택 · 조합 탭 즉시 합계/배송비/경고/공유 링크. `validate.js` 에 `intakeDays` 규칙 |
| `/dev/erd` | `shop/util/erd.mmd` 를 `docs/ERD.md` 의 mermaid(21 엔티티)로 교체 |

K 결정 반영: K3 쿼리 상태(`/custom?pick=&days=`) + `combo` 저장 둘 다 · K4 덮어쓰기 · K5 트랙 자기 재고 · K6 스냅샷 · K7 관리자 화면 · K8 탭당 여러 개 · K10 `system_config` · K11 JSP 소유. **K9 사이트명은 미정이라 `SHOP` 그대로.**

검증 (2026-11-10, 새 샘플 데이터 적용 후 — 낱개 38 · 트랙 11 · 구성 75 · 금지 조합 5 · 내 조합 3):
- javac 0 오류 · JspC 43 0 오류 · `node --check` JS 3개
- Tomcat 9 실행: 비로그인 · USER(hong · kim) · ADMIN · DEV 주요 경로 전부 200. 조합 탭 즉시 계산 · URL 상태 · 탭 전환 · 경고 표시/해제 · 남의 조합 차단 · 재고 0 낱개 제외 안내 · 장바구니 트랙 펼침 경고(중복 · 금지) · 주문상세 트랙 스냅샷 · 관리자 트랙 구성 체크 확인
- **쓰기 흐름(담기 · 조합 저장 · 결제 · 금지 조합 등록 · 트랙 저장)은 실행하지 않았다** — 테스트 데이터를 바꾸지 않으려고

데이터 점검 후 고친 하드코딩
- 대분류 이름 `'낱개 영양제'` · `'트랙 패키지'` 를 코드에서 뺐다. 조합 탭 분류 탭은 판매 중 낱개가 든 카테고리를 트리 순서로, 상품 등록 검사(§ 8-12)는 "같은 대분류에 다른 구분 상품이 있는가" 로 판정 (`ProductDAO.countUnderRoot`)
- 섭취 일수 처음 값 30 → `ConfigService.DEFAULT_DAYS` 한 곳 (상품상세 JSP 의 숫자 제거)
- `/dev` 의 "21 테이블 + 뷰 5개" · "업무 테이블 18개" → `DevDAO` 에서 계산
- 메인 '신상품' 이 트랙 영역과 같은 카드로 겹쳐 → 낱개만 '새로 들어온 영양제'
- 장바구니 안내의 "30일분" 예시 → 상한 `MAX_INTAKE_DAYS` 표시, 금지 조합 입력 예시 문구 일반화, footer 의 "디자인 미적용" 문구, 주석의 의류 예시
- `web.xml` 에 JSTL fmt 로케일 `ko_KR` 고정 — Accept-Language 가 없으면 금액 쉼표 · 날짜 형식이 빠지고 Timestamp 원문이 찍혔다

그대로 둔 것: 사이트명 `SHOP` (K9 미정) · 로그인 화면 테스트 계정 안내 (M2) · 빠른 선택 7/14/30/60/90 (CONCEPT § 5 명세값)

## 2. 화면 목록 · URL 매핑

서블릿은 `@WebServlet` 정확 일치 매핑(와일드카드 금지). 뷰는 `/{영역}/{화면}.jsp`.

**고객** — 공개: 1~5 · 19~21 / USER 전용: 6~18

| # | 화면 | URL |
|---|---|---|
| 1 | 메인 | `/` · `/main` |
| 2 · 3 | 회원가입 · 로그인 · 로그아웃 | `/member/join` · `/member/login` · `/member/logout` · `/member/idcheck` |
| 4 · 5 | 상품목록 · 상세 | `/product/list` · `/product/view` |
| 6 | 장바구니 | `/cart/list` · `/cart/add` · `/cart/update` · `/cart/delete` |
| — | 찜 토글 · 해제 | `/wish/toggle` · `/wish/remove` |
| 7~12 | 주문서 · 결제 · 완료 · 내역 · 상세 · 취소 | `/order/form` · `/order/pay` · `/order/complete` · `/order/list` · `/order/view` · `/order/cancel` |
| 13~18 | 마이페이지 · 정보수정 · 배송지 · 찜 · 리뷰 · 문의 | `/mypage/` · `/mypage/edit` (+`password` · `leave`) · `/mypage/address` · `/mypage/wishlist` · `/mypage/review` · `/mypage/qna` |
| 19 · 20 | 공지목록 · 상세 | `/notice/list` · `/notice/view` |
| 37 | 조합 탭 (공개 열람) | `/custom` (`?pick=1,4,7&days=30` · `?track=13` · `?combo=2`) |
| 38 | 조합 담기 (USER) | `/cart/addAll` |
| 40 · 41 | 조합 저장 · 내 조합 (USER) | `/mypage/combo/save` · `/mypage/combo` · `/mypage/combo/delete` |
| 21 | 점검 안내 | `/maintenance` |

**관리자** (ADMIN 전용)

| # | 화면 | URL |
|---|---|---|
| 22 | 대시보드 | `/admin/dashboard` |
| 23 · 24 | 상품 · 카테고리 | `/admin/products` · `/admin/categories` |
| 25 | 주문·배송 | `/admin/orders` |
| 26 | 회원 (탈퇴 처리만) | `/admin/members` |
| 27 · 28 · 35 | 문의답변 · 공지 · 리뷰 | `/admin/qna` · `/admin/notices` · `/admin/reviews` |
| 39 | 금지 조합 관리 | `/admin/conflicts` |

**dev** (DEV 전용, 복귀 경로만 예외)

| # | 화면 | URL |
|---|---|---|
| 29 · 30 | 허브 · 상태 컨트롤러 | `/dev/` · `/dev/state` |
| 31 · 32 · 33 | 데이터 브라우저 · 설계 보기 · 데이터 관리 | `/dev/data` · `/dev/erd` · `/dev/seed` |
| 34 | 계정 전환 · 복귀 | `/dev/loginAs` · `/dev/loginAs/return` |
| 36 | 감사 로그 | `/dev/log` |

**팝업** — 부모 화면에 포함, 별도 라우트 없음

| 팝업 | 부모 → 제출 |
|---|---|
| 상품 문의 작성 | `/product/view` → `/mypage/qna` |
| 리뷰 작성 · 수정 | `/order/view` · `/mypage/review` → `/mypage/review` |
| 문의 수정 · 배송지 추가/수정 | `/mypage/qna` · `/mypage/address` → 같은 경로 |
| 상품 등록/수정 · 운송장 입력 · 문의 답변 · 공지 등록/수정 | `/admin/products` · `/admin/orders` · `/admin/qna` · `/admin/notices` → 같은 경로 |

## 3. 열린 요청 · 미결

**PM 에 보내는 요청 (2026-11-10)**

| 대상 | 내용 |
|---|---|
| PM | `README.md` 의 ERD mermaid 가 아직 17 엔티티다. `/dev/erd` 는 README 를 먼저 읽으므로 사본 `erd.mmd`(21 엔티티)와 어긋난다 — README 를 `docs/ERD.md` § 2 와 맞춰 달라 |
| PM | K9 사이트명 결정 시 `common/header.jsp` · `MainServlet` 의 `SHOP` 교체 |
| PM · DB | 로컬 DB 재적용 승인(P2 · K2) 후 실행 검증 — 비로그인/USER/ADMIN/DEV × `/custom` · `/cart/addAll` · `/mypage/combo` · `/admin/conflicts` · 상품등록(트랙) |

ID 는 `docs/STATUS.md` PM 결정 대기 표를 따른다.

| ID | 내용 | 이 트랙의 현재 처리 |
|---|---|---|
| P4 | 비밀번호 해시 | 평문 비교 (로그인 · 비밀번호 변경 2곳만 바꾸면 됨) |
| P5 | 샘플 이미지 404 | `/upload/sample-*.png` 경로 그대로 출력 |
| C1 | URL 매핑 확정 | § 2 가 JSP 측 기록 |
| C2 · C3 | 팝업 방식 · doctype | `<dialog>` · HTML5. 별창이면 팝업이 라우트로 분리됨 |
| C4 | DTO 계약 | `OrderDTO` 조인 필드 4개(`loginId` · `memberName` · `itemCount` · `firstProductName`) 추가됨. EL 예약어 프로퍼티명 금지 |
| C5 | CSS 클래스 계약 보완 | 마크업 변경 대기. 신규 `dev_erd` (ERD 감싸개 — 그림은 SVG `width=100%` 로 이미 페이지 폭에 맞춘다. 스타일 지정은 불필요) |
| C7 | `pageCss` 단일화 | 서블릿 · JSP 양쪽 설정 중 (JSP 값 우선) |
| D1 | 리프 카테고리만 등록 | 미적용 (대분류 선택 가능) |
| D2 | 고객 주문 목록 N+1 | 주문마다 `delivery` 조회 |
| D5 | JSP 직접 접근 차단 | `JspDirectAccessFilter` (REQUEST 디스패치 한정) |
| M2 | 제출 전 제거 | `error500.jsp` 예외 노출 · 로그인 화면 테스트 계정 안내 남아 있음 |
| M3 | CLI 기동 시 `context.xml` 무시 | Eclipse 배포는 해당 없음 |

**전달받은 요청 — 미반영**

| 출처 | 내용 |
|---|---|
| DB | 취소 주문의 배송 상태를 `order_status` 먼저 보고 렌더 — 주문내역 · 관리자 목록이 아직 `delivery.status` 를 그대로 표시 |
| DB | 판매 집계 6일 범위 — 대시보드는 현재 최근 7행(`findSalesDaily(7)`) |

**전달받은 요청 — 반영 완료** (`docs/STATUS.md` 전달 대기 표에서 정리 가능)

- `admin_log.action` `REVIEW_DELETE` · `CONFIG_UPDATE` — 설정 변경은 이미 `CONFIG_UPDATE` 로 기록됨
- 회원관리 역할 토글 제거 · 역할별 헤더
- qna 답변 · 취소 · 환불 · 배송중 전환의 관련 컬럼 동시 UPDATE
