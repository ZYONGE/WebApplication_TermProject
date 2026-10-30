# JSP 배선 트랙 — 샌드박스

> 소유: JSP 배선 세션 · 상태: 🟢 전 화면 배선 완료 · 실행 검증 완료 · 최종 갱신 2026-10-30
> 이 문서는 `zyonge_Sandbox` 전용이다. 여기 산출물은 `main` 으로 가지 않는다.
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
| 검증 | javac 0 오류 · JspC 39 0 오류 · Tomcat 실행 (비로그인/USER/ADMIN/DEV × 주요 경로) |
| 디자인 | CSS 미작성. 마크업은 `css/DECISIONS.md` 클래스명만 사용 |

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
| 21 | 점검 안내 | `/maintenance` |

**관리자** (ADMIN 전용)

| # | 화면 | URL |
|---|---|---|
| 22 | 대시보드 | `/admin/dashboard` |
| 23 · 24 | 상품 · 카테고리 | `/admin/products` · `/admin/categories` |
| 25 | 주문·배송 | `/admin/orders` |
| 26 | 회원 (탈퇴 처리만) | `/admin/members` |
| 27 · 28 · 35 | 문의답변 · 공지 · 리뷰 | `/admin/qna` · `/admin/notices` · `/admin/reviews` |

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

ID 는 `docs/STATUS.md` PM 결정 대기 표를 따른다.

| ID | 내용 | 이 트랙의 현재 처리 |
|---|---|---|
| P4 | 비밀번호 해시 | 평문 비교 (로그인 · 비밀번호 변경 2곳만 바꾸면 됨) |
| P5 | 샘플 이미지 404 | `/upload/sample-*.png` 경로 그대로 출력 |
| C1 | URL 매핑 확정 | § 2 가 JSP 측 기록 |
| C2 · C3 | 팝업 방식 · doctype | `<dialog>` · HTML5. 별창이면 팝업이 라우트로 분리됨 |
| C4 | DTO 계약 | `OrderDTO` 조인 필드 4개(`loginId` · `memberName` · `itemCount` · `firstProductName`) 추가됨. EL 예약어 프로퍼티명 금지 |
| C5 | CSS 클래스 계약 보완 | 마크업 변경 대기 |
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
