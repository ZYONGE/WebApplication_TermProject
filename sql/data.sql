-- =============================================================
-- 샌드박스 샘플 데이터 (zyonge_Sandbox 전용)
--
--   대상 DB : webapplication_sandbox
--   전제    : sql/schema.sql 적용 후 실행
--   근거    : docs/ERD.md § 2.19 (INSERT 도 FK 의존 순서를 따른다)
--   실행    : mysql -D webapplication_sandbox < sql/data.sql   (재실행 가능)
--
--   모든 값은 화면 확인용 가상 데이터다. 실제 개인정보를 쓰지 않는다.
--   비밀번호는 샌드박스 전용 테스트 값이며 운영에 쓰지 않는다.
--
--   ★ 지켜야 하는 것 (ERD § 2.19 주의)
--     1. category 는 대분류(parent_id IS NULL)를 먼저 넣는다 — 자기 참조 FK
--     2. system_config 초기 4건이 없으면 배송비 계산과 점검 판정이 동작하지 않는다
--     3. DEV 계정은 이 스크립트로만 만들어진다 (/admin 화면에서 부여 불가)
--     4. qna 의 answer 와 answered_at 은 항상 함께 채우거나 함께 NULL (§ 2.14 불변식)
-- =============================================================

-- ★ 접속 문자셋 고정 — 이 줄이 없으면 한글이 깨진다.
--   Homebrew mysql 클라이언트의 기본 character_set_client 는 latin1 이다.
--   그 상태로 UTF-8 파일을 밀어 넣으면 서버가 바이트를 latin1 로 해석해
--   utf8mb4 로 다시 인코딩한다 — 이중 인코딩(mojibake)이 되어 '베' 가 'ë²' 로 저장된다.
--   같은 latin1 접속으로 읽으면 되돌아와 멀쩡해 보이지만, DBeaver 등 UTF-8
--   클라이언트에서는 깨져 보인다. SET NAMES 로 세션 문자셋을 못 박는다.
SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

USE webapplication_sandbox;

-- 재실행 대비 — FK 역순으로 비운다 ---------------------------------
DELETE FROM admin_log;
DELETE FROM qna;
DELETE FROM review;
DELETE FROM delivery_history;
DELETE FROM delivery;
DELETE FROM payment;
DELETE FROM order_item;
DELETE FROM orders;
DELETE FROM wishlist;
DELETE FROM cart;
DELETE FROM product_image;
DELETE FROM address;
DELETE FROM system_config;
DELETE FROM notice;
DELETE FROM product;
DELETE FROM category WHERE parent_id IS NOT NULL;   -- 소분류 먼저
DELETE FROM category WHERE parent_id IS NULL;       -- 그다음 대분류
DELETE FROM member;

ALTER TABLE admin_log        AUTO_INCREMENT = 1;
ALTER TABLE qna              AUTO_INCREMENT = 1;
ALTER TABLE review           AUTO_INCREMENT = 1;
ALTER TABLE delivery_history AUTO_INCREMENT = 1;
ALTER TABLE delivery         AUTO_INCREMENT = 1;
ALTER TABLE payment          AUTO_INCREMENT = 1;
ALTER TABLE order_item       AUTO_INCREMENT = 1;
ALTER TABLE orders           AUTO_INCREMENT = 1;
ALTER TABLE cart             AUTO_INCREMENT = 1;
ALTER TABLE product_image    AUTO_INCREMENT = 1;
ALTER TABLE address          AUTO_INCREMENT = 1;
ALTER TABLE notice           AUTO_INCREMENT = 1;
ALTER TABLE product          AUTO_INCREMENT = 1;
ALTER TABLE category         AUTO_INCREMENT = 1;
ALTER TABLE member           AUTO_INCREMENT = 1;


-- 1. member ---------------------------------------------------------
--   role = 'DEV' 는 여기서만 만들어진다. /admin 은 USER <-> ADMIN 만 토글한다.
--   status = 'LEFT' 는 소프트 삭제 — 주문이 FK 로 묶여 물리 삭제가 안 된다.
INSERT INTO member (member_id, login_id, password, name, email, phone, role, status, created_at) VALUES
    (1, 'admin',  'test1234', '관리자',   'admin@example.com',  '010-0000-0001', 'ADMIN', 'ACTIVE',    '2026-09-01 10:00:00'),
    (2, 'devops', 'test1234', '개발자',   'dev@example.com',    '010-0000-0002', 'DEV',   'ACTIVE',    '2026-09-01 10:05:00'),
    (3, 'hong',   'test1234', '홍길동',   'hong@example.com',   '010-0000-0003', 'USER',  'ACTIVE',    '2026-09-10 14:20:00'),
    (4, 'kim',    'test1234', '김영희',   'kim@example.com',    '010-0000-0004', 'USER',  'ACTIVE',    '2026-09-15 09:30:00'),
    (5, 'leaver', 'test1234', '탈퇴회원', 'leaver@example.com', '010-0000-0005', 'USER',  'LEFT',      '2026-09-20 16:45:00');


-- 2. category -------------------------------------------------------
--   ★ 대분류(parent_id IS NULL) 먼저. 자기 참조 FK 라 역순이면 걸린다.
INSERT INTO category (category_id, parent_id, name, sort_order) VALUES
    (1, NULL, '의류',     1),
    (2, NULL, '신발',     2),
    (3, NULL, '액세서리', 3);

INSERT INTO category (category_id, parent_id, name, sort_order) VALUES
    (4, 1, '상의',     1),
    (5, 1, '하의',     2),
    (6, 2, '스니커즈', 1),
    (7, 3, '모자',     1),
    (8, 3, '벨트',     2);


-- 3. product --------------------------------------------------------
--   상품은 소분류에만 붙인다. status 는 '판매중' / '숨김' 2값이다 (ERD § 2.4).
--
--   'status = 숨김' 과 'stock = 0' 은 다른 것이다 — 합치면 안 된다.
--     숨김   = 관리자 의도.   재고가 들어와도 자동 복귀하지 않는다 (관리자가 되돌린다)
--     재고 0 = 판매의 결과.   재고가 들어오면 자동으로 다시 노출된다
--
--   노출 규칙(WHERE status='판매중' AND stock > 0)의 시연 표본:
--     3번 stock=0 -> 판매중이지만 목록·검색·상세에서 사라진다. 관리자는 v_low_stock 으로만 안다
--     5번 stock=3 -> 사라지기 직전
--     7번 stock=5 -> 임계값(LOW_STOCK_THRESHOLD=5) 경계
--     8번 숨김    -> 관리자가 내린 상품. v_low_stock 에서도 제외된다
--   즉 v_low_stock 에는 3·5·7번이 잡히고, 사용자 화면에서는 3번만 빠진다.
INSERT INTO product (product_id, category_id, name, price, stock, description, thumbnail, status, created_at) VALUES
    (1, 4, '베이식 반팔 티셔츠', 19000, 50, '면 100% 기본 티셔츠.',       '/upload/sample-tee.png',     '판매중', '2026-09-05 11:00:00'),
    (2, 4, '옥스퍼드 셔츠',      39000, 20, '사계절용 옥스퍼드 원단.',    '/upload/sample-shirt.png',   '판매중', '2026-09-06 11:00:00'),
    (3, 4, '레이어드 후드집업',  59000,  0, '기모 안감 후드집업.',        '/upload/sample-hood.png',    '판매중', '2026-09-07 11:00:00'),
    (4, 5, '와이드 데님 팬츠',   49000, 35, '밑단 조절 가능한 데님.',     '/upload/sample-denim.png',   '판매중', '2026-09-08 11:00:00'),
    (5, 5, '코튼 슬랙스',        45000,  3, '구김이 적은 코튼 혼방.',     '/upload/sample-slacks.png',  '판매중', '2026-09-09 11:00:00'),
    (6, 6, '캔버스 스니커즈',    69000, 12, '데일리 캔버스화.',           '/upload/sample-sneaker.png', '판매중', '2026-09-10 11:00:00'),
    (7, 7, '니트 비니',          15000,  5, '겨울용 니트 비니.',          '/upload/sample-beanie.png',  '판매중', '2026-09-11 11:00:00'),
    (8, 8, '가죽 벨트',          29000, 40, '단종 예정 상품.',            '/upload/sample-belt.png',    '숨김',   '2026-09-12 11:00:00');


-- 4. notice ---------------------------------------------------------
INSERT INTO notice (notice_id, title, content, view_count, created_at) VALUES
    (1, '쇼핑몰 오픈 안내',       '샌드박스 쇼핑몰이 문을 열었습니다.',               128, '2026-09-01 09:00:00'),
    (2, '추석 연휴 배송 안내',    '연휴 기간 배송이 지연될 수 있습니다.',              54, '2026-09-25 09:00:00'),
    (3, '무료배송 기준 변경 안내', '5만원 이상 구매 시 배송비가 무료입니다.',           17, '2026-10-15 09:00:00');


-- 5. system_config --------------------------------------------------
--   ★ 이 4건이 없으면 배송비 계산(DELIVERY_FEE/FREE_SHIPPING_MIN),
--     점검 모드 판정(SHOP_STATUS), 대시보드 재고 부족(LOW_STOCK_THRESHOLD)이
--     전부 동작하지 않는다. 값은 전부 문자열이므로 읽는 쪽에서 변환한다.
INSERT INTO system_config (config_key, config_value, description) VALUES
    ('SHOP_STATUS',         'OPEN',  'OPEN / MAINTENANCE — 점검 모드'),
    ('DELIVERY_FEE',        '3000',  '기본 배송비 (원)'),
    ('FREE_SHIPPING_MIN',   '50000', '무료배송 기준 금액 (원)'),
    ('LOW_STOCK_THRESHOLD', '5',     '대시보드 재고 부족 기준 수량');


-- 6. address --------------------------------------------------------
--   회원당 is_default = 'Y' 는 최대 하나다. 주문서에 자동 입력되는 주소.
INSERT INTO address (addr_id, member_id, addr_name, receiver, receiver_phone, zipcode, addr1, addr2, is_default) VALUES
    (1, 3, '집',   '홍길동', '010-0000-0003', '06236', '서울특별시 강남구 테헤란로 22', '101동 1001호', 'Y'),
    (2, 3, '회사', '홍길동', '010-0000-0003', '04512', '서울특별시 중구 세종대로 110',  '7층',          'N'),
    (3, 4, '집',   '김영희', '010-0000-0004', '48058', '부산광역시 해운대구 센텀로 33', '202동 303호',  'Y');


-- 7. product_image --------------------------------------------------
--   DB 에는 웹 참조 경로만 넣는다. 물리 경로를 넣으면 배포 위치가 바뀔 때 전부 깨진다.
INSERT INTO product_image (image_id, product_id, img_path, sort_order) VALUES
    (1, 1, '/upload/sample-tee-1.png',     1),
    (2, 1, '/upload/sample-tee-2.png',     2),
    (3, 4, '/upload/sample-denim-1.png',   1),
    (4, 6, '/upload/sample-sneaker-1.png', 1),
    (5, 6, '/upload/sample-sneaker-2.png', 2);


-- 8. cart -----------------------------------------------------------
INSERT INTO cart (cart_id, member_id, product_id, quantity, added_at) VALUES
    (1, 3, 2, 1, '2026-10-27 20:11:00'),
    (2, 3, 5, 2, '2026-10-27 20:13:00'),
    (3, 4, 1, 3, '2026-10-28 09:02:00');


-- 9. wishlist -------------------------------------------------------
INSERT INTO wishlist (member_id, product_id, created_at) VALUES
    (3, 6, '2026-10-18 21:40:00'),
    (3, 3, '2026-10-19 10:05:00'),
    (4, 4, '2026-10-22 13:27:00');


-- 10. orders --------------------------------------------------------
--   delivery_fee = (total_price >= FREE_SHIPPING_MIN) ? 0 : DELIVERY_FEE
--     1번 103000 >= 50000 -> 0
--     2번  49000 <  50000 -> 3000
--     3번  15000 <  50000 -> 3000
--     4번  84000 >= 50000 -> 0
--   배송지는 복사본이다. 주소록을 고쳐도 과거 주문은 바뀌지 않는다.
INSERT INTO orders (order_id, order_no, member_id, total_price, delivery_fee, order_status,
                    receiver, receiver_phone, zipcode, addr1, addr2, delivery_memo,
                    ordered_at, canceled_at) VALUES
    (1, '20261020-0001', 3, 103000,    0, '정상', '홍길동', '010-0000-0003', '06236', '서울특별시 강남구 테헤란로 22', '101동 1001호', '부재 시 경비실',   '2026-10-20 14:03:00', NULL),
    (2, '20261026-0002', 4,  49000, 3000, '정상', '김영희', '010-0000-0004', '48058', '부산광역시 해운대구 센텀로 33', '202동 303호',  NULL,               '2026-10-26 09:41:00', NULL),
    (3, '20261028-0003', 3,  15000, 3000, '취소', '홍길동', '010-0000-0003', '04512', '서울특별시 중구 세종대로 110',  '7층',          NULL,               '2026-10-28 18:12:00', '2026-10-28 19:30:00'),
    (4, '20261028-0004', 4,  84000,    0, '정상', '김영희', '010-0000-0004', '48058', '부산광역시 해운대구 센텀로 33', '202동 303호',  '빠른 배송 부탁해요', '2026-10-28 21:05:00', NULL);


-- 11. order_item ----------------------------------------------------
--   product_name · price 는 주문 시점 복사본이다.
--   1번 주문의 티셔츠는 17000 으로 담겨 있고 현재가는 19000 이다 —
--   가격이 바뀌어도 과거 주문이 보존되는지 확인하는 표본이다.
INSERT INTO order_item (item_id, order_id, product_id, product_name, price, quantity) VALUES
    (1, 1, 1, '베이식 반팔 티셔츠', 17000, 2),
    (2, 1, 6, '캔버스 스니커즈',    69000, 1),
    (3, 2, 4, '와이드 데님 팬츠',   49000, 1),
    (4, 3, 7, '니트 비니',          15000, 1),
    (5, 4, 2, '옥스퍼드 셔츠',      39000, 1),
    (6, 4, 5, '코튼 슬랙스',        45000, 1);


-- 12. payment -------------------------------------------------------
--   amount = total_price + delivery_fee. 외부 PG 연동 없이 선택값만 저장한다.
INSERT INTO payment (payment_id, order_id, method, amount, pay_status, paid_at, refunded_at) VALUES
    (1, 1, '카드',     103000, '완료', '2026-10-20 14:03:10', NULL),
    (2, 2, '간편결제',  52000, '완료', '2026-10-26 09:41:08', NULL),
    (3, 3, '무통장',    18000, '환불', '2026-10-28 18:12:05', '2026-10-28 19:30:00'),
    (4, 4, '카드',      84000, '완료', '2026-10-28 21:05:12', NULL);


-- 13. delivery ------------------------------------------------------
--   3번은 주문이 취소됐는데 배송 상태가 '결제완료' 로 남아 있다.
--   ERD § 8 쟁점 1 — 화면에서는 orders.order_status 를 먼저 읽어 '취소' 로 표시한다.
INSERT INTO delivery (delivery_id, order_id, status, courier, tracking_no,
                      paid_at, ready_at, shipped_at, delivered_at) VALUES
    (1, 1, '배송완료', '샌드박스택배', '1234567890', '2026-10-20 14:03:10', '2026-10-21 10:00:00', '2026-10-22 08:30:00', '2026-10-23 15:20:00'),
    (2, 2, '배송중',   '샌드박스택배', '9876543210', '2026-10-26 09:41:08', '2026-10-27 10:00:00', '2026-10-28 08:10:00', NULL),
    (3, 3, '결제완료', NULL,           NULL,         '2026-10-28 18:12:05', NULL,                  NULL,                 NULL),
    (4, 4, '결제완료', NULL,           NULL,         '2026-10-28 21:05:12', NULL,                  NULL,                 NULL);


-- 14. delivery_history ----------------------------------------------
--   상태가 바뀔 때마다 한 줄. 마이페이지 배송 타임라인의 유일한 근거다.
--   최초 생성은 prev_status 가 NULL 이고 changed_by 도 NULL 이다 (시스템 자동).
--   is_forced = 'Y' 는 /dev 가 강제로 바꾼 줄이다 — 2번 주문에 한 건 있다.
INSERT INTO delivery_history (history_id, delivery_id, prev_status, new_status, changed_by, is_forced, changed_at) VALUES
    (1, 1, NULL,       '결제완료', NULL, 'N', '2026-10-20 14:03:10'),
    (2, 1, '결제완료', '상품준비', 1,    'N', '2026-10-21 10:00:00'),
    (3, 1, '상품준비', '배송중',   1,    'N', '2026-10-22 08:30:00'),
    (4, 1, '배송중',   '배송완료', 1,    'N', '2026-10-23 15:20:00'),
    (5, 2, NULL,       '결제완료', NULL, 'N', '2026-10-26 09:41:08'),
    (6, 2, '결제완료', '상품준비', 1,    'N', '2026-10-27 10:00:00'),
    (7, 2, '상품준비', '배송중',   2,    'Y', '2026-10-28 08:10:00'),
    (8, 3, NULL,       '결제완료', NULL, 'N', '2026-10-28 18:12:05'),
    (9, 4, NULL,       '결제완료', NULL, 'N', '2026-10-28 21:05:12');


-- 15. review --------------------------------------------------------
--   배송완료(1번 주문)의 order_item 에만 달려 있다. item_id 는 UNIQUE —
--   구매 1건당 리뷰 1개.
INSERT INTO review (review_id, member_id, product_id, item_id, rating, content, created_at) VALUES
    (1, 3, 1, 1, 5, '기본템으로 좋아요. 두 장 샀는데 둘 다 만족합니다.', '2026-10-24 19:10:00'),
    (2, 3, 6, 2, 4, '편한데 생각보다 조금 크게 나왔어요. 반 사이즈 작게 추천.', '2026-10-24 19:15:00');


-- 16. qna -----------------------------------------------------------
--   ★ 불변식 — answer 와 answered_at 은 함께 채우거나 함께 NULL.
--     1번은 답변 완료(둘 다 채움), 2·3번은 미답변(둘 다 NULL).
--     미답변 조회는 answered_at IS NULL 로 한다.
INSERT INTO qna (qna_id, member_id, product_id, question, is_secret, answer, created_at, answered_at) VALUES
    (1, 3, 1, '세탁기에 돌려도 되나요?',       'N', '찬물 단독 세탁을 권장합니다.', '2026-10-21 11:00:00', '2026-10-21 15:30:00'),
    (2, 4, 4, '허리 밴딩인가요?',             'N', NULL,                          '2026-10-27 13:20:00', NULL),
    (3, 3, 6, '재입고 예정이 궁금합니다.',     'Y', NULL,                          '2026-10-28 10:05:00', NULL);


-- 17. admin_log -----------------------------------------------------
--   delivery_history 와 목적이 다르다 — 저쪽은 "배송이 어떻게 흘렀나",
--   이쪽은 "누가 무엇을 건드렸나". action 어휘는 ERD § 2.17 고정 목록을 쓴다.
--   ★ 역할 격리(ERD § 5, § 9.6-8): mode 는 member 의 role 과 같아야 하고 USER 는 기록하지 않는다.
--     고객 본인 취소는 admin_log 에 남지 않는다. 6번은 관리자가 취소한 건이다.
INSERT INTO admin_log (log_id, member_id, mode, action, target_table, target_id, detail, created_at) VALUES
    (1, 1, 'ADMIN', 'PRODUCT_CREATE',        'product',       8, '가죽 벨트 등록',                      '2026-09-12 11:00:00'),
    (2, 1, 'ADMIN', 'DELIVERY_TRACKING_SET', 'delivery',      1, '운송장 1234567890 (샌드박스택배)',     '2026-10-22 08:30:00'),
    (3, 1, 'ADMIN', 'DELIVERY_NEXT_STEP',    'delivery',      1, '배송중 -> 배송완료',                   '2026-10-23 15:20:00'),
    (4, 1, 'ADMIN', 'QNA_ANSWER',            'qna',           1, '세탁 방법 문의 답변',                  '2026-10-21 15:30:00'),
    (5, 2, 'DEV',   'FORCE_DELIVERY_STATUS', 'delivery',      2, '상품준비 -> 배송중 (강제)',            '2026-10-28 08:10:00'),
    (6, 1, 'ADMIN', 'ORDER_CANCEL',          'orders',        3, '관리자 취소 처리, 환불 완료',         '2026-10-28 19:30:00'),
    (7, 2, 'DEV',   'SHOP_STATUS_TOGGLE',    'system_config', NULL, 'MAINTENANCE -> OPEN',               '2026-10-28 22:00:00'),
    (8, 1, 'ADMIN', 'PRODUCT_UPDATE',        'product',       1, '판매가 17000 -> 19000',                '2026-10-25 10:00:00');
