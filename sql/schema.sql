-- =============================================================
-- 샌드박스 스키마 DDL (zyonge_Sandbox 전용)
--
--   대상 DB : webapplication_sandbox
--   근거    : docs/ERD.md
--               § 2.1 ~ 2.17  테이블 17개
--               § 2.18        인덱스 9개
--               § 2.19        FK 의존 생성 순서 (아래 생성 순서가 이 표다)
--               § 3.1 ~ 3.5   뷰 5개
--   공통    : ENGINE=InnoDB / utf8mb4 / utf8mb4_unicode_ci
--
--   실행    : mysql -D webapplication_sandbox < sql/schema.sql   (재실행 가능)
--   다음    : sql/data.sql
--
--   ※ 이 ERD 는 실험용이다. main 의 설계가 아니며 main 에 반영하지 않는다. (CLAUDE.md § 4.5)
-- =============================================================

USE webapplication_sandbox;

-- 제거 — 생성의 역순 ------------------------------------------------
DROP VIEW  IF EXISTS v_low_stock;
DROP VIEW  IF EXISTS v_admin_member;
DROP VIEW  IF EXISTS v_admin_order;
DROP VIEW  IF EXISTS v_delivery_status_count;
DROP VIEW  IF EXISTS v_sales_daily;

DROP TABLE IF EXISTS admin_log;
DROP TABLE IF EXISTS qna;
DROP TABLE IF EXISTS review;
DROP TABLE IF EXISTS delivery_history;
DROP TABLE IF EXISTS delivery;
DROP TABLE IF EXISTS payment;
DROP TABLE IF EXISTS order_item;
DROP TABLE IF EXISTS orders;
DROP TABLE IF EXISTS wishlist;
DROP TABLE IF EXISTS cart;
DROP TABLE IF EXISTS product_image;
DROP TABLE IF EXISTS address;
DROP TABLE IF EXISTS system_config;
DROP TABLE IF EXISTS notice;
DROP TABLE IF EXISTS product;
DROP TABLE IF EXISTS category;
DROP TABLE IF EXISTS member;


-- =============================================================
-- 1. member — 회원                                      ERD § 2.1
-- =============================================================
--   status 로 소프트 삭제한다. 주문이 FK 로 묶여 물리 삭제가 불가능하다.
CREATE TABLE member (
    member_id  INT UNSIGNED NOT NULL AUTO_INCREMENT,
    login_id   VARCHAR(30)  NOT NULL,
    password   VARCHAR(100) NOT NULL,
    name       VARCHAR(30)  NOT NULL,
    email      VARCHAR(100) NOT NULL,
    phone      VARCHAR(20)  NOT NULL,
    role       ENUM('USER', 'ADMIN', 'DEV') NOT NULL DEFAULT 'USER',
    status     ENUM('ACTIVE', 'LEFT')       NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (member_id),
    UNIQUE KEY uk_member_login_id (login_id),
    UNIQUE KEY uk_member_email (email)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;


-- =============================================================
-- 2. category — 카테고리                                ERD § 2.3
-- =============================================================
--   parent_id 자기 참조로 대분류·소분류를 한 테이블에서 처리한다.
--   NULL 이면 대분류다.
CREATE TABLE category (
    category_id INT UNSIGNED NOT NULL AUTO_INCREMENT,
    parent_id   INT UNSIGNED NULL,
    name        VARCHAR(50)  NOT NULL,
    sort_order  INT          NOT NULL DEFAULT 0,
    PRIMARY KEY (category_id),
    KEY idx_category_parent (parent_id),
    CONSTRAINT fk_category_parent FOREIGN KEY (parent_id)
        REFERENCES category (category_id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    -- ERD § 9.5-7 — 같은 부모 밑 동명 형제를 막는다.
    --   ⚠️ parent_id IS NULL 인 대분류는 보호되지 않는다. MySQL UNIQUE 는 NULL 을 중복으로 보지 않는다.
    --      대분류 이름 중복은 관리자 화면(/admin/categories)에서 검증한다.
    UNIQUE KEY uk_category_parent_name (parent_id, name)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;


-- =============================================================
-- 3. product — 상품                                     ERD § 2.4
-- =============================================================
--   price · stock 이 UNSIGNED 다. 재고 차감에서 WHERE stock >= ? 가드를
--   빼먹으면 음수가 되는 대신 에러가 난다 — 버그가 데이터로 남지 않는다.
--   thumbnail 은 웹 참조 경로 '/upload/<파일명>' 만 넣는다.
--
--   ★ status 에 '품절' 이 없다. status 는 관리자의 전시 의도만 담고 재고를 모른다.
--     재고 상태를 status 에 복사하면 두 컬럼이 같은 사실을 말하게 되고, 동기화를
--     빠뜨린 코드 한 곳이 status='품절' + stock=10 같은 모순 행을 만든다. (ERD § 2.4)
--
--   ★ 노출 규칙 — 상품을 찾아가는 경로의 공통 조건은 다음 한 줄이다.
--         WHERE status = '판매중' AND stock > 0
--     목록 · 검색 · 상품상세(직접 URL 포함)에 적용한다.
--     장바구니 · 찜은 예외다 — 사용자 자기 기록이므로 보여주되 '재고 없음' 표시만 하고
--     주문 대상에서 뺀다. 주문내역은 order_item 스냅샷을 쓰므로 무관하다.
CREATE TABLE product (
    product_id  INT UNSIGNED NOT NULL AUTO_INCREMENT,
    category_id INT UNSIGNED NOT NULL,
    name        VARCHAR(100) NOT NULL,
    price       INT UNSIGNED NOT NULL,
    stock       INT UNSIGNED NOT NULL DEFAULT 0,
    description TEXT         NULL,
    thumbnail   VARCHAR(200) NULL,
    status      ENUM('판매중', '숨김') NOT NULL DEFAULT '판매중',
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (product_id),
    KEY idx_product_category_status (category_id, status),
    KEY idx_product_created (created_at),
    CONSTRAINT fk_product_category FOREIGN KEY (category_id)
        REFERENCES category (category_id) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;


-- =============================================================
-- 4. notice — 공지사항                                 ERD § 2.15
-- =============================================================
CREATE TABLE notice (
    notice_id  INT UNSIGNED NOT NULL AUTO_INCREMENT,
    title      VARCHAR(200) NOT NULL,
    content    TEXT         NOT NULL,
    view_count INT UNSIGNED NOT NULL DEFAULT 0,
    created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (notice_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;


-- =============================================================
-- 5. system_config — 전역 설정                         ERD § 2.16
-- =============================================================
--   배송비·무료배송 기준·점검 모드를 DB에 둔다. /dev/state.jsp 에서
--   실시간으로 바꿔 시연해야 하므로 코드 상수로 두지 않는다.
--   ON UPDATE CURRENT_TIMESTAMP 는 MySQL 고유 기능 — Oracle 이면 트리거가 필요했다.
CREATE TABLE system_config (
    config_key   VARCHAR(50)  NOT NULL,
    config_value VARCHAR(200) NOT NULL,
    description  VARCHAR(200) NULL,
    updated_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
                              ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (config_key)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;


-- =============================================================
-- 6. address — 회원 배송지                              ERD § 2.2
-- =============================================================
--   기본 배송지 변경은 한 트랜잭션에서 두 문장으로 처리한다.
--   두 번째 UPDATE 에 member_id 조건을 반드시 붙인다 — 없으면 남의 주소를
--   기본값으로 만들 수 있다.
CREATE TABLE address (
    addr_id        INT UNSIGNED NOT NULL AUTO_INCREMENT,
    member_id      INT UNSIGNED NOT NULL,
    addr_name      VARCHAR(30)  NULL,
    receiver       VARCHAR(30)  NOT NULL,
    receiver_phone VARCHAR(20)  NOT NULL,
    zipcode        VARCHAR(10)  NOT NULL,
    addr1          VARCHAR(200) NOT NULL,
    addr2          VARCHAR(200) NULL,
    is_default     ENUM('Y', 'N') NOT NULL DEFAULT 'N',
    PRIMARY KEY (addr_id),
    KEY idx_address_member (member_id),
    CONSTRAINT fk_address_member FOREIGN KEY (member_id)
        REFERENCES member (member_id) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;


-- =============================================================
-- 7. product_image — 상품 이미지                        ERD § 2.5
-- =============================================================
CREATE TABLE product_image (
    image_id   INT UNSIGNED NOT NULL AUTO_INCREMENT,
    product_id INT UNSIGNED NOT NULL,
    img_path   VARCHAR(200) NOT NULL,
    sort_order INT          NOT NULL DEFAULT 0,
    PRIMARY KEY (image_id),
    KEY idx_product_image_product (product_id),
    CONSTRAINT fk_product_image_product FOREIGN KEY (product_id)
        REFERENCES product (product_id) ON DELETE CASCADE ON UPDATE RESTRICT,
    -- ERD § 9.5-6 — 순서값이 겹치면 갤러리 표시 순서가 비결정적이 된다.
    UNIQUE KEY uk_product_image_sort (product_id, sort_order)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;


-- =============================================================
-- 8. cart — 장바구니                                    ERD § 2.6
-- =============================================================
--   UNSIGNED 는 0 을 허용하므로 CHECK (quantity > 0) 이 그대로 필요하다.
--   담기는 INSERT ... ON DUPLICATE KEY UPDATE 한 문장으로 끝난다
--   (UNIQUE KEY 가 그걸 가능하게 한다).
CREATE TABLE cart (
    cart_id    INT UNSIGNED NOT NULL AUTO_INCREMENT,
    member_id  INT UNSIGNED NOT NULL,
    product_id INT UNSIGNED NOT NULL,
    quantity   INT UNSIGNED NOT NULL,
    added_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (cart_id),
    UNIQUE KEY uk_cart_member_product (member_id, product_id),
    KEY idx_cart_product (product_id),
    CONSTRAINT fk_cart_member FOREIGN KEY (member_id)
        REFERENCES member (member_id) ON DELETE CASCADE ON UPDATE RESTRICT,
    CONSTRAINT fk_cart_product FOREIGN KEY (product_id)
        REFERENCES product (product_id) ON DELETE CASCADE ON UPDATE RESTRICT,
    CONSTRAINT ck_cart_quantity CHECK (quantity > 0)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;


-- =============================================================
-- 9. wishlist — 찜                                      ERD § 2.7
-- =============================================================
--   대리키 없는 복합 PK. 찜은 줄 단위 수정이 없고 토글만 있어
--   식별용 단일 키가 필요 없다.
CREATE TABLE wishlist (
    member_id  INT UNSIGNED NOT NULL,
    product_id INT UNSIGNED NOT NULL,
    created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (member_id, product_id),
    KEY idx_wishlist_product (product_id),
    CONSTRAINT fk_wishlist_member FOREIGN KEY (member_id)
        REFERENCES member (member_id) ON DELETE CASCADE ON UPDATE RESTRICT,
    CONSTRAINT fk_wishlist_product FOREIGN KEY (product_id)
        REFERENCES product (product_id) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;


-- =============================================================
-- 10. orders — 주문                                     ERD § 2.8
-- =============================================================
--   배송지를 FK 가 아닌 복사본으로 저장한다. 주소록을 수정·삭제해도
--   과거 주문 내역이 바뀌지 않는다.
--   delivery_fee 도 주문 시점 복사본이다 — system_config 가 바뀌어도
--   과거 주문 금액이 변하지 않는다.
--   order_no 는 INSERT 후 order_id 기반으로 같은 트랜잭션에서 채운다 (§ 2.8).
CREATE TABLE orders (
    order_id       INT UNSIGNED NOT NULL AUTO_INCREMENT,
    order_no       VARCHAR(20)  NOT NULL,
    member_id      INT UNSIGNED NOT NULL,
    total_price    INT UNSIGNED NOT NULL,
    delivery_fee   INT UNSIGNED NOT NULL DEFAULT 0,
    order_status   ENUM('정상', '취소') NOT NULL DEFAULT '정상',
    receiver       VARCHAR(30)  NOT NULL,
    receiver_phone VARCHAR(20)  NOT NULL,
    zipcode        VARCHAR(10)  NOT NULL,
    addr1          VARCHAR(200) NOT NULL,
    addr2          VARCHAR(200) NULL,
    delivery_memo  VARCHAR(200) NULL,
    ordered_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    canceled_at    DATETIME     NULL,
    PRIMARY KEY (order_id),
    UNIQUE KEY uk_orders_order_no (order_no),
    KEY idx_orders_member_ordered (member_id, ordered_at),
    KEY idx_orders_ordered (ordered_at),
    CONSTRAINT fk_orders_member FOREIGN KEY (member_id)
        REFERENCES member (member_id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    -- ERD § 9.4-2 — 취소인데 취소일시가 없는 행을 막는다.
    CONSTRAINT chk_orders_canceled CHECK (
        (order_status = '취소' AND canceled_at IS NOT NULL)
     OR (order_status = '정상' AND canceled_at IS NULL))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;


-- =============================================================
-- 11. order_item — 주문 상품                            ERD § 2.9
-- =============================================================
--   product_name · price 는 주문 시점 복사본이다. 상품 정보가 바뀌어도
--   주문 내역은 그대로다.
CREATE TABLE order_item (
    item_id      INT UNSIGNED NOT NULL AUTO_INCREMENT,
    order_id     INT UNSIGNED NOT NULL,
    product_id   INT UNSIGNED NOT NULL,
    product_name VARCHAR(100) NOT NULL,
    price        INT UNSIGNED NOT NULL,
    quantity     INT UNSIGNED NOT NULL,
    PRIMARY KEY (item_id),
    KEY idx_order_item_order (order_id),
    KEY idx_order_item_product (product_id),
    CONSTRAINT fk_order_item_order FOREIGN KEY (order_id)
        REFERENCES orders (order_id) ON DELETE CASCADE ON UPDATE RESTRICT,
    CONSTRAINT fk_order_item_product FOREIGN KEY (product_id)
        REFERENCES product (product_id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT ck_order_item_quantity CHECK (quantity > 0)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;


-- =============================================================
-- 12. payment — 결제 (모의)                            ERD § 2.10
-- =============================================================
--   외부 PG 연동은 범위 외다 (CLAUDE.md § 5). 버튼 선택값 저장 + 상태 전이만 한다.
--   order_id 가 NOT NULL + UNIQUE 다 — UNIQUE 만으로 1:1 을 표현하면
--   order_id IS NULL 인 고아 결제가 여러 건 들어갈 수 있다.
CREATE TABLE payment (
    payment_id  INT UNSIGNED NOT NULL AUTO_INCREMENT,
    order_id    INT UNSIGNED NOT NULL,
    method      ENUM('카드', '무통장', '간편결제') NOT NULL,
    amount      INT UNSIGNED NOT NULL,
    pay_status  ENUM('완료', '환불') NOT NULL DEFAULT '완료',
    paid_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    refunded_at DATETIME     NULL,
    PRIMARY KEY (payment_id),
    UNIQUE KEY uk_payment_order (order_id),
    CONSTRAINT fk_payment_order FOREIGN KEY (order_id)
        REFERENCES orders (order_id) ON DELETE CASCADE ON UPDATE RESTRICT,
    -- ERD § 9.4-3 — 환불인데 환불일시가 없는 행을 막는다.
    CONSTRAINT chk_payment_refunded CHECK (
        (pay_status = '환불' AND refunded_at IS NOT NULL)
     OR (pay_status = '완료' AND refunded_at IS NULL))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;


-- =============================================================
-- 13. delivery — 배송                                  ERD § 2.11
-- =============================================================
--   주문과 1:1. 상태는 결제완료 → 상품준비 → 배송중 → 배송완료 로만 전이한다.
--   단 /dev 는 이 규칙을 무시할 수 있다 (ERD § 6.4).
--   tracking_no 는 임의 입력값이다 — 외부 조회는 하지 않는다.
CREATE TABLE delivery (
    delivery_id  INT UNSIGNED NOT NULL AUTO_INCREMENT,
    order_id     INT UNSIGNED NOT NULL,
    status       ENUM('결제완료', '상품준비', '배송중', '배송완료')
                 NOT NULL DEFAULT '결제완료',
    courier      VARCHAR(30) NULL,
    tracking_no  VARCHAR(30) NULL,
    paid_at      DATETIME    NULL,
    ready_at     DATETIME    NULL,
    shipped_at   DATETIME    NULL,
    delivered_at DATETIME    NULL,
    PRIMARY KEY (delivery_id),
    UNIQUE KEY uk_delivery_order (order_id),
    CONSTRAINT fk_delivery_order FOREIGN KEY (order_id)
        REFERENCES orders (order_id) ON DELETE CASCADE ON UPDATE RESTRICT,
    -- ERD § 9.4-5 — 운송장 없이 배송중/배송완료 로 넘어간 행을 막는다.
    --   /dev 강제 변경과 충돌하지 않는다. § 6.4 가 시각·운송장을 함께 맞추도록 규정하므로,
    --   이 CHECK 는 그 규칙을 빠뜨린 코드를 즉시 드러내는 장치가 된다.
    CONSTRAINT chk_delivery_tracking CHECK (
        status IN ('결제완료', '상품준비')
     OR (courier IS NOT NULL AND tracking_no IS NOT NULL))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;


-- =============================================================
-- 14. delivery_history — 배송 상태 이력                 ERD § 2.12
-- =============================================================
--   상태가 바뀔 때마다 한 줄씩. 마이페이지 배송 타임라인의 유일한 근거다.
--   is_forced = 'Y' 면 /dev 가 강제로 바꾼 줄이다.
--   changed_by 가 NULL 허용이라 ON DELETE SET NULL 이 성립한다.
CREATE TABLE delivery_history (
    history_id  INT UNSIGNED NOT NULL AUTO_INCREMENT,
    delivery_id INT UNSIGNED NOT NULL,
    prev_status ENUM('결제완료', '상품준비', '배송중', '배송완료') NULL,
    new_status  ENUM('결제완료', '상품준비', '배송중', '배송완료') NOT NULL,
    changed_by  INT UNSIGNED NULL,
    is_forced   ENUM('Y', 'N') NOT NULL DEFAULT 'N',
    changed_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (history_id),
    KEY idx_delivery_history_timeline (delivery_id, changed_at),
    KEY idx_delivery_history_member (changed_by),
    CONSTRAINT fk_delivery_history_delivery FOREIGN KEY (delivery_id)
        REFERENCES delivery (delivery_id) ON DELETE CASCADE ON UPDATE RESTRICT,
    CONSTRAINT fk_delivery_history_member FOREIGN KEY (changed_by)
        REFERENCES member (member_id) ON DELETE SET NULL ON UPDATE RESTRICT,
    -- ERD § 9.4-4 — 변화 없는 이력 행(상품준비 -> 상품준비)을 막는다.
    CONSTRAINT chk_delivery_history_changed CHECK (
        prev_status IS NULL OR prev_status <> new_status)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;


-- =============================================================
-- 15. review — 리뷰                                    ERD § 2.13
-- =============================================================
--   item_id 가 NOT NULL + UNIQUE 다 — "구매한 건에만 리뷰 1개" 규칙은
--   NULL 이 허용되면 서지 않는다.
--   product_id 는 item_id → order_item 으로 유도 가능한 중복 컬럼이다.
--   상품 상세에서 조인을 줄이려는 의도적 비정규화다 (ERD § 8 쟁점 4).
--   작성 조건(배송완료)은 서비스 계층에서 검사한다.
CREATE TABLE review (
    review_id  INT UNSIGNED  NOT NULL AUTO_INCREMENT,
    member_id  INT UNSIGNED  NOT NULL,
    product_id INT UNSIGNED  NOT NULL,
    item_id    INT UNSIGNED  NOT NULL,
    rating     TINYINT UNSIGNED NOT NULL,
    content    VARCHAR(1000) NOT NULL,
    created_at DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (review_id),
    UNIQUE KEY uk_review_item (item_id),
    KEY idx_review_product_created (product_id, created_at),
    KEY idx_review_member (member_id),
    CONSTRAINT fk_review_member FOREIGN KEY (member_id)
        REFERENCES member (member_id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_review_product FOREIGN KEY (product_id)
        REFERENCES product (product_id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_review_item FOREIGN KEY (item_id)
        REFERENCES order_item (item_id) ON DELETE CASCADE ON UPDATE RESTRICT,
    CONSTRAINT ck_review_rating CHECK (rating BETWEEN 1 AND 5)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;


-- =============================================================
-- 16. qna — 상품 문의                                  ERD § 2.14
-- =============================================================
--   ★ 불변식: answer 와 answered_at 은 항상 같은 UPDATE 로 함께 쓴다.
--     하나만 채우면 미답변 판정이 깨진다. 답변 수정 시에도 answered_at 을 갱신한다.
--     미답변 조회 조건은 answered_at IS NULL 이다 (answer IS NULL 이 아니다).
--
--   answer 에는 인덱스를 걸 수 없다 — VARCHAR(1000) x utf8mb4 = 4000 byte 로
--   InnoDB 키 길이 한계 3072 byte 를 넘어 ERROR 1071 이 난다.
--   그래서 판정 컬럼을 answered_at (DATETIME, 5 byte) 으로 옮겼다. (ERD § 2.18)
CREATE TABLE qna (
    qna_id      INT UNSIGNED  NOT NULL AUTO_INCREMENT,
    member_id   INT UNSIGNED  NOT NULL,
    product_id  INT UNSIGNED  NOT NULL,
    question    VARCHAR(1000) NOT NULL,
    is_secret   ENUM('Y', 'N') NOT NULL DEFAULT 'N',
    answer      VARCHAR(1000) NULL,
    created_at  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    answered_at DATETIME      NULL,
    PRIMARY KEY (qna_id),
    KEY idx_qna_product_created (product_id, created_at),
    KEY idx_qna_unanswered (answered_at),
    KEY idx_qna_member (member_id),
    CONSTRAINT fk_qna_member FOREIGN KEY (member_id)
        REFERENCES member (member_id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_qna_product FOREIGN KEY (product_id)
        REFERENCES product (product_id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    -- ERD § 9.4-1 · § 2.14 불변식을 DB 수준에서 보장한다.
    --   미답변 판정을 answered_at 으로 옮긴 대가가 두 컬럼의 동기화이고, 이 CHECK 가 그 대가를 치른다.
    CONSTRAINT chk_qna_answered CHECK (
        (answer IS NULL     AND answered_at IS NULL)
     OR (answer IS NOT NULL AND answered_at IS NOT NULL))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;


-- =============================================================
-- 17. admin_log — 관리자·dev 조작 기록                 ERD § 2.17
-- =============================================================
--   delivery_history 와 목적이 다르다 — 저쪽은 "배송이 어떻게 흘렀나",
--   이쪽은 "누가 무엇을 건드렸나".
--   member_id 가 NOT NULL 이다. 조작 로그에 행위자가 없으면 의미가 없다.
CREATE TABLE admin_log (
    log_id       INT UNSIGNED NOT NULL AUTO_INCREMENT,
    member_id    INT UNSIGNED NOT NULL,
    mode         ENUM('ADMIN', 'DEV') NOT NULL,
    action       VARCHAR(50)  NOT NULL,
    target_table VARCHAR(30)  NULL,
    target_id    INT UNSIGNED NULL,
    detail       VARCHAR(500) NULL,
    created_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (log_id),
    KEY idx_admin_log_created (created_at),
    KEY idx_admin_log_member (member_id),
    CONSTRAINT fk_admin_log_member FOREIGN KEY (member_id)
        REFERENCES member (member_id) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;


-- =============================================================
-- 뷰 — 테이블 전체를 만든 뒤 마지막에 생성한다        ERD § 3, § 2.19
--      뷰끼리는 서로 참조하지 않으므로 순서는 자유다.
-- =============================================================

-- 3.1 v_sales_daily — 일별 매출 -------------------------------------
--   order_item 을 조인하지 않는다. 조인하면 주문 1건이 상품 수만큼 늘어나
--   delivery_fee 가 중복 합산된다. 금액은 orders 에서만 집계한다.
CREATE OR REPLACE VIEW v_sales_daily AS
SELECT DATE(ordered_at)                AS sales_date,
       COUNT(*)                        AS order_count,
       SUM(total_price)                AS goods_amount,
       SUM(delivery_fee)               AS delivery_amount,
       SUM(total_price + delivery_fee) AS total_amount
FROM   orders
WHERE  order_status = '정상'
GROUP  BY DATE(ordered_at);


-- 3.2 v_delivery_status_count — 배송 상태별 대기 건수 ----------------
--   건수가 0인 상태는 행이 나오지 않는다. 대시보드가 4단계를 항상 보여줘야 하므로
--   JSP 쪽에서 4개 상태를 0으로 초기화한 뒤 조회 결과를 덮어쓴다.
CREATE OR REPLACE VIEW v_delivery_status_count AS
SELECT d.status AS delivery_status,
       COUNT(*) AS order_count
FROM   delivery d
JOIN   orders   o ON o.order_id = d.order_id
WHERE  o.order_status = '정상'
GROUP  BY d.status;


-- 3.3 v_admin_order — 관리자 주문 목록 -------------------------------
--   payment · delivery 는 주문과 같은 트랜잭션에서 생기므로 실제로는 항상 있다.
--   그래도 LEFT JOIN 인 이유는 /dev 가 데이터를 강제로 비틀 수 있기 때문이다 —
--   반쪽 주문이 생겨도 관리자 화면이 그 행을 감추지 않고 보여줘야 이상을 알아챈다.
--   목록 제목은 first_product_name + (item_count - 1) 로 "티셔츠 외 2건" 형태를 만든다.
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


-- 3.4 v_admin_member — 관리자 회원 목록 ------------------------------
--   qna_waiting 의 판정은 answered_at IS NULL 이다 (§ 2.14 불변식, § 2.18 인덱스).
--   미답변 판정 기준은 문서 전체에서 answered_at 하나로 통일한다.
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


-- 3.5 v_low_stock — 재고 부족 상품 -----------------------------------
--   기준값을 하드코딩하지 않고 system_config.LOW_STOCK_THRESHOLD 에서 읽는다.
--   /dev 에서 조절해 시연할 수 있어야 하므로.
--
--   ★ 이 뷰는 선택 기능이 아니다. § 2.4 노출 규칙에 따라 stock = 0 인 상품은
--     사용자 화면에서 조용히 사라지므로, 관리자가 그 사실을 알 수 있는 유일한 경로다.
--     stock = 0 도 임계값 이하라 함께 잡힌다 — 0 과 1~5 를 구분해 보여주면
--     "이미 사라진 것" 과 "사라지기 직전인 것" 이 나뉜다.
CREATE OR REPLACE VIEW v_low_stock AS
SELECT p.product_id, p.name, p.stock, p.status,
       c.name AS category_name
FROM   product  p
JOIN   category c ON c.category_id = p.category_id
WHERE  p.status = '판매중'
  AND  p.stock <= CAST((SELECT config_value FROM system_config
                         WHERE config_key = 'LOW_STOCK_THRESHOLD') AS UNSIGNED);
