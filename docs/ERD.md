# ERD & 테이블 명세

> 최종 갱신: 2026-10-29 · 대상 DB: **MySQL 9.6.0 / InnoDB / utf8mb4**
> 짝 문서: `docs/CRUD.md` (화면 × 테이블 매트릭스)

---

## 0. 원본 사양과 이 문서의 차이

입력 사양은 **Oracle 기준**으로 작성되어 있었다. 이 프로젝트의 확정 환경은 MySQL 9.6.0 이고
(`README.md` Prerequisites = 단일 기준, `CLAUDE.md` § 1), 샌드박스 배너도 § 1 스펙은 그대로
유효하다고 규정한다. 따라서 **설계 의도는 보존하고 타입·함수만 MySQL로 치환**했다.

| Oracle | MySQL | 비고 |
|---|---|---|
| `NUMBER` (대리키) | `INT UNSIGNED AUTO_INCREMENT` | 시퀀스(`member_seq`) 불필요 — MySQL에 시퀀스 객체가 없다 |
| `NUMBER(10)` (금액) | `INT UNSIGNED` | 0 ~ 42.9억. 원화 정수 금액에 충분하고 `CHECK (price >= 0)` 을 타입으로 대체 |
| `NUMBER(6)` (배송비) | `INT UNSIGNED` | 금액 컬럼은 전부 같은 타입으로 통일 — DAO 쪽 분기 제거 |
| `NUMBER(1)` (별점) | `TINYINT UNSIGNED` | `CHECK (rating BETWEEN 1 AND 5)` 유지 |
| `VARCHAR2(n)` | `VARCHAR(n)` | utf8mb4 기준이므로 n 은 **문자 수** |
| `CLOB` | `TEXT` | 최대 64KB. 상품 상세·공지 본문에 충분 |
| `CHAR(1)` + `CHECK IN ('Y','N')` | `ENUM('Y','N')` | § 0.1 참조 |
| `VARCHAR2` + `CHECK IN (...)` | `ENUM(...)` | § 0.1 참조 |
| **`DATE`** | **`DATETIME`** | **§ 0.2 — 가장 위험한 변환** |
| `DEFAULT SYSDATE` | `DEFAULT CURRENT_TIMESTAMP` | |
| `USER_TAB_COLUMNS` | `INFORMATION_SCHEMA.COLUMNS` | `dev/erd.jsp` 탭 2 |
| `USER_CONSTRAINTS` | `INFORMATION_SCHEMA.TABLE_CONSTRAINTS` + `KEY_COLUMN_USAGE` | 같음 |
| (트리거 필요) | `ON UPDATE CURRENT_TIMESTAMP` | `system_config.updated_at` 이 공짜로 갱신된다 |

### 0.1 고정 어휘는 `ENUM` 으로

`CHECK IN (...)` 을 그대로 옮기는 대신 `ENUM` 을 썼다. MySQL 8.0.16+ 는 `CHECK` 도 실제로
강제하므로 둘 다 가능하지만, `ENUM` 을 고른 이유는 세 가지다.

1. DDL 자체가 명세가 된다 — 테이블을 보면 허용 값이 바로 보인다.
2. **선언 순서가 정렬 순서다.** `ENUM('결제완료','상품준비','배송중','배송완료')` 는
   `ORDER BY status` 가 배송 생애주기 순으로 정렬된다. 관리자 주문 목록에 그대로 쓸 수 있다.
3. 상태 전이 가드(`WHERE status = '직전상태'`)는 문자열 비교라 그대로 동작한다.

⚠️ 대가: 값을 추가하려면 `ALTER TABLE` 이 필요하고, 정렬이 사전순이 아니라 **선언순**이라는 점을
모르면 혼란이 생긴다. 사전순이 필요하면 `ORDER BY CAST(status AS CHAR)` 로 명시한다.

### 0.2 `DATE` → `DATETIME` — 이 변환을 놓치면 실험이 깨진다

Oracle `DATE` 는 **날짜 + 시각**을 담는다. MySQL `DATE` 는 **날짜만** 담는다.
글자가 같아서 그대로 옮기기 쉬운데, 그러면 시각이 전부 00:00:00 으로 잘린다.

깨지는 곳:
- `delivery_history.changed_at` — 같은 날 일어난 상태 변경의 순서가 사라져 배송 타임라인이 뒤섞인다.
- `orders.ordered_at` — 당일 주문 정렬 불가.
- `delivery.shipped_at` / `delivered_at` — 당일 발송·도착 구분 불가.

→ **시각이 필요한 모든 컬럼은 `DATETIME`.** 이 문서에 `DATE` 타입 컬럼은 하나도 없다.

---

## 1. ERD

```mermaid
erDiagram
  MEMBER ||--o{ ADDRESS : "배송지"
  MEMBER ||--o{ CART : "담기"
  MEMBER ||--o{ WISHLIST : "찜"
  MEMBER ||--o{ ORDERS : "주문"
  MEMBER ||--o{ REVIEW : "작성"
  MEMBER ||--o{ QNA : "문의"
  MEMBER ||--o{ DELIVERY_HISTORY : "상태변경"
  MEMBER ||--o{ ADMIN_LOG : "조작"
  CATEGORY ||--o{ CATEGORY : "하위"
  CATEGORY ||--o{ PRODUCT : "분류"
  PRODUCT ||--o{ PRODUCT_IMAGE : "이미지"
  PRODUCT ||--o{ CART : "담김"
  PRODUCT ||--o{ WISHLIST : "찜됨"
  PRODUCT ||--o{ ORDER_ITEM : "판매"
  PRODUCT ||--o{ REVIEW : "평가"
  PRODUCT ||--o{ QNA : "질문"
  ORDERS ||--|{ ORDER_ITEM : "포함"
  ORDERS ||--|| PAYMENT : "결제"
  ORDERS ||--|| DELIVERY : "배송"
  DELIVERY ||--|{ DELIVERY_HISTORY : "이력"
  ORDER_ITEM ||--o| REVIEW : "구매후기"

  MEMBER {
    int member_id PK
    varchar login_id UK
    varchar password
    varchar name
    varchar email UK
    varchar phone
    enum role "USER/ADMIN/DEV"
    enum status "ACTIVE/LEFT"
    datetime created_at
  }
  ADDRESS {
    int addr_id PK
    int member_id FK
    varchar addr_name
    varchar receiver
    varchar receiver_phone
    varchar zipcode
    varchar addr1
    varchar addr2
    enum is_default "Y/N"
  }
  CATEGORY {
    int category_id PK
    int parent_id FK
    varchar name
    int sort_order
  }
  PRODUCT {
    int product_id PK
    int category_id FK
    varchar name
    int price
    int stock
    text description
    varchar thumbnail
    enum status "판매중/숨김"
    datetime created_at
  }
  PRODUCT_IMAGE {
    int image_id PK
    int product_id FK
    varchar img_path
    int sort_order
  }
  CART {
    int cart_id PK
    int member_id FK
    int product_id FK
    int quantity
    datetime added_at
  }
  WISHLIST {
    int member_id PK,FK
    int product_id PK,FK
    datetime created_at
  }
  ORDERS {
    int order_id PK
    varchar order_no UK
    int member_id FK
    int total_price
    int delivery_fee
    enum order_status "정상/취소"
    varchar receiver
    varchar receiver_phone
    varchar zipcode
    varchar addr1
    varchar addr2
    varchar delivery_memo
    datetime ordered_at
    datetime canceled_at
  }
  ORDER_ITEM {
    int item_id PK
    int order_id FK
    int product_id FK
    varchar product_name
    int price
    int quantity
  }
  PAYMENT {
    int payment_id PK
    int order_id FK,UK
    enum method "카드/무통장/간편결제"
    int amount
    enum pay_status "완료/환불"
    datetime paid_at
    datetime refunded_at
  }
  DELIVERY {
    int delivery_id PK
    int order_id FK,UK
    enum status "결제완료/상품준비/배송중/배송완료"
    varchar courier
    varchar tracking_no
    datetime paid_at
    datetime ready_at
    datetime shipped_at
    datetime delivered_at
  }
  DELIVERY_HISTORY {
    int history_id PK
    int delivery_id FK
    enum prev_status
    enum new_status
    int changed_by FK
    enum is_forced "Y/N"
    datetime changed_at
  }
  REVIEW {
    int review_id PK
    int member_id FK
    int product_id FK
    int item_id FK,UK
    tinyint rating
    varchar content
    datetime created_at
  }
  QNA {
    int qna_id PK
    int member_id FK
    int product_id FK
    varchar question
    enum is_secret "Y/N"
    varchar answer
    datetime created_at
    datetime answered_at
  }
  NOTICE {
    int notice_id PK
    varchar title
    text content
    int view_count
    datetime created_at
  }
  SYSTEM_CONFIG {
    varchar config_key PK
    varchar config_value
    varchar description
    datetime updated_at
  }
  ADMIN_LOG {
    int log_id PK
    int member_id FK
    enum mode "ADMIN/DEV"
    varchar action
    varchar target_table
    int target_id
    varchar detail
    datetime created_at
  }
```

### 설계 포인트

- **회원 배송지(`address`)**: 회원별 주소록 1:N. `is_default = 'Y'` 인 주소를 주문서에 자동 입력한다.
- **주문 배송지(`orders`)**: 주소를 FK가 아닌 **복사본**으로 저장한다. 주소록을 수정·삭제해도
  과거 주문 내역이 바뀌지 않는다.
- **주문 시점 가격(`order_item`)**: 상품명·단가를 복사해 둔다. 상품 정보가 바뀌어도 주문 내역은 그대로다.
- **배송(`delivery`)**: 주문과 1:1. 상태는 `결제완료 → 상품준비 → 배송중 → 배송완료` 로만 전이한다.
  단 `/dev` 는 이 규칙을 무시할 수 있다 (§ 6.4).
- **배송 이력(`delivery_history`)**: 상태가 바뀔 때마다 한 줄씩. 마이페이지 배송 타임라인의 유일한 근거다.
  `is_forced = 'Y'` 면 `/dev` 가 강제로 바꾼 줄이다.
- **카테고리(`category`)**: `parent_id` 자기 참조로 대분류·소분류를 한 테이블에서 처리한다.
- **장바구니(`cart`)**: `UNIQUE (member_id, product_id)`. MySQL 에서는 Oracle `MERGE` 대신
  `INSERT ... ON DUPLICATE KEY UPDATE quantity = quantity + VALUES(quantity)` 한 문장으로 끝난다.
- **전역 설정(`system_config`)**: 배송비·무료배송 기준·점검 모드를 DB에 둔다. 코드 상수로 두지 않는
  이유는 `/dev/state.jsp` 에서 실시간으로 바꿔 시연해야 하기 때문이다.
- **조작 로그(`admin_log`)**: `/admin` · `/dev` 의 모든 쓰기 동작을 남긴다. 업무 이력(`delivery_history`)과
  **목적이 다르다** — 저쪽은 "배송이 어떻게 흘렀나", 이쪽은 "누가 무엇을 건드렸나".

---

## 2. 테이블 명세

| 항목 | 값 |
|---|---|
| 스키마 | **`webapplication_sandbox`** (utf8mb4 / utf8mb4_unicode_ci) |
| 테이블 공통 | `ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci` |
| PK | 모두 `INT UNSIGNED NOT NULL AUTO_INCREMENT` (`system_config` 제외 — 자연키) |
| 테이블명 | **접두사 없음.** 전용 스키마라 수업 실습 DB(`web`)와 스키마 단위로 분리된다 |

> 스키마명·계정은 DB Schema 트랙이 확정했다 (`docs/tracks/SCHEMA.md` § 9).
> 접속 설정은 `WebContents/META-INF/context.xml` 에 있고 git 추적에서 제외된다.

### 2.1 `member` — 회원

| 컬럼 | 타입 | 제약 | 설명 |
|---|---|---|---|
| member_id | INT UNSIGNED | PK, AUTO_INCREMENT | 회원 번호 |
| login_id | VARCHAR(30) | NOT NULL, UNIQUE | 로그인 아이디 |
| password | VARCHAR(100) | NOT NULL | 비밀번호 (해시 저장) |
| name | VARCHAR(30) | NOT NULL | 이름 |
| email | VARCHAR(100) | **NOT NULL**, UNIQUE | 이메일 |
| phone | VARCHAR(20) | NOT NULL | 연락처 |
| role | ENUM('USER','ADMIN','DEV') | NOT NULL DEFAULT 'USER' | 권한 (§ 5) |
| status | ENUM('ACTIVE','LEFT') | NOT NULL DEFAULT 'ACTIVE' | 탈퇴 여부 (소프트 삭제) |
| created_at | DATETIME | NOT NULL DEFAULT CURRENT_TIMESTAMP | 가입일시 |

> `email` 에 **NOT NULL 을 추가**했다. 원본은 UNIQUE 만 걸려 있었는데, MySQL 의 UNIQUE 는
> NULL 을 중복으로 보지 않는다. 이메일 없는 회원이 여러 명 생길 수 있어 UNIQUE 가 무의미해진다.
>
> `role` 에 `'DEV'` 를 추가했다 (§ 7.0 요구). **`/admin` 화면은 `USER ↔ ADMIN` 만 토글할 수 있고,
> 서버에서도 `/admin` 경로의 `role = 'DEV'` 쓰기를 거부해야 한다.** DEV 는 더미 데이터
> 스크립트(`sql/data.sql`)로만 만든다.

### 2.2 `address` — 회원 배송지

| 컬럼 | 타입 | 제약 | 설명 |
|---|---|---|---|
| addr_id | INT UNSIGNED | PK, AUTO_INCREMENT | 배송지 번호 |
| member_id | INT UNSIGNED | NOT NULL, FK → member ON DELETE CASCADE | 소유 회원 |
| addr_name | VARCHAR(30) | NULL | 별칭 (집, 회사, 기숙사) |
| receiver | VARCHAR(30) | NOT NULL | 받는 사람 |
| receiver_phone | VARCHAR(20) | NOT NULL | 받는 사람 연락처 |
| zipcode | VARCHAR(10) | NOT NULL | 우편번호 |
| addr1 | VARCHAR(200) | NOT NULL | 기본 주소 |
| addr2 | VARCHAR(200) | NULL | 상세 주소 |
| is_default | ENUM('Y','N') | NOT NULL DEFAULT 'N' | 기본 배송지 여부 |

> 기본 배송지 변경은 **한 트랜잭션**에서 두 문장으로 처리한다.
> `UPDATE address SET is_default='N' WHERE member_id=?` → `UPDATE address SET is_default='Y' WHERE addr_id=? AND member_id=?`
> 두 번째 문장에 `member_id` 조건을 반드시 붙인다. 없으면 남의 주소를 기본값으로 만들 수 있다.

### 2.3 `category` — 카테고리

| 컬럼 | 타입 | 제약 | 설명 |
|---|---|---|---|
| category_id | INT UNSIGNED | PK, AUTO_INCREMENT | 카테고리 번호 |
| parent_id | INT UNSIGNED | NULL, FK → category ON DELETE RESTRICT | NULL 이면 대분류 |
| name | VARCHAR(50) | NOT NULL | 카테고리명 |
| sort_order | INT | NOT NULL DEFAULT 0 | 메뉴 표시 순서 |

### 2.4 `product` — 상품

| 컬럼 | 타입 | 제약 | 설명 |
|---|---|---|---|
| product_id | INT UNSIGNED | PK, AUTO_INCREMENT | 상품 번호 |
| category_id | INT UNSIGNED | NOT NULL, FK → category ON DELETE RESTRICT | 소분류 |
| name | VARCHAR(100) | NOT NULL | 상품명 |
| price | INT UNSIGNED | NOT NULL | 판매가 |
| stock | INT UNSIGNED | NOT NULL DEFAULT 0 | 재고 |
| description | TEXT | NULL | 상세 설명 |
| thumbnail | VARCHAR(200) | NULL | 대표 이미지 — `/upload/<파일명>` |
| status | ENUM('판매중','숨김') | NOT NULL DEFAULT '판매중' | 전시 상태 — **관리자 의도만** 담는다 (§ 8 쟁점 2) |
| created_at | DATETIME | NOT NULL DEFAULT CURRENT_TIMESTAMP | 등록일시 |

> `UNSIGNED` 가 `CHECK (price >= 0)` · `CHECK (stock >= 0)` 을 대체한다. 부수 효과가 하나 있다 —
> 재고 차감에서 `WHERE stock >= ?` 가드를 빼먹으면 **조용히 음수가 되는 대신 에러가 난다.**
> 버그가 데이터로 남지 않고 즉시 드러나므로 이쪽이 낫다.

#### 판매 가능 여부는 `status` 와 `stock` 이 **각각 다른 것**을 말한다

| 컬럼 | 담는 것 | 쓰는 주체 |
|---|---|---|
| `status` | **관리자의 전시 의도** — `판매중` / `숨김` | 관리자만 (23번 `/admin/products`) |
| `stock` | **실제 재고 수량** | 시스템 (결제·취소) · 관리자 · `/dev` |

ENUM 에 `'품절'` 을 두지 않는다. 재고 상태를 `status` 에 복사하면 두 컬럼이 같은 사실을 말하게 되고,
동기화를 빠뜨린 코드 한 곳이 `status='품절'` + `stock=10` 같은 모순 행을 만든다
(§ 8 쟁점 2 · § 10.2). **`status` 는 재고를 모른다.**

#### 노출 규칙 — 재고가 없는 상품은 사용자에게 보이지 않는다

품절 배지를 띄우는 대신 **탐색 경로에서 제외한다.**

```sql
-- 상품 탐색 경로의 공통 조건
WHERE status = '판매중' AND stock > 0
```

| 경로 | 적용 |
|---|---|
| 1번 메인 · 4번 상품목록 · 검색 | 위 조건으로 **목록에서 제외** |
| 5번 상품상세 | 직접 URL 접근도 **차단.** 조건에 안 맞으면 목록으로 되돌린다 |
| 6번 장바구니 · 16번 찜목록 | **보여준다.** 사용자가 담아 둔 자기 기록이다. 대신 "재고 없음"으로 표시하고 주문 대상에서 뺀다 |
| 10·11번 주문내역 · 주문상세 | 영향 없음. `order_item` 이 주문 시점 스냅샷을 갖는다 (§ 2.9) |
| 17·18번 내 리뷰 · 내 문의 | 보여준다. 자기 기록이다 |

#### `숨김` 과 `stock = 0` 은 둘 다 숨지만 **성질이 다르다**

| | `status = '숨김'` | `stock = 0` |
|---|---|---|
| 누가 만든 상태인가 | **관리자의 의도** | 판매의 결과 |
| 재고가 들어오면 | **자동 복귀하지 않는다.** 관리자가 `판매중` 으로 되돌려야 한다 | **자동으로 다시 노출된다.** 별도 조작이 필요 없다 |
| 관리자 화면 표기 | "숨김" | "재고 없음" |

> 둘을 합쳐 하나의 플래그로 만들면 안 된다. 재입고 시 **복귀 여부가 반대**이기 때문이다.
> 숨겨 둔 상품이 입고만으로 다시 노출되면 관리자의 결정이 뒤집힌다.

> **경계가 핵심이다.** 차단은 *상품을 찾아가는 경로*(목록·검색·상세)에만 적용한다.
> 장바구니·찜·주문내역은 **사용자 자신의 기록**이므로 막으면 안 된다. 담아 둔 물건이
> 설명 없이 사라지는 것이 품절 표시보다 나쁘다.
>
> **대가**: 재고가 0이 되면 상품이 조용히 사라진다. 관리자가 알아차릴 경로가 필요하므로
> 22번 대시보드의 `v_low_stock`(§ 3.5)이 **장식이 아니라 필수 장치**가 된다.

### 2.5 `product_image` — 상품 이미지

| 컬럼 | 타입 | 제약 | 설명 |
|---|---|---|---|
| image_id | INT UNSIGNED | PK, AUTO_INCREMENT | 이미지 번호 |
| product_id | INT UNSIGNED | NOT NULL, FK → product ON DELETE CASCADE | 상품 |
| img_path | VARCHAR(200) | NOT NULL | 웹 참조 경로 — `/upload/<파일명>` |
| sort_order | INT | NOT NULL DEFAULT 0 | 표시 순서 |

> 원본 사양은 경로를 `WebContent/upload` 로 적었는데, 이 프로젝트의 웹 루트는 **`WebContents`**(s 있음)다.
> 그리고 `CLAUDE.md` § 4.1 공통 계약은 **저장 = `WebContents/upload/`, DB·웹 참조 = `/upload/<파일명>`**
> 으로 고정돼 있다. DB에는 웹 참조 경로만 넣는다 — 물리 경로를 넣으면 배포 위치가 바뀔 때 전부 깨진다.

### 2.6 `cart` — 장바구니

| 컬럼 | 타입 | 제약 | 설명 |
|---|---|---|---|
| cart_id | INT UNSIGNED | PK, AUTO_INCREMENT | 장바구니 줄 번호 |
| member_id | INT UNSIGNED | NOT NULL, FK → member ON DELETE CASCADE | 회원 |
| product_id | INT UNSIGNED | NOT NULL, FK → product ON DELETE CASCADE | 상품 |
| quantity | INT UNSIGNED | NOT NULL, CHECK (quantity > 0) | 수량 |
| added_at | DATETIME | NOT NULL DEFAULT CURRENT_TIMESTAMP | 담은 시각 |

> `UNIQUE KEY uk_cart_member_product (member_id, product_id)`
> `UNSIGNED` 는 0 을 허용하므로 `CHECK (quantity > 0)` 은 그대로 필요하다.

### 2.7 `wishlist` — 찜

| 컬럼 | 타입 | 제약 | 설명 |
|---|---|---|---|
| member_id | INT UNSIGNED | PK, FK → member ON DELETE CASCADE | 회원 |
| product_id | INT UNSIGNED | PK, FK → product ON DELETE CASCADE | 상품 |
| created_at | DATETIME | NOT NULL DEFAULT CURRENT_TIMESTAMP | 찜한 시각 |

> `PRIMARY KEY (member_id, product_id)` — 대리키 없는 복합 PK. `cart` 와 스타일이 다른 이유는
> 찜은 "줄 단위 수정"이 없어 식별용 단일 키가 필요 없기 때문이다. 토글(INSERT / DELETE)만 있다.

### 2.8 `orders` — 주문

| 컬럼 | 타입 | 제약 | 설명 |
|---|---|---|---|
| order_id | INT UNSIGNED | PK, AUTO_INCREMENT | 주문 번호 (내부) |
| order_no | VARCHAR(20) | NOT NULL, UNIQUE | 화면 표시용 (예: `20261029-0001`) |
| member_id | INT UNSIGNED | NOT NULL, FK → member ON DELETE RESTRICT | 주문 회원 |
| total_price | INT UNSIGNED | NOT NULL | 상품 합계 |
| delivery_fee | INT UNSIGNED | NOT NULL DEFAULT 0 | 배송비 (주문 시점 복사본) |
| order_status | ENUM('정상','취소') | NOT NULL DEFAULT '정상' | 주문 상태 (배송 상태와 별개) |
| receiver | VARCHAR(30) | NOT NULL | 받는 사람 (복사본) |
| receiver_phone | VARCHAR(20) | NOT NULL | 연락처 (복사본) |
| zipcode | VARCHAR(10) | NOT NULL | 우편번호 (복사본) |
| addr1 | VARCHAR(200) | NOT NULL | 기본 주소 (복사본) |
| addr2 | VARCHAR(200) | NULL | 상세 주소 (복사본) |
| delivery_memo | VARCHAR(200) | NULL | 배송 요청사항 |
| ordered_at | DATETIME | NOT NULL DEFAULT CURRENT_TIMESTAMP | 주문일시 |
| canceled_at | DATETIME | NULL | 취소일시 |

> **`order_no` 생성은 MySQL 에서 Oracle 과 다르다.** 시퀀스가 없으므로 아래 중 하나를 쓴다.
> 샌드박스 권장안은 (b) — 단순하고 트랜잭션 안에서 안전하다.
>
> - (a) `order_id` 를 받은 뒤 `UPDATE orders SET order_no = CONCAT(DATE_FORMAT(ordered_at,'%Y%m%d'),'-',LPAD(order_id,4,'0'))`
>   → INSERT 후 한 번 더 UPDATE. 같은 트랜잭션 안에서 처리.
> - (b) `INSERT` 시점에 `CONCAT(DATE_FORMAT(NOW(),'%Y%m%d'),'-',LPAD(LAST_INSERT_ID()+1,...))` 같은 추측은 **쓰지 않는다.**
>   대신 `order_no` 를 **`order_id` 기반으로 INSERT 직후 채운다** = (a) 와 동일.
> - (c) 일자별 카운터를 `system_config` 에 두는 방식은 동시성 처리 때문에 과하다. 채택하지 않는다.
>
> `delivery_fee` 는 `system_config` 의 `DELIVERY_FEE` · `FREE_SHIPPING_MIN` 으로 주문 시점에 계산해
> **복사 저장**한다. 설정이 바뀌어도 과거 주문 금액이 변하지 않는다.
> `delivery_fee = (total_price >= FREE_SHIPPING_MIN) ? 0 : DELIVERY_FEE`

### 2.9 `order_item` — 주문 상품

| 컬럼 | 타입 | 제약 | 설명 |
|---|---|---|---|
| item_id | INT UNSIGNED | PK, AUTO_INCREMENT | 주문 상품 번호 |
| order_id | INT UNSIGNED | NOT NULL, FK → orders ON DELETE CASCADE | 주문 |
| product_id | INT UNSIGNED | NOT NULL, FK → product ON DELETE RESTRICT | 상품 |
| product_name | VARCHAR(100) | NOT NULL | 주문 시점 상품명 (복사본) |
| price | INT UNSIGNED | NOT NULL | 주문 시점 단가 (복사본) |
| quantity | INT UNSIGNED | NOT NULL, CHECK (quantity > 0) | 수량 |

### 2.10 `payment` — 결제 (모의)

| 컬럼 | 타입 | 제약 | 설명 |
|---|---|---|---|
| payment_id | INT UNSIGNED | PK, AUTO_INCREMENT | 결제 번호 |
| order_id | INT UNSIGNED | **NOT NULL**, UNIQUE, FK → orders ON DELETE CASCADE | 주문 (1:1) |
| method | ENUM('카드','무통장','간편결제') | NOT NULL | 결제 수단 (버튼 선택값만 저장) |
| amount | INT UNSIGNED | NOT NULL | `total_price + delivery_fee` |
| pay_status | ENUM('완료','환불') | NOT NULL DEFAULT '완료' | 결제 상태 |
| paid_at | DATETIME | NOT NULL DEFAULT CURRENT_TIMESTAMP | 결제일시 |
| refunded_at | DATETIME | NULL | 환불일시 |

> `order_id` 에 **NOT NULL 을 추가**했다. 1:1 을 UNIQUE 로만 표현하면 MySQL 에서는
> `order_id IS NULL` 인 고아 결제가 여러 건 들어갈 수 있다. `delivery.order_id` 도 같다.
> **외부 PG 연동은 범위 외**(`CLAUDE.md` § 5). 결제는 버튼 선택값 저장 + 상태 전이만 한다.

### 2.11 `delivery` — 배송

| 컬럼 | 타입 | 제약 | 설명 |
|---|---|---|---|
| delivery_id | INT UNSIGNED | PK, AUTO_INCREMENT | 배송 번호 |
| order_id | INT UNSIGNED | **NOT NULL**, UNIQUE, FK → orders ON DELETE CASCADE | 주문 (1:1) |
| status | ENUM('결제완료','상품준비','배송중','배송완료') | NOT NULL DEFAULT '결제완료' | 배송 상태 |
| courier | VARCHAR(30) | NULL | 택배사 |
| tracking_no | VARCHAR(30) | NULL | 운송장 번호 (임의 입력, 외부 조회 없음) |
| paid_at | DATETIME | NULL | 결제완료 시각 |
| ready_at | DATETIME | NULL | 상품준비 시각 |
| shipped_at | DATETIME | NULL | 배송중 전환 시각 |
| delivered_at | DATETIME | NULL | 배송완료 시각 |

### 2.12 `delivery_history` — 배송 상태 이력

| 컬럼 | 타입 | 제약 | 설명 |
|---|---|---|---|
| history_id | INT UNSIGNED | PK, AUTO_INCREMENT | 이력 번호 |
| delivery_id | INT UNSIGNED | NOT NULL, FK → delivery ON DELETE CASCADE | 배송 |
| prev_status | ENUM('결제완료','상품준비','배송중','배송완료') | NULL | 이전 상태 (최초 생성 시 NULL) |
| new_status | ENUM('결제완료','상품준비','배송중','배송완료') | NOT NULL | 변경된 상태 |
| changed_by | INT UNSIGNED | NULL, FK → member ON DELETE SET NULL | 변경자 (시스템 자동이면 NULL) |
| is_forced | ENUM('Y','N') | NOT NULL DEFAULT 'N' | `/dev` 강제 변경 여부 (§ 7.3 요구) |
| changed_at | DATETIME | NOT NULL DEFAULT CURRENT_TIMESTAMP | 변경 시각 |

> `ON DELETE SET NULL` 은 대상 컬럼이 NULL 허용이어야 성립한다 — `changed_by` 가 그렇다.
>
> `is_forced` 를 **읽는 곳은 `/dev` 상태 컨트롤러뿐이다.** 고객 화면의 배송 타임라인에는 노출하지 않는다 —
> "강제로 바뀐 상태"는 시연자에게만 의미가 있는 내부 정보다. → `docs/CRUD.md` § 6-1

### 2.13 `review` — 리뷰

| 컬럼 | 타입 | 제약 | 설명 |
|---|---|---|---|
| review_id | INT UNSIGNED | PK, AUTO_INCREMENT | 리뷰 번호 |
| member_id | INT UNSIGNED | NOT NULL, FK → member ON DELETE RESTRICT | 작성자 |
| product_id | INT UNSIGNED | NOT NULL, FK → product ON DELETE RESTRICT | 상품 |
| item_id | INT UNSIGNED | **NOT NULL**, UNIQUE, FK → order_item ON DELETE CASCADE | 구매 건 (1건당 리뷰 1개) |
| rating | TINYINT UNSIGNED | NOT NULL, CHECK (rating BETWEEN 1 AND 5) | 별점 |
| content | VARCHAR(1000) | NOT NULL | 내용 |
| created_at | DATETIME | NOT NULL DEFAULT CURRENT_TIMESTAMP | 작성일시 |

> `item_id` 에 **NOT NULL 을 추가**했다. "구매한 건에만 리뷰" 라는 규칙이 UNIQUE 만으로는 서지 않는다 —
> NULL 이 허용되면 구매와 무관한 리뷰가 무제한으로 들어간다.
>
> **작성은 강제되지 않는다.** 구매자는 구매 건당 1회 쓸 수 있고 쓰지 않아도 된다 — 즉 리뷰 행의
> **부재가 정상 상태**다. § 1 ERD 의 `ORDER_ITEM ||--o| REVIEW` 가 이미 0..1 로 그려져 있어 맞다.
> `item_id` 의 UNIQUE 가 "1회" 를, 행의 부재가 "안 씀" 을 표현한다. 별도 플래그 컬럼은 필요 없다.
> 작성 조건: `delivery.status = '배송완료'` 인 주문의 `order_item` 에만. 서비스 계층에서 검사한다.
>
> **권한 — 조회는 전체 공개, 수정은 작성자만, 삭제는 작성자와 ADMIN.**
> ADMIN 은 **삭제만 되고 수정은 안 된다.** 남의 글 내용을 고치는 경로를 아예 두지 않고,
> 부적절한 리뷰는 내리는 것으로 처리한다. 화면별 CRUD 는 `docs/CRUD.md` § 6-3.
>
> `product_id` 는 `item_id → order_item → product_id` 로 유도 가능한 **중복 컬럼**이다
> (상품 상세에서 조인을 줄이려는 의도적 비정규화 — § 8 쟁점 4).

### 2.14 `qna` — 상품 문의

| 컬럼 | 타입 | 제약 | 설명 |
|---|---|---|---|
| qna_id | INT UNSIGNED | PK, AUTO_INCREMENT | 문의 번호 |
| member_id | INT UNSIGNED | NOT NULL, FK → member ON DELETE RESTRICT | 작성자 |
| product_id | INT UNSIGNED | NOT NULL, FK → product ON DELETE RESTRICT | 상품 |
| question | VARCHAR(1000) | NOT NULL | 문의 내용 |
| is_secret | ENUM('Y','N') | NOT NULL DEFAULT 'N' | 비밀글 여부 |
| answer | VARCHAR(1000) | NULL | 관리자 답변 |
| created_at | DATETIME | NOT NULL DEFAULT CURRENT_TIMESTAMP | 작성일시 |
| answered_at | DATETIME | NULL | 답변일시 |

> 미답변 목록(27번 `/admin/qna`)의 조건은 **`answered_at IS NULL`** 이다. `answer IS NULL` 과
> 결과는 같지만 인덱스를 걸 수 있는 쪽이 `answered_at` 이다 (§ 2.18).
>
> **불변식**: `answer` 와 `answered_at` 은 **항상 같은 UPDATE 로 함께 쓴다.** 하나만 채우면
> 미답변 판정이 깨진다. 답변 수정 시에도 `answered_at` 을 갱신한다.

### 2.15 `notice` — 공지사항

| 컬럼 | 타입 | 제약 | 설명 |
|---|---|---|---|
| notice_id | INT UNSIGNED | PK, AUTO_INCREMENT | 공지 번호 |
| title | VARCHAR(200) | NOT NULL | 제목 |
| content | TEXT | NOT NULL | 내용 |
| view_count | INT UNSIGNED | NOT NULL DEFAULT 0 | 조회수 |
| created_at | DATETIME | NOT NULL DEFAULT CURRENT_TIMESTAMP | 작성일시 |

### 2.16 `system_config` — 전역 설정

| 컬럼 | 타입 | 제약 | 설명 |
|---|---|---|---|
| config_key | VARCHAR(50) | PK | 설정 키 |
| config_value | VARCHAR(200) | NOT NULL | 설정 값 |
| description | VARCHAR(200) | NULL | 설명 |
| updated_at | DATETIME | NOT NULL DEFAULT CURRENT_TIMESTAMP **ON UPDATE CURRENT_TIMESTAMP** | 수정 시각 |

초기값:

| key | value | 설명 |
|---|---|---|
| `SHOP_STATUS` | `OPEN` | `OPEN` / `MAINTENANCE` — 점검 모드 |
| `DELIVERY_FEE` | `3000` | 기본 배송비 (원) |
| `FREE_SHIPPING_MIN` | `50000` | 무료배송 기준 금액 (원) |
| `LOW_STOCK_THRESHOLD` | `5` | **추가분** — 대시보드 "재고 부족" 기준 (§ 8 쟁점 6) |

> `updated_at` 의 `ON UPDATE CURRENT_TIMESTAMP` 는 MySQL 고유 기능이다. Oracle 이라면 트리거가
> 필요했던 동작이 선언 하나로 끝난다. `/dev/state.jsp` 가 설정을 바꿀 때마다 자동으로 갱신된다.
>
> 값이 전부 문자열이므로 읽는 쪽에서 타입 변환이 필요하다. `Integer.parseInt` 실패에 대비해
> 기본값을 코드에 두는 편이 안전하다 — 설정 한 줄이 지워져도 쇼핑몰이 멈추지 않는다.

### 2.17 `admin_log` — 관리자·dev 조작 기록

| 컬럼 | 타입 | 제약 | 설명 |
|---|---|---|---|
| log_id | INT UNSIGNED | PK, AUTO_INCREMENT | 로그 번호 |
| member_id | INT UNSIGNED | NOT NULL, FK → member ON DELETE RESTRICT | 조작한 계정 |
| mode | ENUM('ADMIN','DEV') | NOT NULL | 조작 모드 |
| action | VARCHAR(50) | NOT NULL | 예: `FORCE_DELIVERY_STATUS` |
| target_table | VARCHAR(30) | NULL | 대상 테이블 |
| target_id | INT UNSIGNED | NULL | 대상 PK |
| detail | VARCHAR(500) | NULL | 변경 전 → 후 값 |
| created_at | DATETIME | NOT NULL DEFAULT CURRENT_TIMESTAMP | 시각 |

`action` 어휘 (고정):

| action | mode | 대상 |
|---|---|---|
| `PRODUCT_CREATE` / `PRODUCT_UPDATE` / `PRODUCT_DELETE` | ADMIN | product |
| `CATEGORY_CREATE` / `CATEGORY_UPDATE` / `CATEGORY_DELETE` | ADMIN | category |
| `DELIVERY_NEXT_STEP` | ADMIN | delivery |
| `DELIVERY_TRACKING_SET` | ADMIN | delivery |
| `ORDER_CANCEL` | ADMIN | orders |
| `MEMBER_ROLE_CHANGE` | ADMIN | member |
| `MEMBER_LEAVE` | ADMIN | member |
| `QNA_ANSWER` | ADMIN | qna |
| `NOTICE_CREATE` / `NOTICE_UPDATE` / `NOTICE_DELETE` | ADMIN | notice |
| `FORCE_DELIVERY_STATUS` | DEV | delivery |
| `FORCE_ORDER_STATUS` | DEV | orders |
| `FORCE_PAY_STATUS` | DEV | payment |
| `FORCE_STOCK_SET` | DEV | product |
| `SHOP_STATUS_TOGGLE` | DEV | system_config |
| `SEED_GENERATE` / `SEED_TRUNCATE_ALL` / `SEED_TRUNCATE_TABLE` | DEV | — |
| `LOGIN_AS` | DEV | member |

> `member_id` 를 **NOT NULL** 로 두었다. 조작 로그에 행위자가 없으면 로그의 존재 의미가 없다.
> ⚠️ 단 `LOGIN_AS` 와 결합하면 감사 구멍이 생긴다 — § 8 쟁점 7.

### 2.18 인덱스

FK 컬럼은 InnoDB 가 자동으로 인덱스를 만든다. 아래는 **추가로 필요한 것**만.

| 인덱스 | 대상 | 쓰이는 화면 |
|---|---|---|
| `idx_product_category_status (category_id, status)` | product | 카테고리별 목록 |
| `idx_product_created (created_at)` | product | 메인 신상품 |
| `idx_orders_member_ordered (member_id, ordered_at)` | orders | 주문내역 |
| `idx_orders_ordered (ordered_at)` | orders | 대시보드 일별 매출 |
| `idx_review_product_created (product_id, created_at)` | review | 상품 상세 리뷰 |
| `idx_qna_product_created (product_id, created_at)` | qna | 상품 상세 문의 |
| `idx_qna_unanswered (answered_at)` | qna | 미답변 목록 (`answered_at IS NULL`) — § 2.18 주의 참조 |
| `idx_delivery_history_timeline (delivery_id, changed_at)` | delivery_history | 배송 타임라인 |
| `idx_admin_log_created (created_at)` | admin_log | `/dev` 로그 조회 |

> ⚠️ **`answer` 컬럼에는 인덱스를 걸 수 없다.** `VARCHAR(1000)` × utf8mb4 = **4000 byte** 로
> InnoDB 인덱스 키 길이 한계 **3072 byte** 를 넘어 `CREATE INDEX` 가 `ERROR 1071` 로 실패한다.
> 그래서 미답변 판정을 `answered_at`(`DATETIME`, 5 byte)으로 옮겼다 — 같은 뜻이면서 키가 작다.
> 접두사 인덱스(`answer(1)`)로도 되지만, 의미가 드러나는 컬럼을 쓰는 편이 읽기 쉽다.
> 전제는 § 2.14 의 불변식(두 컬럼을 함께 쓴다)이다.
>
> 상품명 검색 `WHERE name LIKE '%키워드%'` 는 **B-tree 인덱스를 쓸 수 없다.** 선행 와일드카드 때문이다.
> 샘플 데이터 규모에서는 풀스캔으로 충분하다. 규모를 키울 경우에만 MySQL 의 ngram 전문 검색
> (`FULLTEXT INDEX ... WITH PARSER ngram`)을 검토한다. 한국어는 공백 토크나이저가 안 맞아 ngram 이 필요하다.

### 2.19 테이블 생성 순서 — FK 의존 순서

DDL 작성의 근거다. FK 는 참조 대상이 이미 존재해야 걸리므로 **부모부터** 만든다.
`DROP` 은 이 순서의 **역순**이다 — 그래야 `schema.sql` 이 재실행 가능해진다 (`docs/tracks/SCHEMA.md` § 4).

| 단계 | 테이블 | 참조하는 테이블 |
|---|---|---|
| 1 | `member` | — |
| 2 | `category` | `category` (자기 참조 — `parent_id` NULL 허용) |
| 3 | `product` | `category` |
| 4 | `notice` | — |
| 5 | `system_config` | — |
| 6 | `address` | `member` |
| 7 | `product_image` | `product` |
| 8 | `cart` | `member`, `product` |
| 9 | `wishlist` | `member`, `product` |
| 10 | `orders` | `member` |
| 11 | `order_item` | `orders`, `product` |
| 12 | `payment` | `orders` |
| 13 | `delivery` | `orders` |
| 14 | `delivery_history` | `delivery`, `member` |
| 15 | `review` | `member`, `product`, `order_item` |
| 16 | `qna` | `member`, `product` |
| 17 | `admin_log` | `member` |

**뷰는 테이블 전체를 만든 뒤 마지막에** 만든다 (§ 3 의 5개). 뷰끼리는 서로 참조하지 않으므로 순서는 자유다.

**샘플 데이터(`data.sql`) INSERT 순서**도 같은 의존 순서를 따른다. 두 가지만 추가로 주의한다.

1. `category` 는 **대분류(`parent_id IS NULL`) 를 먼저** 넣고 소분류를 넣는다. 자기 참조라
   역순이면 FK 가 걸린다.
2. `system_config` 는 초기 4건(`SHOP_STATUS` · `DELIVERY_FEE` · `FREE_SHIPPING_MIN` ·
   `LOW_STOCK_THRESHOLD`)을 반드시 넣는다. § 2.16 — 이 값이 없으면 장바구니·주문서의
   금액 계산과 헤더 점검 판정이 동작하지 않는다.

> `DEV` 계정은 `data.sql` 로만 만든다 (§ 5). 화면에서는 생성 경로가 없다.

---

## 3. 뷰 명세

원본 § 7.1 은 뷰 4개를 **이름으로만** 지정했다. 아래는 그 정의다. `/dev/data.jsp` 가 테이블과 함께
뷰도 조회하므로 허용 목록에 포함한다.

### 3.1 `v_sales_daily` — 일별 매출

```sql
CREATE OR REPLACE VIEW v_sales_daily AS
SELECT DATE(ordered_at)                AS sales_date,
       COUNT(*)                        AS order_count,
       SUM(total_price)                AS goods_amount,
       SUM(delivery_fee)               AS delivery_amount,
       SUM(total_price + delivery_fee) AS total_amount
FROM   orders
WHERE  order_status = '정상'
GROUP  BY DATE(ordered_at);
```

> `order_item` 을 조인하지 않는다. 조인하면 주문 1건이 상품 수만큼 늘어나 `delivery_fee` 가
> 중복 합산된다. 금액은 `orders` 에서만 집계한다.

### 3.2 `v_delivery_status_count` — 배송 상태별 대기 건수

```sql
CREATE OR REPLACE VIEW v_delivery_status_count AS
SELECT d.status   AS delivery_status,
       COUNT(*)   AS order_count
FROM   delivery d
JOIN   orders   o ON o.order_id = d.order_id
WHERE  o.order_status = '정상'
GROUP  BY d.status;
```

> ⚠️ 건수가 0인 상태는 **행이 아예 나오지 않는다.** 대시보드가 4단계를 항상 보여줘야 하므로,
> JSP 쪽에서 4개 상태를 0으로 초기화한 뒤 조회 결과를 덮어쓴다. 뷰에서 해결하려면 상태 목록
> 테이블과 LEFT JOIN 해야 하는데, 그 테이블을 만드는 건 과하다.

### 3.3 `v_admin_order` — 관리자 주문 목록

```sql
CREATE OR REPLACE VIEW v_admin_order AS
SELECT o.order_id, o.order_no, o.ordered_at, o.canceled_at,
       o.order_status, o.total_price, o.delivery_fee,
       o.total_price + o.delivery_fee          AS pay_amount,
       o.receiver, o.receiver_phone,
       o.zipcode, o.addr1, o.addr2, o.delivery_memo,
       m.member_id, m.login_id, m.name          AS member_name,
       p.payment_id, p.method                   AS pay_method,
       p.pay_status, p.paid_at, p.refunded_at,
       d.delivery_id, d.status                  AS delivery_status,
       d.courier, d.tracking_no,
       d.ready_at, d.shipped_at, d.delivered_at,
       (SELECT COUNT(*) FROM order_item oi
         WHERE oi.order_id = o.order_id)        AS item_count,
       (SELECT oi.product_name FROM order_item oi
         WHERE oi.order_id = o.order_id
         ORDER BY oi.item_id LIMIT 1)           AS first_product_name
FROM      orders   o
JOIN      member   m ON m.member_id = o.member_id
LEFT JOIN payment  p ON p.order_id  = o.order_id
LEFT JOIN delivery d ON d.order_id  = o.order_id;
```

> `payment` · `delivery` 는 주문과 같은 트랜잭션에서 생성되므로 실제로는 항상 존재한다.
> 그래도 `LEFT JOIN` 을 쓴 이유는 `/dev` 가 데이터를 강제로 비틀 수 있기 때문이다 —
> 반쪽 주문이 생겨도 관리자 화면이 그 행을 **감추지 않고 보여줘야** 이상을 알아챌 수 있다.
> 목록 제목은 `first_product_name` + `item_count - 1` 로 "티셔츠 외 2건" 형태로 만든다.

### 3.4 `v_admin_member` — 관리자 회원 목록

```sql
CREATE OR REPLACE VIEW v_admin_member AS
SELECT m.member_id, m.login_id, m.name, m.email, m.phone,
       m.role, m.status, m.created_at,
       (SELECT COUNT(*) FROM orders o
         WHERE o.member_id = m.member_id
           AND o.order_status = '정상')                       AS order_count,
       (SELECT IFNULL(SUM(o.total_price + o.delivery_fee), 0)
          FROM orders o
         WHERE o.member_id = m.member_id
           AND o.order_status = '정상')                       AS pay_total,
       (SELECT MAX(o.ordered_at) FROM orders o
         WHERE o.member_id = m.member_id)                     AS last_ordered_at,
       (SELECT COUNT(*) FROM review r
         WHERE r.member_id = m.member_id)                     AS review_count,
       (SELECT COUNT(*) FROM qna q
         WHERE q.member_id = m.member_id
           AND q.answered_at IS NULL)                         AS qna_waiting
FROM   member m;
```

> `qna_waiting` 의 판정은 **`answered_at IS NULL`** 이다. § 2.14 의 불변식과 § 2.18 의
> `idx_qna_unanswered` 를 따른다 — 미답변 판정 기준은 문서 전체에서 `answered_at` 하나로 통일한다.

### 3.5 `v_low_stock` — 재고 부족 상품 **(추가분)**

```sql
CREATE OR REPLACE VIEW v_low_stock AS
SELECT p.product_id, p.name, p.stock, p.status,
       c.name AS category_name
FROM   product  p
JOIN   category c ON c.category_id = p.category_id
WHERE  p.status = '판매중'
  AND  p.stock <= CAST((SELECT config_value FROM system_config
                         WHERE config_key = 'LOW_STOCK_THRESHOLD') AS UNSIGNED);
```

> ⚠️ **이 뷰는 선택 기능이 아니다.** § 2.4 노출 규칙에 따라 `stock = 0` 인 상품은 사용자 화면에서
> 사라지므로, 관리자가 그 사실을 알 수 있는 **유일한 경로**가 이 뷰다. `stock = 0` 도
> 임계값 이하라 함께 잡힌다 — 0 과 1~5 를 구분해 보여주면 "이미 사라진 것"과
> "사라지기 직전인 것"이 나뉜다.

> 원본 § 7.1 의 `dashboard.jsp` 는 기능이 **3개**(일별 매출 · 배송 상태별 대기 · 재고 부족)인데
> 뷰는 **2개**만 지정되어 있었다. 재고를 담은 것이 없어 이 뷰를 추가했다. 기준값은 하드코딩하지 않고
> `system_config.LOW_STOCK_THRESHOLD` 에서 읽는다 — `/dev` 에서 조절해 시연할 수 있어야 하므로.

---

## 4. 배송 상태 전이 규칙

| 상태 | 전환 시점 | 처리 주체 | 기록 값 | 주문 취소 |
|---|---|---|---|---|
| 결제완료 | 결제 버튼 클릭 | 시스템 (자동) | `paid_at` | 가능 |
| 상품준비 | 관리자 주문 확인 | 관리자 | `ready_at` | 가능 |
| 배송중 | 운송장 번호 입력 | 관리자 | `courier`, `tracking_no`, `shipped_at` | 불가 |
| 배송완료 | 배송완료 처리 | 관리자 | `delivered_at` | 불가 (리뷰 작성 가능) |

**정상 경로는 단방향이다.** 역방향·건너뛰기는 `/dev/state.jsp` 만 할 수 있고, 그때는
`delivery_history.is_forced = 'Y'` 로 구분된다 (§ 6.4).

---

## 5. 권한 · 라우팅

| 항목 | 내용 |
|---|---|
| `member.role` | `ENUM('USER','ADMIN','DEV')` |
| `/admin/*` | `ADMIN`, `DEV` 접근 가능 |
| `/dev/*` | `DEV` 만 접근 가능 |
| 접근 제어 | `AdminFilter` · `DevFilter` (Servlet Filter)가 세션의 `role` 을 검사, 없으면 메인으로 redirect |
| 권한 부여 제한 | **ADMIN 은 DEV 권한을 줄 수 없다.** DEV 계정은 `sql/data.sql` 더미 스크립트로만 생성 |

**필터 체인 순서** (`web.xml`) — 순서가 틀리면 점검 모드가 관리자를 잠근다.

```
1. EncodingFilter   /*            요청 인코딩 UTF-8
2. ShopFilter       /*            SHOP_STATUS=MAINTENANCE 면 일반 사용자를 점검 페이지로.
                                  /admin/*, /dev/*, 정적 자원(css·js·images·upload)은 통과
3. AdminFilter      /admin/*      role IN ('ADMIN','DEV')
4. DevFilter        /dev/*        role = 'DEV'
```

> `ShopFilter` 가 `AdminFilter` 보다 **먼저** 와야 한다. 반대면 점검 모드에서 관리자도 막혀
> 점검 모드를 해제할 수 없게 된다.
>
> 서버 측 권한 검사는 필터만으로 끝나지 않는다. `/admin/members.jsp` 의 권한 변경 처리에서
> **요청 파라미터가 `DEV` 면 거부**해야 한다. 필터는 "경로 접근"만 막고, "권한 상승"은
> 처리 로직에서 막는 문제다.

---

## 6. 트랜잭션 · 처리 규칙

DBCP 커넥션은 기본값이 `autoCommit = true` 다. 아래 다중 문장 작업은 모두
`conn.setAutoCommit(false)` → 성공 시 `commit()` → 예외 시 `rollback()` → `finally` 에서
`setAutoCommit(true)` 복원 후 반납해야 한다. **복원을 빼먹으면 풀에 돌아간 커넥션이 다음 요청에서
자동 커밋을 하지 않아, 원인을 찾기 힘든 버그가 된다.**

### 6.1 결제 버튼 클릭 (한 트랜잭션)

1. `orders` INSERT → `order_id` 획득 → `order_no` UPDATE (§ 2.8)
2. `order_item` INSERT (장바구니 줄마다)
3. `payment` INSERT (`amount = total_price + delivery_fee`)
4. `delivery` INSERT (`status = '결제완료'`, `paid_at = NOW()`)
5. `delivery_history` INSERT (`prev_status = NULL`, `new_status = '결제완료'`, `is_forced = 'N'`)
6. 재고 차감 — `UPDATE product SET stock = stock - ? WHERE product_id = ? AND stock >= ?`
   **영향 행이 0이면 재고 부족이므로 rollback.**
7. 장바구니 비우기 — `DELETE FROM cart WHERE member_id = ?`

### 6.2 관리자 상태 변경 (다음 단계 버튼)

```sql
UPDATE delivery
   SET status = ?, <해당 시각 컬럼> = NOW()
 WHERE delivery_id = ? AND status = ?   -- 직전 상태
```

영향 행이 0이면 잘못된 전이(또는 동시 클릭)이므로 rollback. 성공하면 `delivery_history` INSERT
(`is_forced = 'N'`) + `admin_log` INSERT (`DELIVERY_NEXT_STEP`).

### 6.3 주문 취소

`delivery.status` 가 `'결제완료'` 또는 `'상품준비'` 일 때만 허용.

1. `orders.order_status = '취소'`, `canceled_at = NOW()`
2. `payment.pay_status = '환불'`, `refunded_at = NOW()`
3. 재고 복구 — `UPDATE product SET stock = stock + ? WHERE product_id = ?` (주문 상품마다)
4. `admin_log` INSERT (`ORDER_CANCEL`)

> `delivery` 는 건드리지 않는다. 그래서 취소된 주문의 배송 상태가 `'상품준비'` 로 남는다 —
> § 8 쟁점 1 참조.

### 6.4 `/dev` 강제 상태 변경

- 상태 컬럼을 바꾸면서 **해당 단계의 시각 컬럼도 함께 맞춘다.**
- 역방향으로 되돌릴 때는 **이후 단계의 시각 컬럼을 NULL 로 비운다.**
  예: `배송완료 → 상품준비` 로 되돌리면 `shipped_at`, `delivered_at` 을 NULL 로. `courier`·`tracking_no` 도 함께 비운다.
- 기록을 **두 곳에** 남긴다 — `delivery_history` (`is_forced = 'Y'`) + `admin_log` (`FORCE_DELIVERY_STATUS`).
- 강제 취소는 **재고 복구 여부를 체크박스로 선택**할 수 있게 한다.

### 6.5 `/dev` 데이터 브라우저 — SQL 인젝션

**테이블·뷰 이름은 `?` 바인딩이 되지 않는다.** 반드시 허용 목록(불변 `Set`)에 있는 이름만
SQL 문자열에 넣고, 식별자는 백틱으로 감싼다.

```java
// 17 테이블 + 뷰 5개. 이 Set 밖의 이름은 즉시 거부한다.
private static final Set<String> ALLOWED = Set.of(
    "member", "address", "category", "product", "product_image",
    "cart", "wishlist", "orders", "order_item", "payment",
    "delivery", "delivery_history", "review", "qna", "notice",
    "system_config", "admin_log",
    "v_sales_daily", "v_delivery_status_count",
    "v_admin_order", "v_admin_member", "v_low_stock");
```

정렬 컬럼·정렬 방향도 같다 — 파라미터로 받은 컬럼명을 그대로 `ORDER BY` 에 붙이면 같은 구멍이다.
컬럼 역시 화이트리스트로 검증한다.

### 6.6 `/dev` 설계 보기 (`erd.jsp`)

- **탭 1 — Mermaid 렌더링**: `mermaid.min.js` 를 프로젝트 안에 두고 로드한다
  (`WebContents/js/mermaid.min.js`). **CDN 금지** (`CLAUDE.md` § 5 — 오프라인 localhost 에서 깨지면 안 된다).
  ⚠️ 이 파일은 저장소에 없다. 실험 전에 수동으로 받아 넣어야 한다 (§ 8 쟁점 8).
- **탭 2 — 실제 스키마 조회**: Oracle 데이터 딕셔너리는 MySQL 에 없다. 아래로 치환한다.

```sql
-- USER_TAB_COLUMNS 대체
SELECT table_name, column_name, ordinal_position,
       column_type, is_nullable, column_key, column_default, extra
FROM   information_schema.columns
WHERE  table_schema = DATABASE()
ORDER  BY table_name, ordinal_position;

-- USER_CONSTRAINTS 대체
SELECT tc.table_name, tc.constraint_name, tc.constraint_type,
       kcu.column_name, kcu.referenced_table_name, kcu.referenced_column_name,
       rc.delete_rule, rc.update_rule
FROM        information_schema.table_constraints tc
LEFT JOIN   information_schema.key_column_usage kcu
       ON  kcu.constraint_name  = tc.constraint_name
      AND  kcu.table_schema     = tc.table_schema
      AND  kcu.table_name       = tc.table_name
LEFT JOIN   information_schema.referential_constraints rc
       ON  rc.constraint_name   = tc.constraint_name
      AND  rc.constraint_schema = tc.table_schema
WHERE  tc.table_schema = DATABASE()
ORDER  BY tc.table_name, tc.constraint_type, kcu.ordinal_position;

-- CHECK 제약 (MySQL 8.0.16+)
SELECT cc.constraint_name, cc.check_clause
FROM   information_schema.check_constraints cc
WHERE  cc.constraint_schema = DATABASE();
```

> `information_schema` 조회는 `DATABASE()` 를 쓰므로 스키마명을 파라미터로 받을 필요가 없다 —
> 인젝션 표면이 생기지 않는다.

### 6.7 `/dev` 초기화 (`seed.jsp`)

FK 때문에 `TRUNCATE` 는 순서가 중요하고, 참조되는 테이블은 `TRUNCATE` 자체가 거부된다.

```sql
SET FOREIGN_KEY_CHECKS = 0;
TRUNCATE TABLE delivery_history;  TRUNCATE TABLE delivery;
TRUNCATE TABLE payment;           TRUNCATE TABLE review;
TRUNCATE TABLE order_item;        TRUNCATE TABLE orders;
TRUNCATE TABLE cart;              TRUNCATE TABLE wishlist;
TRUNCATE TABLE qna;               TRUNCATE TABLE product_image;
TRUNCATE TABLE product;           TRUNCATE TABLE category;
TRUNCATE TABLE notice;            TRUNCATE TABLE admin_log;
-- member 는 비우지 않는다. DEV 계정이 사라지면 /dev 에 다시 들어갈 수 없다.
SET FOREIGN_KEY_CHECKS = 1;
```

> ⚠️ **`member` 전체 초기화 금지.** DEV 계정이 날아가면 `/dev` 진입 경로가 사라지고,
> ADMIN 은 DEV 권한을 줄 수 없으므로(§ 5) 더미 스크립트를 다시 돌리는 것 말고 복구 수단이 없다.
> "전체 초기화" 버튼은 `member` 에서 `role = 'USER'` 인 행만 지우도록 한다.
> `TRUNCATE` 는 `AUTO_INCREMENT` 를 1로 되돌린다 — 그게 시연에서 원하는 동작이다.

---

## 7. DTO 필드 계약

JSP 배선이 소비하는 객체. `src/main/java/shop/dto/`.

**타입 규칙**

| DB | Java | 이유 |
|---|---|---|
| `INT UNSIGNED` (PK·FK·금액·수량) | `int` | AUTO_INCREMENT 가 21억을 넘지 않는다. NULL 허용 FK 는 `Integer` |
| `TINYINT UNSIGNED` | `int` | 별점 1~5 |
| `VARCHAR` / `TEXT` / `ENUM` | `String` | ENUM 은 JSTL 문자열 비교(`${d.status == '배송중'}`)로 그대로 쓴다 |
| `DATETIME` | `java.sql.Timestamp` | **`<fmt:formatDate>` 가 `java.util.Date` 를 요구한다.** `Timestamp` 는 그 하위 타입이라 바로 들어간다. `LocalDateTime` 은 별도 변환이 필요하다 |

| DTO | 필드 |
|---|---|
| `MemberDTO` | `int memberId` · `String loginId, password, name, email, phone, role, status` · `Timestamp createdAt` |
| `AddressDTO` | `int addrId, memberId` · `String addrName, receiver, receiverPhone, zipcode, addr1, addr2, isDefault` |
| `CategoryDTO` | `int categoryId` · `Integer parentId` · `String name` · `int sortOrder` · `List<CategoryDTO> children` |
| `ProductDTO` | `int productId, categoryId` · `String categoryName` *(조인)* · `String name` · `int price, stock` · `String description, thumbnail, status` · `Timestamp createdAt` · `List<ProductImageDTO> images` · `double avgRating` · `int reviewCount` *(집계)* |
| `ProductImageDTO` | `int imageId, productId` · `String imgPath` · `int sortOrder` |
| `CartItemDTO` | `int cartId, memberId, productId` · `String productName, thumbnail, status` · `int price, stock, quantity` · `int lineTotal` *(유도: price × quantity)* |
| `WishItemDTO` | `int memberId, productId` · `String productName, thumbnail, status` · `int price` · `Timestamp createdAt` |
| `OrderDTO` | `int orderId` · `String orderNo` · `int memberId` · `int totalPrice, deliveryFee, payAmount` *(유도)* · `String orderStatus, receiver, receiverPhone, zipcode, addr1, addr2, deliveryMemo` · `Timestamp orderedAt, canceledAt` · `List<OrderItemDTO> items` · `PaymentDTO payment` · `DeliveryDTO delivery` |
| `OrderItemDTO` | `int itemId, orderId, productId` · `String productName` · `int price, quantity, lineTotal` *(유도)* · `boolean reviewWritten` *(유도)* |
| `PaymentDTO` | `int paymentId, orderId` · `String method, payStatus` · `int amount` · `Timestamp paidAt, refundedAt` |
| `DeliveryDTO` | `int deliveryId, orderId` · `String status, courier, trackingNo` · `Timestamp paidAt, readyAt, shippedAt, deliveredAt` · `List<DeliveryHistoryDTO> history` |
| `DeliveryHistoryDTO` | `int historyId, deliveryId` · `String prevStatus, newStatus, isForced` · `Integer changedBy` · `String changedByName` *(조인)* · `Timestamp changedAt` |
| `ReviewDTO` | `int reviewId, memberId, productId, itemId` · `String memberName` *(마스킹)* · `String productName` *(조인)* · `int rating` · `String content` · `Timestamp createdAt` |
| `QnaDTO` | `int qnaId, memberId, productId` · `String memberName` *(마스킹)* · `String productName` *(조인)* · `String question, isSecret, answer` · `Timestamp createdAt, answeredAt` |
| `NoticeDTO` | `int noticeId` · `String title, content` · `int viewCount` · `Timestamp createdAt` |
| `SystemConfigDTO` | `String configKey, configValue, description` · `Timestamp updatedAt` |
| `AdminLogDTO` | `int logId, memberId` · `String memberName` *(조인)* · `String mode, action, targetTable, detail` · `Integer targetId` · `Timestamp createdAt` |
| `SalesDailyDTO` | `java.sql.Date salesDate` · `int orderCount, goodsAmount, deliveryAmount, totalAmount` |
| `DeliveryStatusCountDTO` | `String deliveryStatus` · `int orderCount` |
| `LowStockDTO` | `int productId` · `String name, status, categoryName` · `int stock` |
| `PageDTO<T>` | `List<T> list` · `int page, size, total, totalPages` |

> **마스킹**: 리뷰·문의 목록에 노출되는 작성자명은 `홍*동` 형태로 가린다. 조회 SQL 에서
> 가공하지 말고 DTO 세팅 시점에 처리한다 — 관리자 화면은 원본 이름이 필요하다.
> **유도 필드**(`payAmount`, `lineTotal`, `reviewWritten`)는 DB 컬럼이 아니다. getter 에서 계산하거나
> 서비스 계층에서 채운다. 테이블에 추가하지 않는다.

---

## 8. 설계 점검 — 발견한 쟁점

번호가 작은 쪽이 영향이 크다.

### 쟁점 1 — 취소된 주문의 배송 상태가 멈춘 채 남는다 ⚠️

`delivery.status` 의 ENUM 에 `'취소'` 가 없고, 주문 취소(§ 6.3)는 `delivery` 를 건드리지 않는다.
결과: 취소된 주문의 배송 조회 화면이 `'상품준비'` 를 계속 보여준다. 사용자는 취소한 주문이
준비되고 있다고 읽는다.

**권장**: 스키마를 바꾸지 않고 **화면에서 `orders.order_status` 를 먼저 읽는다.**
`order_status = '취소'` 면 배송 타임라인 대신 "취소 완료"를 렌더한다.
`delivery` 에 `'취소'` 를 추가하는 방법도 있지만, 그러면 단방향 전이 규칙(§ 4)에
예외 경로가 생기고 `/dev` 역방향 로직까지 복잡해진다. 화면 분기가 싸다.

### 쟁점 2 — ~~`status = '품절'` 과 `stock = 0` 이 이중 진실이다~~ → **해소 (2026-10-29)**

**결론: ENUM 에서 `'품절'` 을 제거하고, 재고 없는 상품은 사용자에게 노출하지 않는다.**

```sql
status ENUM('판매중','숨김') NOT NULL DEFAULT '판매중'
```

원래 문제는 품절을 두 곳(`status` 와 `stock`)이 말한다는 것이었다. 애초에 `'품절'` 을 두지 않으면
**모순이 생길 자리가 없어진다** — `status` 는 관리자의 전시 의도만, `stock` 은 수량만 담는다.

품절 배지 대신 **탐색 경로에서 제외**한다. 상세 규칙은 § 2.4 "노출 규칙" 이다.

| 얻는 것 | 잃는 것 |
|---|---|
| 동기화 코드가 **불필요하다.** 결제·취소·관리자·`/dev` 네 곳에서 `status` 를 맞출 일이 없다 | 사용자가 "이 상품은 품절" 을 알 수 없다. 그냥 안 보인다 |
| 서비스 계층 불변식이 **늘지 않는다** (§ 9.6 이 7건에서 그대로) | 재고가 0이 되면 상품이 조용히 사라진다 → `v_low_stock` 이 필수가 된다 (§ 3.5) |
| `stock → status` 잠재 함수 종속이 사라진다 (§ 10.2) | — |

> 재고 0 상품을 숨기는 쪽이 **"없는 재고를 파는 것"보다 안전하다.** 품절 배지 방식은
> 표시를 빠뜨린 화면 하나가 곧 주문 실패로 이어지는데, 숨기는 방식은 빠뜨릴 표시가 없다.

### 쟁점 3 — CRUD 매트릭스가 테이블 4개를 누락했다 ⚠️

원본 § 4 매트릭스의 컬럼에 `CATEGORY` · `PRODUCT_IMAGE` · `WISHLIST` · `NOTICE` 가 **아예 없다.**
ERD 에는 있는데 어느 화면도 읽거나 쓰지 않는다. 매트릭스의 존재 목적이 바로 이 누락 검출이므로,
이건 매트릭스가 제 일을 한 결과다.

`docs/CRUD.md` 에서 화면 × 테이블로 확장해 4개를 모두 배선했다.
연결된 화면: 카테고리 → 메인 네비·목록·관리자 / 상품이미지 → 상품상세·관리자 /
찜 → 상품상세 토글·마이페이지 / 공지 → 공지 목록·상세·관리자.

### 쟁점 4 — `review` 의 `product_id` 는 중복 컬럼이다

`item_id → order_item → product_id` 로 유도할 수 있다. 두 값이 어긋나면
"A 상품을 산 기록에 B 상품 리뷰"가 성립한다. DB 제약으로는 막을 수 없다.

**판단**: 의도적 비정규화로 **유지**한다. 상품 상세의 리뷰 목록이 `order_item` 조인 없이
`WHERE product_id = ?` 로 끝나는 이득이 크다. 대신 불변식을 서비스 계층에 못 박는다 —
리뷰 INSERT 시 `product_id` 를 요청 파라미터에서 받지 않고 **`item_id` 로 조회해 채운다.**

### 쟁점 5 — 우편번호 검색을 외부 API로 할 수 없다

주소 입력은 보통 다음/카카오 우편번호 서비스를 쓰는데, 그건 외부 CDN 스크립트라
`CLAUDE.md` § 5(외부 API · CDN 금지)에 정면으로 걸린다.

**권장**: 우편번호·주소를 **직접 입력 필드**로 둔다. 검색 팝업이 꼭 필요하면 샘플 주소
수십 건을 로컬 테이블이나 JSON 으로 넣고 그 안에서 검색한다. 평가 기준은 세 계층의
연결성(§ 2 항목 11)이므로 주소 검색의 완성도는 가점 요소가 아니다.

### 쟁점 6 — 대시보드 기능 3개 vs 지정된 뷰 2개

§ 7.1 `dashboard.jsp` 는 재고 부족 상품을 보여주는데 뷰 목록에 재고 관련이 없었다.
`v_low_stock` 과 `system_config.LOW_STOCK_THRESHOLD` 를 추가했다 (§ 3.5, § 2.16).

### 쟁점 7 — `login-as` 가 `admin_log` 에 감사 구멍을 만든다

DEV 가 `login-as` 로 일반 회원 세션으로 전환한 뒤 조작하면, `admin_log.member_id` 에는
**그 회원**이 기록된다. 실제 행위자인 DEV 가 로그에서 사라진다.

**권장**: 스키마를 바꾸지 않고 세션에 원래 DEV 의 `member_id` 를 별도 키
(예: `impersonatorId`)로 보관하고, 로그를 남길 때 `detail` 에 `impersonated_by=<devId>` 를 덧붙인다.
컬럼 추가 없이 추적이 복원된다.

### 쟁점 8 — `mermaid.min.js` 가 저장소에 없다

`erd.jsp` 탭 1 의 선행 조건이다. CDN 은 금지이므로 파일을 직접 넣어야 하는데
현재 `WebContents/js/` 에 없다. 파일을 받아 두는 것은 네트워크 작업이라 이 세션에서 하지 않았다.
**실험 전 수동 배치 필요**: `WebContents/js/mermaid.min.js`.
없으면 탭 1 만 비고 탭 2(실제 스키마 조회)는 정상 동작하게 만들어 둔다.

### 쟁점 9 — 탈퇴 회원의 이메일이 영구 점유된다

소프트 삭제(`status = 'LEFT'`)인데 `email` 과 `login_id` 가 UNIQUE 다. 탈퇴해도 행이 남아
같은 이메일로 재가입할 수 없다.

**판단**: 이 범위에서는 **그대로 둔다.** 재가입 요구사항이 없고, 해소하려면 탈퇴 시
이메일을 치환(`left_<id>@…`)해야 하는데 그건 개인정보 이력을 훼손한다. 재가입을 지원해야 하면
UNIQUE 를 `(email, status)` 복합으로 바꾸는 쪽을 검토한다.

### 쟁점 10 — `payment.paid_at` 과 `delivery.paid_at` 이 같은 값을 두 번 저장한다

결제 시각이 두 테이블에 있다. 어긋날 수 있다.

**판단**: `delivery.paid_at` 은 **배송 단계별 시각 4개의 일관된 자리**로서 의미가 있다
(`paid_at` → `ready_at` → `shipped_at` → `delivered_at`). 타임라인 렌더가 한 테이블에서 끝난다.
유지하되 § 6.1 에서 **같은 `NOW()` 값을 두 곳에 쓴다**는 점을 명시한다.

---

## 9. 무결성 제약조건 점검

2026-10-29 · § 2 의 17 테이블 전수 점검. **현재 선 제약과 빠진 제약을 구분해 적는다.**

### 9.1 현재 걸려 있는 제약 — 집계

| 종류 | 수 | 비고 |
|---|---|---|
| PRIMARY KEY | 17 | 대리키 15 · 자연키 1 (`system_config.config_key`) · 복합키 1 (`wishlist`) |
| UNIQUE | 7 | `member.login_id` · `member.email` · `orders.order_no` · `payment.order_id` · `delivery.order_id` · `review.item_id` · `cart(member_id, product_id)` |
| FOREIGN KEY | 21 | CASCADE 11 · RESTRICT 9 · SET NULL 1 |
| CHECK | **3** | `cart.quantity > 0` · `order_item.quantity > 0` · `review.rating BETWEEN 1 AND 5` |
| ENUM (도메인) | 13 컬럼 | `CHECK IN (...)` 을 대체 (§ 0.1) |
| UNSIGNED (도메인) | 금액·수량·재고 전부 | `CHECK (>= 0)` 을 대체 (§ 0) |

### 9.2 개체 무결성 — 이상 없음

17 테이블 전부 PK 가 있고 NULL 이 불가능하다. 후보키가 둘 이상인 테이블은 둘 다 UNIQUE 로 선언돼 있다.

| 테이블 | 후보키 |
|---|---|
| `member` | `member_id` · `login_id` · `email` |
| `cart` | `cart_id` · `(member_id, product_id)` |
| `payment` · `delivery` | 각 `*_id` · `order_id` |
| `review` | `review_id` · `item_id` |
| `orders` | `order_id` · `order_no` |

> `system_config.config_key` 는 `VARCHAR(50)` 자연키다. utf8mb4 기준 200 byte 로 InnoDB 키 한계
> 3072 byte 안쪽이라 문제없다 (§ 2.18 의 `answer` 와 달리).

### 9.3 참조 무결성 — 삭제 정책의 의도

| 정책 | 대상 | 의도 |
|---|---|---|
| **CASCADE** (11) | `address` · `cart` · `wishlist` ← `member` / `product_image` · `cart` · `wishlist` ← `product` / `order_item` · `payment` · `delivery` ← `orders` / `delivery_history` ← `delivery` / `review` ← `order_item` | **부모가 사라지면 의미가 없는 종속 데이터.** 장바구니·찜은 회원의 사적 상태이고, 주문 구성요소는 주문 없이 존재할 수 없다 |
| **RESTRICT** (9) | `orders` · `review` · `qna` · `admin_log` ← `member` / `order_item` · `review` · `qna` ← `product` / `product` · `category` ← `category` | **기록이므로 남아야 한다.** 부모 삭제를 막아 이력이 사라지지 않게 한다 |
| **SET NULL** (1) | `delivery_history.changed_by` ← `member` | 변경자가 사라져도 이력 자체는 남는다 |

**이 정책이 만드는 결과 세 가지**

1. **회원·상품은 사실상 물리 삭제가 불가능하다.** `orders`/`review`/`qna`/`admin_log` 가 회원을,
   `order_item`/`review`/`qna` 가 상품을 RESTRICT 로 잡는다. 그래서 § 2.1 의 소프트 삭제
   (`status='LEFT'`)와 § 8 쟁점 2 의 `status='숨김'` 이 **선택이 아니라 유일한 경로**다. 일관성 있다.
2. **CASCADE 연쇄가 2단계 있다.** `orders` 삭제 → `order_item` → `review`.
   주문을 지우면 **리뷰까지 조용히 사라진다.** 화면에는 주문 삭제 경로가 없고
   33번 `/dev/seed` 만 이 길을 열 수 있으므로 운영상 위험은 없다. 다만 시연 중 초기화하면
   리뷰가 함께 날아가는 것이 정상 동작임을 알고 있어야 한다.
3. **`delivery_history.changed_by` 의 SET NULL 은 실제로 발동하지 않는다.** 회원이 물리 삭제되지
   않으므로(1번) 영원히 트리거되지 않는 정책이다. 해롭지는 않으나 죽은 코드에 가깝다.

#### ⚠️ 빠진 것 1 — `ON UPDATE` 를 어디에도 지정하지 않았다

21개 FK 전부 `ON DELETE` 만 있고 `ON UPDATE` 가 없다. MySQL 기본값은 `RESTRICT` 라 동작은 안전하지만,
**DDL 에 명시되지 않으면 읽는 사람이 의도를 알 수 없다.** 모든 PK 가 변하지 않는 대리키이므로
`ON UPDATE RESTRICT` 를 명시하는 것이 맞다. → DB Schema 트랙 전달 사항.

#### ⚠️ 빠진 것 2 — `admin_log` 의 다형 참조는 FK 를 걸 수 없다

`target_table`(VARCHAR) + `target_id`(INT) 조합은 **대상 테이블이 행마다 다르므로 FK 가 불가능하다.**
`target_id = 9999` 처럼 존재하지 않는 행을 가리켜도 DB 가 막지 못한다.

**판단: 그대로 둔다.** 감사 로그에서 흔한 패턴이고, 대안(테이블별 로그 17개 또는 nullable FK 17개)이
훨씬 나쁘다. 대신 § 2.17 의 `action` 어휘를 고정해 둔 것이 사실상의 방어선이다 —
`target_table` 에 임의 문자열이 들어가지 않도록 **서비스 계층에서 허용 목록으로 검증**한다
(§ 6.5 의 데이터 브라우저 화이트리스트를 재사용하면 된다).

### 9.4 도메인 무결성 — 추가를 권고하는 CHECK 5건

MySQL 8.0.16+ 는 `CHECK` 를 실제로 강제하므로 9.6 에서 아래는 전부 동작한다.
**모두 한 행 안의 컬럼만 참조**하므로 MySQL 의 제약(서브쿼리·타 테이블 참조 불가)에 걸리지 않는다.

| # | 대상 | 제약 | 막는 것 |
|---|---|---|---|
| 1 | `qna` | `CHECK ((answer IS NULL AND answered_at IS NULL) OR (answer IS NOT NULL AND answered_at IS NOT NULL))` | § 2.14 불변식. 하나만 채워 **미답변 판정이 깨지는 것** |
| 2 | `orders` | `CHECK ((order_status='취소' AND canceled_at IS NOT NULL) OR (order_status='정상' AND canceled_at IS NULL))` | 취소인데 취소일시가 없는 행 |
| 3 | `payment` | `CHECK ((pay_status='환불' AND refunded_at IS NOT NULL) OR (pay_status='완료' AND refunded_at IS NULL))` | 환불인데 환불일시가 없는 행 |
| 4 | `delivery_history` | `CHECK (prev_status IS NULL OR prev_status <> new_status)` | 변화 없는 이력 행 (`상품준비 → 상품준비`) |
| 5 | `delivery` | `CHECK (status IN ('결제완료','상품준비') OR (courier IS NOT NULL AND tracking_no IS NOT NULL))` | 운송장 없이 `배송중`/`배송완료` 로 넘어간 행 |

> **1번은 특히 중요하다.** § 2.18 에서 미답변 판정을 `answered_at` 으로 옮긴 대가로 두 컬럼의
> 동기화가 불변식이 됐는데, 이 CHECK 가 그 불변식을 **DB 수준에서 보장**한다.
> (앞서 "DDL 로는 강제할 수 없다" 고 적은 것은 틀렸다 — 같은 행 안의 두 컬럼이므로 행 단위
> CHECK 로 충분하다.)
>
> **2·3·5 번은 `/dev` 강제 변경과 충돌하지 않는다.** § 6.4 가 이미 "상태를 바꾸면서 해당 단계의
> 시각 컬럼도 함께 맞추고, 역방향이면 이후 시각을 NULL 로 비운다" 고 규정하므로, 이 CHECK 들은
> **그 규칙을 지키도록 강제하는 장치**가 된다. 규칙을 빠뜨린 `/dev` 코드는 즉시 에러로 드러난다.

### 9.5 도메인 무결성 — 추가를 권고하는 UNIQUE 2건

| # | 대상 | 제약 | 막는 것 | 한계 |
|---|---|---|---|---|
| 6 | `product_image` | `UNIQUE (product_id, sort_order)` | 같은 순서값이 중복돼 **갤러리 표시 순서가 비결정적**이 되는 것 | — |
| 7 | `category` | `UNIQUE (parent_id, name)` | 같은 부모 밑에 같은 이름의 형제 카테고리 | ⚠️ `parent_id IS NULL` 인 **대분류는 보호되지 않는다.** MySQL UNIQUE 는 NULL 을 중복으로 보지 않는다 |

7번의 한계는 MySQL 의 성질이라 우회가 없다. 대분류 이름 중복은 **관리자 화면에서 검증**한다
(24번 `/admin/categories`).

### 9.6 DB 가 강제할 수 없는 불변식 — 7건

아래는 **CHECK 로 표현 불가능**하다. MySQL 의 `CHECK` 는 서브쿼리와 타 테이블 참조를 허용하지 않고,
여러 행에 걸친 조건도 쓸 수 없다. 전부 **서비스 계층의 책임**이며, 트리거는 도입하지 않는다
(디버깅이 어렵고 § 2 의 "읽기 쉬운 코드" 기조에 어긋난다).

| # | 불변식 | 왜 DB 로 못 막나 | 지키는 곳 |
|---|---|---|---|
| 1 | `orders.total_price` = Σ(`order_item.price` × `quantity`) | 타 테이블 집계 | § 6.1 결제 트랜잭션 |
| 2 | `payment.amount` = `orders.total_price` + `delivery_fee` | 타 테이블 참조 | § 6.1 |
| 3 | 회원별 `address.is_default='Y'` 는 **최대 1건** | 부분 UNIQUE 인덱스가 MySQL 에 없다 | § 2.2 의 2문장 트랜잭션 |
| 4 | 리뷰는 `delivery.status='배송완료'` 인 건에만 | 타 테이블 참조 | § 2.13 · `CRUD.md` § 6-3 |
| 5 | `review.product_id` = `order_item.product_id` | 타 테이블 참조 | § 8 쟁점 4 — `item_id` 로 조회해 채운다 |
| 6 | 배송 상태 단방향 전이 | 이전 상태와 비교 필요 (시간축) | § 6.2 의 `WHERE status = 직전상태` 가드 |
| 7 | ADMIN 은 `role='DEV'` 를 부여할 수 없다 | 권한 판정은 세션 정보 | § 5 — `/admin` 처리 로직에서 거부 |

> 3번은 **설계 선택의 결과**다. `is_default` 를 `address` 에 두는 대신 `member.default_addr_id` FK 로
> 옮기면 DB 가 유일성을 보장한다. 그러나 그러면 "기본 배송지 없음" 상태와 순환 참조
> (`member` → `address` → `member`)가 생긴다. 현재 설계가 더 단순하고, 대가는 트랜잭션 2문장이다.

### 9.7 점검 결론

| 항목 | 상태 |
|---|---|
| 개체 무결성 | 🟢 이상 없음 |
| 참조 무결성 | 🟡 동작은 정상. **`ON UPDATE` 미지정**(9.3-1) 보완 권고 |
| 도메인 무결성 | 🟡 CHECK 5건 · UNIQUE 2건 **추가 권고** (9.4 · 9.5) |
| 애플리케이션 불변식 | 🟢 7건 모두 식별·귀속 완료 (9.6) |

> 불변식이 **7건에서 늘지 않았다**는 점이 § 8 쟁점 2 결정의 성과다. `'품절'` 을 ENUM 에 남기면
> `status='품절' ⟺ stock=0` 을 네 곳(결제·취소·관리자·`/dev`)에서 지켜야 하는 8번째 불변식이
> 생겼을 것이다. 값을 없애서 **지킬 것 자체를 만들지 않았다.**

추가 권고 7건은 전부 **DDL 한 줄짜리**이고 ERD 변경을 요구하지 않는다. DB Schema 트랙이
`schema.sql` 작성 시 반영하면 된다.

---

## 10. 정규화 점검

### 10.1 1NF · 2NF — 전 테이블 충족

- **1NF**: 반복 그룹·다중값 컬럼이 없다. `ENUM` 은 단일값이므로 원자적이다.
  배송 시각 4개(`paid_at`·`ready_at`·`shipped_at`·`delivered_at`)는 **반복 그룹이 아니다** —
  각각 다른 의미의 속성이고, 상태 이력은 별도 테이블(`delivery_history`)로 분리돼 있다.
- **2NF**: 복합 PK 가 `wishlist` 하나뿐이다. 유일한 비키 속성 `created_at` 이
  `(member_id, product_id)` 전체에 의존하므로 부분 함수 종속이 없다. 나머지 16 테이블은
  단일 속성 PK 라 2NF 가 자동 성립한다.

### 10.2 3NF — 16/17 충족, 1건 의도적 위반

#### 위반 1건 — `delivery` 의 `status`

`delivery` 안에 아래 함수 종속이 성립한다.

```
delivery_id → {paid_at, ready_at, shipped_at, delivered_at}
{어느 시각 컬럼이 NOT NULL 인가} → status
```

즉 `delivery_id → 시각들 → status` 의 **이행적 종속**이다. `status` 는 비주요 속성이고
결정자인 시각 컬럼 집합은 슈퍼키가 아니므로 **3NF 위반**이다.
(`status='배송중'` 이면 `shipped_at` 이 차 있고 `delivered_at` 은 비어 있다 — 역도 성립한다.)

**판단: 유지한다.** 세 가지 이유다.

1. 주 접근 경로가 `WHERE status = ?` 다 (25번 관리자 주문관리, 22번 대시보드, § 3.2 뷰).
   4개 nullable 시각에서 상태를 SQL 로 유도하면 `CASE WHEN` 중첩이 되고 인덱스를 못 탄다.
2. `ENUM` 선언 순서가 생애주기 순서라 `ORDER BY status` 가 그대로 쓰인다 (§ 0.1).
3. 상태 전이 가드(`WHERE status = 직전상태`, § 6.2)가 이 컬럼 없이는 성립하지 않는다.

**대가**: 시각과 상태가 어긋날 수 있다 → § 9.4 의 CHECK 5번과 § 6.4 의 `/dev` 규칙이 방어선이다.

#### ~~잠재 위반 — `product.status` 의 `'품절'`~~ → **해소 (2026-10-29)**

ENUM 에 `'품절'` 이 있던 동안은 `stock → status` 함수 종속이 잠재했다
(`status='품절'` ⟺ `stock=0`). 비주요 속성 `stock` 이 비주요 속성 `status` 를 결정하므로
그대로 두면 **3NF 위반이 될 소지**가 있었다.

**`'품절'` 을 ENUM 에서 제거해(§ 8 쟁점 2) 종속 자체가 사라졌다.** 이제 `status` 와 `stock` 은
서로 다른 사실을 담는 독립 속성이고 `product` 는 3NF·BCNF 를 완전히 만족한다.

> 정규화 문제를 제약(CHECK)으로 막지 않고 **값을 없애서** 푼 사례다.
> 모순이 표현 불가능해지면 그것을 막는 코드도 필요 없다.

### 10.3 BCNF — 3NF 와 동일

3NF 를 만족하는 16 테이블은 BCNF 도 만족한다. 후보키가 둘 이상인 테이블
(`member` · `cart` · `payment` · `delivery` · `review` · `orders`)에서 모든 결정자가
후보키이기 때문이다. `delivery` 만 10.2 의 이유로 BCNF 도 위반한다.

### 10.4 중복처럼 보이지만 정규화 위반이 아닌 것 — 7건

**정규화는 한 릴레이션 안의 함수 종속만 다룬다.** 아래는 전부 그 범위 밖이라
3NF/BCNF 판정에 영향을 주지 않는다. 혼동하기 쉬운 지점이라 구분해 적는다.

| # | 대상 | 왜 위반이 아닌가 |
|---|---|---|
| 1 | `order_item.product_name` · `price` | **종속이 성립하지 않는다.** `product_id → product_name` 은 `product` 안에서만 참이고, `order_item` 에서는 **주문 시점에 동결된 값**이라 현재 상품명과 다를 수 있다. 종속이 깨져 있는 것이 설계 목적이다 |
| 2 | `orders.receiver` · `zipcode` · `addr1` · `addr2` | 같은 이유. `address` 의 복사본이 아니라 **주문의 고유 값**이다. 주소록을 수정·삭제해도 변하지 않아야 한다 |
| 3 | `review.product_id` | `item_id → product_id` 가 성립하지만 **`item_id` 가 후보키**(UNIQUE NOT NULL)다. 슈퍼키에 대한 종속은 3NF·BCNF 위반이 아니다. 조인 없이 조회하려는 **의도적 중복**이며, 정합성은 § 9.6-5 가 담당한다 |
| 4 | `payment.amount` | 타 테이블(`orders`)에서 유도된다. 릴레이션 간 중복은 정규화 대상이 아니다 (§ 9.6-2) |
| 5 | `orders.total_price` | `order_item` 집계에서 유도된다. 같음 (§ 9.6-1) |
| 6 | `delivery.paid_at` | `payment.paid_at` 과 같은 값. 릴레이션 간 중복 (§ 8 쟁점 10). 배송 시각 4개를 한 테이블에 모으는 쪽이 타임라인 조회에 유리해 유지한다 |
| 7 | `delivery_history.prev_status` | **이전 행**의 `new_status` 에서 유도된다. 행 간 중복이지 컬럼 간 종속이 아니다. 유도하려면 윈도 함수가 필요한데, 이력 테이블은 append-only 라 값이 변할 일이 없어 저장이 안전하다 |

> 1·2 번이 이 설계의 핵심 패턴이다. **"스냅샷은 비정규화가 아니다"** — 시점이 다른 값은
> 애초에 같은 사실이 아니기 때문이다. 정규화 위반으로 오해해 FK 로 바꾸면
> 과거 주문 내역이 현재 데이터를 따라 변하는 버그가 된다.

### 10.5 구조적으로 짚어둘 것 — 2건

**`category` 의 인접 리스트(adjacency list)**

`parent_id` 자기 참조는 정규화 문제가 아니지만, **임의 깊이 조회가 재귀 쿼리를 요구**한다.
현재 설계는 2단(대분류·소분류) 고정이고 화면도 그 전제다(§ `CRUD.md` 1·4·23·24번).
3단 이상이 필요해지면 MySQL 8+ 의 `WITH RECURSIVE` 를 쓰거나 경로 열거(path enumeration)로
바꿔야 한다. 현 범위에서는 2단 고정이 맞다.

**`system_config` 의 키-값 구조**

`config_key → config_value` 는 3NF 를 만족하지만, **값이 전부 `VARCHAR` 라 타입 안전성이 없다.**
`DELIVERY_FEE='삼천원'` 이 들어가도 DB 는 받는다. 이것은 정규화가 아니라 EAV 패턴의 알려진 대가다.
§ 2.16 에 적은 대로 **읽는 쪽에서 파싱 실패에 대비해 코드 기본값을 둔다.**

### 10.6 점검 결론

| 정규형 | 결과 |
|---|---|
| 1NF | 🟢 17/17 |
| 2NF | 🟢 17/17 |
| 3NF | 🟡 **16/17** — `delivery` 1건 의도적 위반 (10.2) |
| BCNF | 🟡 16/17 — 동일 |

**종합**: 과한 정규화도, 무분별한 비정규화도 없다. 유일한 3NF 위반(`delivery.status`)은
조회 경로·전이 가드·정렬 세 가지 근거가 있고 § 9.4 CHECK 로 방어된다.
중복으로 보이는 7건은 전부 정규화 범위 밖이며 그중 2건(스냅샷)은 **의도적으로 종속을 끊은 것**이다.

`product.status` 의 잠재 위반은 **`'품절'` 을 ENUM 에서 제거해 해소됐다**(§ 8 쟁점 2 · § 10.2).
남은 조치는 없다.
