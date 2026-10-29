# 트랙 2 — JSP 배선 (Wiring / Server)

> 소유 세션: **JSP 배선 세션** · 이 문서는 해당 세션이 직접 갱신한다.
> 세션 생성 직후 읽을 역할·권한 안내: `src/main/java/shop/CLAUDE.md` (작업 디렉터리에서 자동 로드)
> 최종 갱신: 2026-10-29 (JSP 배선 세션) · 상태: 🟡 샌드박스 1차 구현 완료 · 실행 검증 전
> ⚠️ 이 문서는 **샌드박스(`zyonge_Sandbox`) 사본**이다. 여기 적힌 산출물은 `main` 에 없다.
>    § 8 의 판단만 PM 을 거쳐 `main` 으로 환원한다 (CLAUDE.md § 4.5).

---

## 1. 소유 파일 — 이 트랙만 수정한다

| 경로 | 내용 |
|---|---|
| `WebContents/{member,product,cart,order,mypage,admin}/*.jsp` | 라우팅 페이지 JSP |
| `WebContents/common/*.jsp` | header / footer 등 공통 include |
| `WebContents/index.jsp` | 메인 진입 페이지 |
| `src/main/java/shop/controller/**` | Servlet |
| `src/main/java/shop/service/**` | 비즈니스 로직 |
| `src/main/java/shop/dao/**` | DB 접근 (SQL 실행) |
| `src/main/java/shop/dto/**` | 데이터 전달 객체 |
| `src/main/java/shop/util/**` | DB 연결, 공통 유틸 |
| `WebContents/WEB-INF/web.xml` | 배포 서술자 |
| `WebContents/META-INF/context.xml` | DB 커넥션 풀 (git 추적 제외) |
| `WebContents/js/validate.js` | 폼 검증 — 서버 검증과 **동일 규칙** 유지 |

## 2. 읽기만 하는 파일

- `WebContents/css/DECISIONS.md` — **UI 트랙의 확정 클래스명·토큰 변수명·팝업 패턴. 마크업 작성 전 반드시 읽는다.** 수정 금지
- `WebContents/css/**`, `WebContents/mockup/**` — 마크업·클래스명 가져오기용. **수정 금지** (UI 트랙 소유)
- `WebContents/js/ui.js` — **수정 금지** (UI 트랙 소유)
- `docs/ERD.md`, `sql/schema.sql` — DTO·DAO 설계 근거. **수정 금지**

## 3. 착수 조건

| 조건 | 상태 |
|---|---|
| 화면 목록 + URL 매핑 계약 | ⬜ 이 세션에서 기록 → PM 재개 |
| DTO 필드 계약 (ERD 트랙과 합의) | ⬜ 대기 |
| UI 목업 마크업 | ⬜ 대기 (없어도 Servlet·DAO 선행 가능) |

> **선행 가능한 작업**: `shop/util`의 DBCP 커넥션 유틸, `web.xml` Servlet 매핑 골격, `META-INF/context.xml` 구성.

## 4. 작업 규칙

- 라이브러리는 `WebContents/WEB-INF/lib/`에 **이미 확보된 6개만** 사용한다. 추가 반입 금지.
  - `mysql-connector-j-9.7.0.jar` · `commons-dbcp-1.4.jar` · `commons-pool-1.5.7.jar`
  - `taglibs-standard-impl-1.2.5.jar` · `taglibs-standard-spec-1.2.5.jar` · `cos.jar`
- 파일 업로드는 `cos.jar`, DB 커넥션 풀은 `commons-dbcp` 사용.
- 업로드 저장 경로는 `WebContents/upload/`, 웹 참조 경로는 `/upload/<파일명>` 으로 고정.
- Servlet 4.0 / JSP 2.3 범위를 벗어나는 문법 금지. (Tomcat 9.0.117)
- 외부 API·소셜 로그인·실결제 연동 금지. (공지 항목 10)
- 공통 JSP는 `WebContents/common/`에 두고 모든 페이지가 include한다.
- `common/header.jsp`의 레이아웃 마크업은 UI 트랙의 `mockup/_layout.html`을 이식해 쓴다. CSS를 직접 수정하지 않고, 필요하면 §7로 요청한다.
- DB 접속 정보는 `context.xml`에만 둔다. 소스에 하드코딩 금지, git 커밋 금지.

## 5. Done 기준

- [x] 화면 목록 + URL 매핑 기록 완료 (§ 9)
- [x] `shop/util` DB 커넥션 유틸 작성 (DBCP · JNDI `jdbc/shop`)
- [x] DTO / DAO / Service / Controller 4계층 배선 완료
- [x] 모든 라우팅 페이지 JSP 작성 (39개 · 번역 0 오류)
- [x] 팝업 7종이 페이지와 연동 (`<dialog>`)
- [~] 프론트 → 배선 → DB 왕복 **실행** 확인 — Tomcat 기동, 비로그인 화면 9개 200 확인. 로그인 이후·결제 흐름은 미확인

## 6. 진행 상황 로그

| 날짜 | 작업 | 산출물 |
|---|---|---|
| 2026-10-29 | 샌드박스 배선 1차 — 디자인 없이 기능 골격 전체 구현 | Java 82개 · JSP 39개 · JS 2개 (아래 집계) |

**산출물 집계**

| 계층 | 개수 | 경로 |
|---|---|---|
| DTO | 21 | `shop/dto/**` |
| DAO | 16 | `shop/dao/**` |
| Service | 10 | `shop/service/**` |
| Controller | 25 | `shop/controller/**` |
| Filter | 6 | `shop/filter/**` |
| Util | 4 | `shop/util/**` |
| JSP | 39 | `WebContents/**/*.jsp` |
| JS | 2 | `js/validate.js`(소유) · `js/ui.js`(임시본) |

**검증한 것 / 하지 않은 것**

| 항목 | 상태 |
|---|---|
| `javac` 전체 컴파일 | ✅ 0 오류 (82 클래스) |
| JSP 번역 · 컴파일 (Jasper `JspC`) | ✅ 39개 0 오류 |
| Tomcat 기동 후 실제 요청 | 🟡 비로그인 화면(메인·목록·상세·공지·로그인·가입) 200 + 실데이터 렌더링 확인 |
| 결제 트랜잭션 실제 롤백 동작 | ❌ 미실시 (코드 경로만 작성) |

**디자인에 대한 입장** — 이번 작업은 CSS 를 한 줄도 쓰지 않았다.
`WebContents/css/**` 는 손대지 않았고, 마크업에는 `css/DECISIONS.md` D-08 · D-09 의
클래스 이름만 계약대로 붙여 두었다. `header.jsp` 가 CSS 를 정해진 순서로 link 하므로,
UI/UX 세션이 값을 채우면 **JSP 수정 없이** 그대로 적용된다.

## 7. 다른 트랙에 넘길 요청

| 대상 | 요청 내용 | 상태 |
|---|---|---|
| UI/UX | `js/ui.js` 를 임시로 작성했다(팝업 open/close 2개). **UI 트랙 소유 파일이므로 가져가거나 교체해 달라.** 배선은 `window.openPopup(id)` / `closePopup(id)` 두 개만 쓴다 | ⬜ |
| UI/UX | 팝업을 `<dialog>` 로 구현했다(§ 8-7). `components/popup.css` 작성 시 `popup` · `popup_head` · `popup_body` · `popup_foot` · `popup_close` 를 `dialog` 요소 기준으로 잡아 달라 | ⬜ |
| UI/UX | 마크업에서 쓴 클래스 중 부록 B 에 없는 것: `page_header_util` · `page_header_notice` · `page_nav` · `page_search` · `admin_nav` · `my_nav` · `qna_answer` · `order_timeline` · `toast_error` · `formtable_form` · `prod_*` · `cart_sum` · `notice_*`. D-09 접두어 규칙에는 맞지만 목록에는 없다 — 추가 확정 필요 | ⬜ |
| ERD | `admin_log.action` 어휘에 **`REVIEW_DELETE` 가 없다.** CRUD.md § 3 의 35번(관리자 리뷰관리)은 `log` 에 **C** 를 요구하는데 쓸 action 이 없어 임의로 추가했다 | ⬜ |
| ERD | `ShopFilter` 통과 목록(§ 5)에 **`/member/login` 을 더해야 한다.** 현재 목록대로면 점검 모드를 켠 뒤 로그아웃한 관리자가 다시 들어올 수 없다 (§ 8-2) | ⬜ |
| ERD | DTO 필드 계약(§ 7)에 표시용 조인 필드 4개를 더했다 — `OrderDTO.loginId` · `memberName` · `itemCount` · `firstProductName`. `v_admin_order` 가 이미 주는 값이고 관리자 목록 제목("티셔츠 외 2건")에 필요하다 | ⬜ |
| PM | ~~`mermaid.min.js` 반입 요청~~ → **철회** (§ 8-15, ERD 는 이미지로 대체). 기존 문구:  없어서 `/dev/erd` 의 **탭 1(ERD 다이어그램)을 제외**하고 탭 2(실제 스키마 조회)만 구현했다 (ERD § 8 쟁점 8) | ⬜ |

## 8. 미결 / 질문 — 샌드박스에서 내린 판단

> CLAUDE.md § 4.5 — "실험에서 얻은 것은 코드가 아니라 **판단**이다."
> 아래는 `main` 으로 환원할 후보다. 코드는 넘기지 않는다.

### 8-1. 실행 검증을 하지 못했다
컴파일과 JSP 번역은 통과했으나 Tomcat 을 띄워 실제 요청을 흘려 보지 않았다.
특히 **결제 트랜잭션의 롤백 경로**(재고 부족 시 7개 테이블 원복)는 코드로만 존재한다.
가장 먼저 확인해야 할 항목이다.

### 8-2. `ShopFilter` 통과 목록이 관리자를 잠글 수 있다 ⚠️
ERD § 5 는 `/admin/*`, `/dev/*`, 정적 자원만 통과시키라고 적는다. 그런데 로그인은
`/member/login` 이다. **점검 모드를 켜고 로그아웃하면 관리자도 로그인 화면에 닿지 못한다.**
"ShopFilter 가 AdminFilter 보다 먼저" 라는 규칙이 막으려던 사고가, 로그인 경로를 빼먹어
다른 경로로 재현된다. 샌드박스에서는 `/member/login` · `/member/logout` 을 통과 목록에 넣었다.

### 8-3. JSP 직접 접근을 어떻게 막을 것인가
CLAUDE.md § 7 이 JSP 를 `WebContents/{member,product,...}/*.jsp` 에 두도록 고정했다.
그 경로는 웹 루트 아래라 **브라우저가 `/product/view.jsp` 를 직접 열 수 있고**,
그러면 Servlet 을 건너뛰어 모델 없이 렌더되거나 권한 검사를 지나친다.

| 안 | 내용 | 대가 |
|---|---|---|
| A (채택) | `JspDirectAccessFilter` 를 `<dispatcher>REQUEST</dispatcher>` 로만 매핑 | 필터 하나 추가. 디렉터리 구조 유지 |
| B | 뷰를 `/WEB-INF/views/` 로 이전 | 표준적이지만 § 7 확정 구조를 바꿔야 함 |

A 를 골랐다. `forward` 는 FORWARD 디스패치라 필터를 타지 않으므로 뷰는 정상 렌더되고,
브라우저가 직접 친 `.jsp` 만 걸러진다.

### 8-4. EL 예약어가 DTO 이름을 제약한다 ⚠️
`PageDTO.isEmpty()` 를 만들었더니 `${pageData.empty}` 가 **파싱 단계에서 실패**했다.
`empty` 는 EL 연산자이기 때문이다. 같은 이유로 요청 파라미터 이름 `new` 도 쓸 수 없다.

→ **DTO 필드 계약에 규칙을 추가할 것**: `empty` · `new` · `true` · `false` · `null` ·
`div` · `mod` · `and` · `or` · `not` · `instanceof` 는 프로퍼티명 · 파라미터명으로 쓰지 않는다.
이 오류는 컴파일에서 안 잡히고 **화면을 열어야 드러난다.**

### 8-5. 비밀번호를 평문으로 다뤘다 ⚠️
`data.sql` 의 더미 계정이 평문이고 `/dev` 계정 전환이 그 전제 위에 서 있어 그대로 두었다.
**제출물에서는 해시가 필요하다.** 해시를 넣으면 `/dev/loginAs` 는 비밀번호 검증을 건너뛰는
경로이므로 영향이 없고, 로그인과 비밀번호 변경 두 곳만 고치면 된다.

### 8-6. `error500.jsp` 가 예외를 화면에 노출한다
배선을 고치는 데 필요해서 그대로 띄웠다. **제출물에서는 지우고 로그로만 남겨야 한다.**

### 8-7. 팝업 — `<dialog>` 를 골랐다 (O-02 미결 상태에서)
"디자인 요소 없이" 라는 조건에서 인페이지 모달을 CSS 없이 구현할 방법이 `<dialog>` 뿐이었다.
표준 요소라 `showModal()` 만으로 모달 동작(배경 inert · ESC 닫기)이 나온다.

| O-02 결정 | 이 코드에 미치는 영향 |
|---|---|
| 인페이지 모달 | 그대로 쓰고 CSS 만 입히면 된다 |
| 별창 `window.open()` | 팝업 7종이 **독립 라우트**가 되어야 한다. URL 매핑에 행 7개 추가 |

### 8-8. doctype 은 HTML5 로 썼다 (O-03 미결 상태에서)
`<dialog>` 가 HTML5 요소라 실습 관례(HTML 4.01 Transitional)를 쓸 수 없었다.
O-03 이 4.01 로 확정되면 8-7 의 팝업 방식부터 다시 정해야 한다.

### 8-9. `order_no` 생성 — 임시값 기법
`order_no` 는 `order_id` 가 있어야 만들 수 있는데 `NOT NULL UNIQUE` 라 비워 둘 수 없다.
`CONCAT('T', LPAD(CONNECTION_ID(), 10, '0'))` 를 넣고 같은 트랜잭션에서 바로 덮어썼다.
커넥션당 하나뿐이고 커밋 전에 교체되므로 충돌하지 않는다. ERD § 2.8 (a) 안의 구체화다.

### 8-10. 고객 주문 목록에 N+1 이 있다
주문 10건마다 `delivery` 를 한 번씩 더 조회한다(왕복 11회). 관리자 목록은 `v_admin_order`
한 방이지만 고객 목록에는 대응 뷰가 없다. 샘플 규모에서는 그대로 두었고,
건수가 늘면 **고객용 주문 목록 뷰**를 요청한다.

### 8-11. 취소된 주문의 배송 상태는 그대로 남겨 두었다
ERD § 8 쟁점 1 을 설계대로 구현했다 — 취소해도 `delivery.status` 는 건드리지 않는다.
실제로 화면에 "주문상태: 취소 / 배송상태: 상품준비" 가 나란히 뜬다. **의도된 공백이
화면에서 어떻게 보이는지** 확인한 셈이므로, 쟁점 1 판단의 근거로 쓸 수 있다.

### 8-12. 서블릿 와일드카드 매핑이 JSP 를 무한 forward 시킨다 ⚠️ (실행해서 발견)
`/product/*` 로 매핑하자, 서블릿이 `forward("/product/list.jsp")` 한 JSP 를 **같은 서블릿이 다시 받아**
StackOverflow 가 났다. JSP 를 `WebContents/product/` 에 두기로 한 확정 구조(CLAUDE.md § 7)와
URL 네임스페이스(`/product/...`)가 **같은 접두어를 공유하기 때문**이다. 컴파일·JspC 에서는 안 잡힌다.
→ 와일드카드를 버리고 URL 을 하나씩 정확히 나열했다. `BaseServlet.action()` 은 `servletPath` 마지막 조각을 쓴다.
→ **main 으로 환원할 규칙**: 서블릿은 `/xxx/*` 로 매핑하지 않는다.

### 8-13. 컨텍스트를 외부 파일로 선언하면 META-INF/context.xml 이 무시된다
Eclipse 가 아닌 CLI 로 띄울 때 `conf/Catalina/localhost/*.xml` 로 컨텍스트를 선언하면 `<Resource jdbc/shop>` 이
바인딩되지 않아 `DBUtil` 초기화가 실패한다. Eclipse(WTP) 배포에서는 해당 없음.

### 8-14. 인코딩: include 조각은 `web.xml` 의 `jsp-property-group` 으로 고정해야 한다
`header.jsp` · `footer.jsp` 는 page 지시자가 없는 정적 include 조각이라 한글이 깨졌다.
`<jsp-config>` 로 `*.jsp` 전체를 UTF-8 로 고정했다. 인코딩 변경 후엔 JSP `work/` 캐시를 비워야 반영된다.

### 8-15. `/dev/erd` — ERD 다이어그램은 이미지로 직접 넣을 예정 (사용자 결정)
Mermaid 탭은 만들지 않고, 다이어그램은 사용자가 **이미지로 하드코딩**해 넣는다.
현재 `/dev/erd` 는 실제 스키마 조회(컬럼 · 제약 · CHECK)만 구현된 상태로 **그대로 유지**한다.
`mermaid.min.js` 반입 요청(§ 7)은 철회 대상이다. 이미지는 `images/**`(UI 트랙)에 두고 `erd.jsp` 상단에서 참조한다.

---

## 9. 화면 목록 + URL 매핑 (이 트랙의 기록)

> CLAUDE.md § 4.2 계약 1번용 기록. 확정은 PM 이 UI 트랙 기록과 대조해서 한다.
> Servlet 매핑은 `@WebServlet` 애노테이션으로 두고, `web.xml` 에는 **순서가 의미를 갖는
> 필터 체인만** 적었다.

### 9.1 고객

| CRUD# | 화면 | Method | URL | Servlet | View |
|---|---|---|---|---|---|
| 1 | 메인 | GET | `/` · `/main` | `MainServlet` | `/main.jsp` |
| 2 | 회원가입 | GET · POST | `/member/join` | `MemberServlet` | `/member/join.jsp` |
| — | 아이디 중복확인 | GET | `/member/idcheck` | `MemberServlet` | (text) |
| 3 | 로그인 | GET · POST | `/member/login` | `MemberServlet` | `/member/login.jsp` |
| — | 로그아웃 | GET | `/member/logout` | `MemberServlet` | (redirect) |
| 4 | 상품목록 | GET | `/product/list` | `ProductServlet` | `/product/list.jsp` |
| 5 | 상품상세 | GET | `/product/view` | `ProductServlet` | `/product/view.jsp` |
| 6 | 장바구니 | GET | `/cart/list` | `CartServlet` | `/cart/list.jsp` |
| 6 | 담기 · 수량변경 · 삭제 | POST | `/cart/add` · `/cart/update` · `/cart/delete` | `CartServlet` | (redirect) |
| — | 찜 토글 · 해제 | POST | `/wish/toggle` · `/wish/remove` | `WishServlet` | (redirect) |
| 7 | 주문서 | GET | `/order/form` | `OrderServlet` | `/order/form.jsp` |
| **8** | **결제처리** | POST | `/order/pay` | `OrderServlet` | (redirect) |
| 9 | 주문완료 | GET | `/order/complete` | `OrderServlet` | `/order/complete.jsp` |
| 10 | 주문내역 | GET | `/order/list` | `OrderServlet` | `/order/list.jsp` |
| 11 | 주문상세 · 배송조회 | GET | `/order/view` | `OrderServlet` | `/order/view.jsp` |
| 12 | 주문취소 | POST | `/order/cancel` | `OrderServlet` | (redirect) |
| 13 | 마이페이지 | GET | `/mypage/` | `MypageServlet` | `/mypage/index.jsp` |
| 14 | 정보수정 · 탈퇴 | GET · POST | `/mypage/edit` · `/mypage/password` · `/mypage/leave` | `MypageServlet` | `/mypage/edit.jsp` |
| 15 | 배송지관리 | GET · POST | `/mypage/address` | `MypageServlet` | `/mypage/address.jsp` |
| 16 | 찜목록 | GET | `/mypage/wishlist` | `MypageServlet` | `/mypage/wishlist.jsp` |
| 17 | 내 리뷰 | GET · POST | `/mypage/review` | `MypageServlet` | `/mypage/review.jsp` |
| 18 | 내 문의 | GET · POST | `/mypage/qna` | `MypageServlet` | `/mypage/qna.jsp` |
| 19 | 공지목록 | GET | `/notice/list` | `NoticeServlet` | `/notice/list.jsp` |
| 20 | 공지상세 | GET | `/notice/view` | `NoticeServlet` | `/notice/view.jsp` |
| 21 | 점검 안내 | GET | `/maintenance` | `MaintenanceServlet` | `/maintenance.jsp` |

### 9.2 관리자 · dev

| CRUD# | 화면 | Method | URL | Servlet |
|---|---|---|---|---|
| 22 | 대시보드 | GET | `/admin/dashboard` | `AdminDashboardServlet` |
| 23 | 상품관리 | GET · POST | `/admin/products` | `AdminProductServlet` |
| 24 | 카테고리관리 | GET · POST | `/admin/categories` | `AdminCategoryServlet` |
| 25 | 주문·배송관리 | GET · POST | `/admin/orders` | `AdminOrderServlet` |
| 26 | 회원관리 | GET · POST | `/admin/members` | `AdminMemberServlet` |
| 27 | 문의답변 | GET · POST | `/admin/qna` | `AdminQnaServlet` |
| 28 | 공지관리 | GET · POST | `/admin/notices` | `AdminNoticeServlet` |
| 35 | 리뷰관리 | GET · POST | `/admin/reviews` | `AdminReviewServlet` |
| 29 | dev 허브 | GET | `/dev/` | `DevHubServlet` |
| 30 | dev 상태 컨트롤러 | GET · POST | `/dev/state` | `DevStateServlet` |
| 31 | dev 데이터 브라우저 | GET | `/dev/data` | `DevDataServlet` |
| 32 | dev 설계 보기 | GET | `/dev/erd` | `DevErdServlet` |
| 33 | dev 데이터 관리 | GET · POST | `/dev/seed` | `DevSeedServlet` |
| 34 | dev 계정 전환 | GET · POST | `/dev/loginAs` | `DevLoginAsServlet` |
| 36 | dev 감사 로그 | GET | `/dev/log` | `DevLogServlet` |

### 9.3 팝업 7종 — 부모 화면에 포함 (별도 라우트 없음)

| 팝업 | 부모 화면 | 제출 대상 |
|---|---|---|
| 아이디 중복확인 | `/member/join` | `/member/idcheck` (화면 없음) |
| 상품 문의 작성 | `/product/view` | `/mypage/qna` |
| 리뷰 작성 | `/order/view` · `/mypage/review` | `/mypage/review` |
| 리뷰 수정 | `/mypage/review` | `/mypage/review` |
| 문의 수정 | `/mypage/qna` | `/mypage/qna` |
| 배송지 추가 · 수정 | `/mypage/address` | `/mypage/address` |
| 상품 등록 · 수정 | `/admin/products` | `/admin/products` |
| 운송장 입력 | `/admin/orders` | `/admin/orders` |
| 문의 답변 | `/admin/qna` | `/admin/qna` |
| 공지 등록 · 수정 | `/admin/notices` | `/admin/notices` |

> O-02 가 "별창" 으로 확정되면 위 팝업이 전부 독립 라우트가 된다 (§ 8-7).

### 9.4 필터 체인 (`web.xml` — 선언 순서 = 실행 순서)

| 순서 | 필터 | 대상 | 하는 일 |
|---|---|---|---|
| 1 | `EncodingFilter` | `/*` | UTF-8 |
| 2 | `JspDirectAccessFilter` | `/*` (REQUEST만) | `.jsp` 직접 접근 차단 (§ 8-3) |
| 3 | `ShopFilter` | `/*` | 점검 모드 — **AdminFilter 보다 먼저** |
| 4 | `LoginFilter` | `/cart/*` `/order/*` `/mypage/*` `/wish/*` | 로그인 필요 (신설 · § 7) |
| 5 | `AdminFilter` | `/admin/*` | `role IN ('ADMIN','DEV')` |
| 6 | `DevFilter` | `/dev/*` | `role = 'DEV'` (전환 중이면 `devOrigin` 으로 판정) |
