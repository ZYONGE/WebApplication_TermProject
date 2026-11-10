-- =============================================================
-- 샌드박스 샘플 데이터 (sandbox 전용)
--
--   대상 DB : webapplication_sandbox
--   전제    : sql/schema.sql 적용 후 실행
--   근거    : docs/ERD.md § 4.4 (INSERT 도 FK 의존 순서를 따른다)
--   컨셉    : 커스텀 영양제 (docs/CONCEPT.md). 1포 = 하루 섭취분, 수량 = 섭취 일수
--
--   ⚠️ 영양제 · 금지 조합 값은 시연용 샘플이다. 일반 정보이며 의학적 조언이 아니다.
--   실행    : mysql -D webapplication_sandbox < sql/data.sql   (재실행 가능)
--
--   모든 값은 화면 확인용 가상 데이터다. 실제 개인정보를 쓰지 않는다.
--   비밀번호는 샌드박스 전용 테스트 값이며 운영에 쓰지 않는다.
--
--   ★ 지켜야 하는 것 (ERD § 4.4 · § 8)
--     1. category 는 대분류(parent_id IS NULL)를 먼저 넣는다 — 자기 참조 FK
--     2. system_config 초기 5건이 없으면 배송비 계산 · 점검 판정 · 일수 상한이 동작하지 않는다
--     3. ADMIN · DEV 계정은 이 스크립트로만 만들어진다 (가입은 항상 USER)
--     4. qna 의 answer 와 answered_at 은 항상 함께 채우거나 함께 NULL
--     5. track_item 의 트랙은 TRACK, 구성은 SINGLE. supplement_conflict · combo_item 은 SINGLE 만
--     6. supplement_conflict 는 product_a < product_b. 트랙 구성품끼리는 금지 조합이 없다
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
DELETE FROM combo_item;
DELETE FROM combo;
DELETE FROM wishlist;
DELETE FROM cart;
DELETE FROM product_image;
DELETE FROM address;
DELETE FROM system_config;
DELETE FROM notice;
DELETE FROM supplement_conflict;
DELETE FROM track_item;
DELETE FROM product;
DELETE FROM category WHERE parent_id IS NOT NULL;   -- 소분류 먼저
DELETE FROM category WHERE parent_id IS NULL;       -- 그다음 대분류
DELETE FROM member;

ALTER TABLE admin_log           AUTO_INCREMENT = 1;
ALTER TABLE qna                 AUTO_INCREMENT = 1;
ALTER TABLE review              AUTO_INCREMENT = 1;
ALTER TABLE delivery_history    AUTO_INCREMENT = 1;
ALTER TABLE delivery            AUTO_INCREMENT = 1;
ALTER TABLE payment             AUTO_INCREMENT = 1;
ALTER TABLE order_item          AUTO_INCREMENT = 1;
ALTER TABLE orders              AUTO_INCREMENT = 1;
ALTER TABLE combo               AUTO_INCREMENT = 1;
ALTER TABLE cart                AUTO_INCREMENT = 1;
ALTER TABLE product_image       AUTO_INCREMENT = 1;
ALTER TABLE address             AUTO_INCREMENT = 1;
ALTER TABLE notice              AUTO_INCREMENT = 1;
ALTER TABLE supplement_conflict AUTO_INCREMENT = 1;
ALTER TABLE product             AUTO_INCREMENT = 1;
ALTER TABLE category            AUTO_INCREMENT = 1;
ALTER TABLE member              AUTO_INCREMENT = 1;


-- 1. member ---------------------------------------------------------
--   role = 'ADMIN' · 'DEV' 는 여기서만 만들어진다. 가입은 항상 USER 이고 역할은 바뀌지 않는다.
--   status = 'LEFT' 는 소프트 삭제 — 주문이 FK 로 묶여 물리 삭제가 안 된다.
INSERT INTO member (member_id, login_id, password, name, email, phone, role, status, created_at) VALUES
    (1, 'admin',  'test1234', '관리자',   'admin@example.com',  '010-0000-0001', 'ADMIN', 'ACTIVE',    '2026-09-01 10:00:00'),
    (2, 'devops', 'test1234', '개발자',   'dev@example.com',    '010-0000-0002', 'DEV',   'ACTIVE',    '2026-09-01 10:05:00'),
    (3, 'hong',   'test1234', '홍길동',   'hong@example.com',   '010-0000-0003', 'USER',  'ACTIVE',    '2026-09-10 14:20:00'),
    (4, 'kim',    'test1234', '김영희',   'kim@example.com',    '010-0000-0004', 'USER',  'ACTIVE',    '2026-09-15 09:30:00'),
    (5, 'leaver', 'test1234', '탈퇴회원', 'leaver@example.com', '010-0000-0005', 'USER',  'LEFT',      '2026-09-20 16:45:00');


-- 2. category -------------------------------------------------------
--   ★ 대분류(parent_id IS NULL) 먼저. 자기 참조 FK 라 역순이면 걸린다.
--   대분류는 낱개 / 트랙 두 갈래다. 낱개 상품은 1번 밑, 트랙 상품은 2번 밑에만 붙인다 (ERD § 8-12).
INSERT INTO category (category_id, parent_id, name, sort_order) VALUES
    (1, NULL, '낱개 영양제', 1),
    (2, NULL, '트랙 패키지', 2);

INSERT INTO category (category_id, parent_id, name, sort_order) VALUES
    (3, 1, '비타민',      1),
    (4, 1, '미네랄',      2),
    (5, 1, '기능성 원료', 3),
    (6, 2, '데일리 케어', 1),
    (7, 2, '목적별 케어', 2);


-- 3. product --------------------------------------------------------
--   price = 1포 가격, stock = 남은 포 수. 낱개(1~12)를 먼저, 트랙(13~16)을 나중에 넣는다.
--   트랙 가격은 구성 낱개 1포 가격 합보다 조금 싸다 (13번: 300+250+450+320 = 1320 -> 1200).
--
--   'status = 숨김' 과 'stock = 0' 은 다른 것이다 — 합치면 안 된다.
--     숨김   = 관리자 의도.   재고가 들어와도 자동 복귀하지 않는다 (관리자가 되돌린다)
--     재고 0 = 판매의 결과.   재고가 들어오면 자동으로 다시 노출된다
--
--   노출 규칙(WHERE status='판매중' AND stock > 0)의 시연 표본 — LOW_STOCK_THRESHOLD = 30포:
--      9번 유산균     stock=0  -> 판매중이지만 목록 · 상세 · 조합 탭에서 사라진다. 관리자는 v_low_stock 으로만 안다
--      6번 아연       stock=25 -> 낱개 재고 부족
--     15번 뼈 건강    stock=20 -> 트랙 재고 부족 (트랙은 자기 stock 만 쓴다)
--     12번 코엔자임Q10 숨김    -> 관리자가 내린 상품. v_low_stock 에서도 제외된다
--   즉 v_low_stock 에는 6 · 9 · 15번이 잡히고, 사용자 화면에서는 9 · 12번이 빠진다.
INSERT INTO product (product_id, category_id, product_type, name, price, stock, description, thumbnail, status, created_at) VALUES
    ( 1, 3, 'SINGLE', '비타민C 1000mg',     300, 2000, '항산화 · 면역 관리. 식후 1포.',                   '/upload/sample-vitc.png',        '판매중', '2026-09-05 11:00:00'),
    ( 2, 3, 'SINGLE', '비타민D 2000IU',     250, 1500, '뼈 건강 · 칼슘 흡수. 식후 1포.',                  '/upload/sample-vitd.png',        '판매중', '2026-09-05 11:10:00'),
    ( 3, 3, 'SINGLE', '비타민B 컴플렉스',   350,  800, 'B1 · B2 · B6 · B12 복합. 아침 식후 1포.',         '/upload/sample-vitb.png',        '판매중', '2026-09-05 11:20:00'),
    ( 4, 4, 'SINGLE', '칼슘 500mg',         280, 1200, '뼈 · 치아 건강. 철분 · 아연과 시간 간격을 두세요.', '/upload/sample-calcium.png',     '판매중', '2026-09-06 11:00:00'),
    ( 5, 4, 'SINGLE', '마그네슘 300mg',     320,  900, '근육 · 신경 기능. 저녁 식후 1포.',                '/upload/sample-magnesium.png',   '판매중', '2026-09-06 11:10:00'),
    ( 6, 4, 'SINGLE', '아연 15mg',          200,   25, '면역 기능. 철분 · 칼슘과 시간 간격을 두세요.',     '/upload/sample-zinc.png',        '판매중', '2026-09-06 11:20:00'),
    ( 7, 4, 'SINGLE', '철분 18mg',          220,  600, '혈액 생성. 비타민C 와 함께 드시면 좋습니다.',       '/upload/sample-iron.png',        '판매중', '2026-09-06 11:30:00'),
    ( 8, 5, 'SINGLE', '오메가3 1000mg',     450, 1000, '혈행 개선. 식후 1포.',                            '/upload/sample-omega3.png',      '판매중', '2026-09-07 11:00:00'),
    ( 9, 5, 'SINGLE', '유산균 100억 CFU',   500,    0, '장 건강. 공복 1포. 재입고 준비 중.',               '/upload/sample-probiotics.png',  '판매중', '2026-09-07 11:10:00'),
    (10, 5, 'SINGLE', '루테인 20mg',        400,  700, '눈 건강. 식후 1포.',                              '/upload/sample-lutein.png',      '판매중', '2026-09-07 11:20:00'),
    (11, 5, 'SINGLE', '녹차추출물 카테킨',  380,  500, '체지방 관리. 철분과 함께 드시지 마세요.',          '/upload/sample-catechin.png',    '판매중', '2026-09-07 11:30:00'),
    (12, 5, 'SINGLE', '코엔자임Q10 100mg',  600,  300, '항산화. 단종 예정 상품.',                         '/upload/sample-coq10.png',       '숨김',   '2026-09-12 11:00:00'),
    (13, 6, 'TRACK',  '데일리 기본 트랙',  1200,  300, '하루 기본 4종을 1포에. 처음 시작하는 분께.',       '/upload/sample-track-daily.png', '판매중', '2026-09-15 11:00:00'),
    (14, 7, 'TRACK',  '피로회복 트랙',     1050,  200, '비타민B · C 와 미네랄로 활력 관리.',               '/upload/sample-track-energy.png','판매중', '2026-09-15 11:10:00'),
    (15, 7, 'TRACK',  '뼈 건강 트랙',       790,   20, '칼슘 · 비타민D · 마그네슘 3종.',                   '/upload/sample-track-bone.png',  '판매중', '2026-09-15 11:20:00'),
    (16, 7, 'TRACK',  '여성 활력 트랙',     800,  150, '철분에 흡수를 돕는 비타민C 를 더했습니다.',        '/upload/sample-track-women.png', '판매중', '2026-09-15 11:30:00');


-- 3-1. track_item ---------------------------------------------------
--   트랙 1포 = 구성 낱개 1회분씩. track_id 는 TRACK(13~16), item_id 는 SINGLE(1~12) 만.
--   ★ 트랙 구성품끼리는 아래 3-2 의 금지 조합이 없다 (ERD § 8-14).
--     16번 여성 활력 트랙에 철분이 들어 있다 — 이 트랙과 칼슘 · 아연 · 녹차추출물을 함께 담으면
--     트랙을 구성품으로 펼쳐서 경고가 뜬다.
INSERT INTO track_item (track_id, item_id, sort_order) VALUES
    (13,  1, 1), (13,  2, 2), (13,  8, 3), (13,  5, 4),   -- 데일리 기본: 비타민C · 비타민D · 오메가3 · 마그네슘
    (14,  3, 1), (14,  1, 2), (14,  6, 3), (14,  5, 4),   -- 피로회복:   비타민B · 비타민C · 아연 · 마그네슘
    (15,  4, 1), (15,  2, 2), (15,  5, 3),                -- 뼈 건강:    칼슘 · 비타민D · 마그네슘
    (16,  7, 1), (16,  1, 2), (16,  3, 3);                -- 여성 활력:  철분 · 비타민C · 비타민B


-- 3-2. supplement_conflict ------------------------------------------
--   ★ product_a < product_b 로 정렬해 넣는다 (CHECK). 두 상품 모두 SINGLE.
--   문구는 시연용 일반 정보다. 화면은 "의학적 조언이 아닙니다" 고지를 함께 띄운다.
INSERT INTO supplement_conflict (conflict_id, product_a, product_b, message, created_at) VALUES
    (1, 4,  7, '칼슘은 철분 흡수를 방해할 수 있습니다. 2시간 이상 간격을 두고 드세요.',               '2026-09-16 10:00:00'),
    (2, 6,  7, '아연과 철분은 흡수 경로가 겹쳐 서로 흡수를 떨어뜨릴 수 있습니다. 시간 간격을 두세요.', '2026-09-16 10:01:00'),
    (3, 7, 11, '녹차 카테킨은 철분 흡수를 방해할 수 있습니다. 함께 드시지 않는 것을 권합니다.',        '2026-09-16 10:02:00'),
    (4, 4,  6, '고용량 칼슘은 아연 흡수를 낮출 수 있습니다. 시간 간격을 두고 드세요.',                 '2026-09-16 10:03:00');


-- 4. notice ---------------------------------------------------------
INSERT INTO notice (notice_id, title, content, view_count, created_at) VALUES
    (1, '커스텀 영양제 오픈 안내', '필요한 영양소만 골라 1포 단위로 담아 보세요. 조합 맞추기에서 나만의 조합을 저장할 수 있습니다.', 128, '2026-09-01 09:00:00'),
    (2, '추석 연휴 배송 안내',     '연휴 기간 배송이 지연될 수 있습니다.',                                                            54, '2026-09-25 09:00:00'),
    (3, '무료배송 기준 안내',      '5만원 이상 구매 시 배송비가 무료입니다.',                                                         17, '2026-10-15 09:00:00'),
    (4, '동시 섭취 경고 안내',     '함께 드시기를 권하지 않는 조합을 고르면 경고 문구가 표시됩니다. 일반 정보이며 의학적 조언이 아닙니다. 복용 중인 약이 있다면 전문가와 상담하세요.', 9, '2026-10-20 09:00:00');


-- 5. system_config --------------------------------------------------
--   ★ 이 5건이 없으면 배송비 계산(DELIVERY_FEE/FREE_SHIPPING_MIN),
--     점검 모드 판정(SHOP_STATUS), 대시보드 재고 부족(LOW_STOCK_THRESHOLD),
--     섭취 일수 상한(MAX_INTAKE_DAYS)이 동작하지 않는다. 값은 전부 문자열이므로 읽는 쪽에서 변환한다.
--   LOW_STOCK_THRESHOLD 는 포 단위다 — 30포 = 한 사람 30일분.
INSERT INTO system_config (config_key, config_value, description) VALUES
    ('SHOP_STATUS',         'OPEN',  'OPEN / MAINTENANCE — 점검 모드'),
    ('DELIVERY_FEE',        '3000',  '기본 배송비 (원)'),
    ('FREE_SHIPPING_MIN',   '50000', '무료배송 기준 금액 (원)'),
    ('LOW_STOCK_THRESHOLD', '30',    '대시보드 재고 부족 기준 (포)'),
    ('MAX_INTAKE_DAYS',     '90',    '한 줄에 담을 수 있는 섭취 일수 상한 (일)');


-- 6. address --------------------------------------------------------
--   회원당 is_default = 'Y' 는 최대 하나다. 주문서에 자동 입력되는 주소.
INSERT INTO address (addr_id, member_id, addr_name, receiver, receiver_phone, zipcode, addr1, addr2, is_default) VALUES
    (1, 3, '집',   '홍길동', '010-0000-0003', '06236', '서울특별시 강남구 테헤란로 22', '101동 1001호', 'Y'),
    (2, 3, '회사', '홍길동', '010-0000-0003', '04512', '서울특별시 중구 세종대로 110',  '7층',          'N'),
    (3, 4, '집',   '김영희', '010-0000-0004', '48058', '부산광역시 해운대구 센텀로 33', '202동 303호',  'Y');


-- 7. product_image --------------------------------------------------
--   DB 에는 웹 참조 경로만 넣는다. 물리 경로를 넣으면 배포 위치가 바뀔 때 전부 깨진다.
INSERT INTO product_image (image_id, product_id, img_path, sort_order) VALUES
    (1,  1, '/upload/sample-vitc-1.png',        1),
    (2,  1, '/upload/sample-vitc-2.png',        2),
    (3, 13, '/upload/sample-track-daily-1.png', 1),
    (4,  8, '/upload/sample-omega3-1.png',      1),
    (5,  8, '/upload/sample-omega3-2.png',      2);


-- 8. cart -----------------------------------------------------------
--   quantity = 섭취 일수(= 포 수). 경고 시연 표본:
--     홍길동(3) — 13번 데일리 기본 트랙 + 1번 비타민C -> 비타민C 가 트랙에도 있어 "중복 섭취" 안내
--                 11번 녹차추출물만 14일 -> 일수가 서로 다른 줄 안내
--     김영희(4) — 16번 여성 활력 트랙(철분 포함) + 4번 칼슘 -> 트랙을 펼쳐서 칼슘 × 철분 경고
INSERT INTO cart (cart_id, member_id, product_id, quantity, added_at) VALUES
    (1, 3, 13, 30, '2026-10-27 20:11:00'),
    (2, 3,  1, 30, '2026-10-27 20:13:00'),
    (3, 3, 11, 14, '2026-10-27 20:15:00'),
    (4, 4, 16, 30, '2026-10-28 09:02:00'),
    (5, 4,  4, 30, '2026-10-28 09:04:00');


-- 9. wishlist -------------------------------------------------------
--   9번 유산균은 재고 0 — 찜 목록에는 남고 "재고 없음" 으로 보인다.
INSERT INTO wishlist (member_id, product_id, created_at) VALUES
    (3,  8, '2026-10-18 21:40:00'),
    (3,  9, '2026-10-19 10:05:00'),
    (4, 14, '2026-10-22 13:27:00');


-- 9-1. combo --------------------------------------------------------
--   조합 탭에서 이름을 붙여 저장한 조합. 가격은 저장하지 않고 불러올 때 다시 계산한다.
--   UNIQUE (member_id, name) — 같은 회원 안에서만 이름이 겹치지 않으면 된다.
--     1번 '출근 전 루틴' — 경고 없는 조합. 저장 후 한 번 덮어써서 updated_at 이 다르다
--     2번 '운동 후 회복' — 칼슘 × 아연 경고가 뜨는 조합 (낱개끼리 직접 부딪힘)
--     3번 'JY package'   — 9번 유산균(재고 0)이 들어 있다. 불러오면 "재고 없음" 표시, 담기에서 빠진다
INSERT INTO combo (combo_id, member_id, name, days, created_at, updated_at) VALUES
    (1, 3, '출근 전 루틴', 30, '2026-10-25 08:10:00', '2026-10-26 07:50:00'),
    (2, 3, '운동 후 회복', 14, '2026-10-26 21:30:00', '2026-10-26 21:30:00'),
    (3, 4, 'JY package',   60, '2026-10-27 12:00:00', '2026-10-27 12:00:00');


-- 9-2. combo_item ---------------------------------------------------
--   SINGLE 만 넣는다 (ERD § 8-11).
INSERT INTO combo_item (combo_id, product_id, sort_order) VALUES
    (1,  1, 1), (1,  2, 2), (1,  8, 3), (1, 10, 4),   -- 비타민C · 비타민D · 오메가3 · 루테인
    (2,  5, 1), (2,  4, 2), (2,  6, 3),               -- 마그네슘 · 칼슘 · 아연
    (3,  2, 1), (3,  7, 2), (3,  1, 3), (3,  9, 4);   -- 비타민D · 철분 · 비타민C · 유산균


-- 10. orders --------------------------------------------------------
--   delivery_fee = (total_price >= FREE_SHIPPING_MIN) ? 0 : DELIVERY_FEE
--     1번 66000 >= 50000 -> 0
--     2번 47400 <  50000 -> 3000
--     3번  6000 <  50000 -> 3000
--     4번 51000 >= 50000 -> 0
--   배송지는 복사본이다. 주소록을 고쳐도 과거 주문은 바뀌지 않는다.
INSERT INTO orders (order_id, order_no, member_id, total_price, delivery_fee, order_status,
                    receiver, receiver_phone, zipcode, addr1, addr2, delivery_memo,
                    ordered_at, canceled_at) VALUES
    (1, '20261020-0001', 3, 66000,    0, '정상', '홍길동', '010-0000-0003', '06236', '서울특별시 강남구 테헤란로 22', '101동 1001호', '부재 시 경비실',   '2026-10-20 14:03:00', NULL),
    (2, '20261026-0002', 4, 47400, 3000, '정상', '김영희', '010-0000-0004', '48058', '부산광역시 해운대구 센텀로 33', '202동 303호',  NULL,               '2026-10-26 09:41:00', NULL),
    (3, '20261028-0003', 3,  6000, 3000, '취소', '홍길동', '010-0000-0003', '04512', '서울특별시 중구 세종대로 110',  '7층',          NULL,               '2026-10-28 18:12:00', '2026-10-28 19:30:00'),
    (4, '20261028-0004', 4, 51000,    0, '정상', '김영희', '010-0000-0004', '48058', '부산광역시 해운대구 센텀로 33', '202동 303호',  '빠른 배송 부탁해요', '2026-10-28 21:05:00', NULL);


-- 11. order_item ----------------------------------------------------
--   product_name · price(1포) · track_snapshot 은 주문 시점 복사본이다. quantity = 섭취 일수.
--   total_price = Σ(price × quantity)
--     1번 1100×30 + 400×30 + 350×60 = 33000 + 12000 + 21000 = 66000
--     2번  790×60                    = 47400
--     3번  200×30                    =  6000
--     4번  800×30 + 450×60           = 24000 + 27000 = 51000
--   1번 주문의 데일리 기본 트랙은 1100 으로 담겨 있고 현재가는 1200 이다 —
--   가격이 바뀌어도 과거 주문이 보존되는지 확인하는 표본이다.
--   track_snapshot 은 트랙 줄에만 있다 (낱개 줄은 NULL).
INSERT INTO order_item (item_id, order_id, product_id, product_name, price, quantity, track_snapshot) VALUES
    (1, 1, 13, '데일리 기본 트랙', 1100, 30, '비타민C 1000mg + 비타민D 2000IU + 오메가3 1000mg + 마그네슘 300mg'),
    (2, 1, 10, '루테인 20mg',       400, 30, NULL),
    (3, 1,  3, '비타민B 컴플렉스',  350, 60, NULL),
    (4, 2, 15, '뼈 건강 트랙',      790, 60, '칼슘 500mg + 비타민D 2000IU + 마그네슘 300mg'),
    (5, 3,  6, '아연 15mg',         200, 30, NULL),
    (6, 4, 16, '여성 활력 트랙',    800, 30, '철분 18mg + 비타민C 1000mg + 비타민B 컴플렉스'),
    (7, 4,  8, '오메가3 1000mg',    450, 60, NULL);


-- 12. payment -------------------------------------------------------
--   amount = total_price + delivery_fee. 외부 PG 연동 없이 선택값만 저장한다.
INSERT INTO payment (payment_id, order_id, method, amount, pay_status, paid_at, refunded_at) VALUES
    (1, 1, '카드',     66000, '완료', '2026-10-20 14:03:10', NULL),
    (2, 2, '간편결제', 50400, '완료', '2026-10-26 09:41:08', NULL),
    (3, 3, '무통장',    9000, '환불', '2026-10-28 18:12:05', '2026-10-28 19:30:00'),
    (4, 4, '카드',     51000, '완료', '2026-10-28 21:05:12', NULL);


-- 13. delivery ------------------------------------------------------
--   3번은 주문이 취소됐는데 배송 상태가 '결제완료' 로 남아 있다.
--   화면에서는 orders.order_status 를 먼저 읽어 '취소' 로 표시한다 (ERD § 7 취소 주문 조회).
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
--   구매 1건당 리뷰 1개. 1번은 트랙, 2번은 낱개 리뷰다.
INSERT INTO review (review_id, member_id, product_id, item_id, rating, content, created_at) VALUES
    (1, 3, 13, 1, 5, '네 가지를 한 포에 먹으니 챙기기 편해요. 한 달째 꾸준히 먹는 중입니다.', '2026-10-24 19:10:00'),
    (2, 3, 10, 2, 4, '알약이 조금 큰 편이지만 눈 피로가 덜한 느낌입니다.',                   '2026-10-24 19:15:00');


-- 16. qna -----------------------------------------------------------
--   ★ 불변식 — answer 와 answered_at 은 함께 채우거나 함께 NULL.
--     1번은 답변 완료(둘 다 채움), 2·3번은 미답변(둘 다 NULL).
--     미답변 조회는 answered_at IS NULL 로 한다.
INSERT INTO qna (qna_id, member_id, product_id, question, is_secret, answer, created_at, answered_at) VALUES
    (1, 3,  1, '공복에 먹어도 되나요?',                            'N', '위가 예민하시면 식후 섭취를 권장합니다.', '2026-10-21 11:00:00', '2026-10-21 15:30:00'),
    (2, 4, 15, '칼슘이랑 마그네슘을 한 포에 같이 먹어도 괜찮나요?', 'N', NULL,                                      '2026-10-27 13:20:00', NULL),
    (3, 3,  9, '재입고 예정이 궁금합니다.',                         'Y', NULL,                                      '2026-10-28 10:05:00', NULL);


-- 17. admin_log -----------------------------------------------------
--   delivery_history 와 목적이 다르다 — 저쪽은 "배송이 어떻게 흘렀나",
--   이쪽은 "누가 무엇을 건드렸나". action 어휘는 ERD § 3 admin_log 고정 목록을 쓴다.
--   ★ 역할 격리(ERD § 8-8): mode 는 member 의 role 과 같아야 하고 USER 는 기록하지 않는다.
--     고객 본인 취소 · 조합 저장은 admin_log 에 남지 않는다. 9번은 관리자가 취소한 건이다.
INSERT INTO admin_log (log_id, member_id, mode, action, target_table, target_id, detail, created_at) VALUES
    ( 1, 1, 'ADMIN', 'PRODUCT_CREATE',        'product',             12,   '코엔자임Q10 100mg 등록',                                      '2026-09-12 11:00:00'),
    ( 2, 1, 'ADMIN', 'TRACK_ITEM_SET',        'track_item',          13,   '구성 (없음) -> 비타민C 1000mg, 비타민D 2000IU, 오메가3 1000mg, 마그네슘 300mg', '2026-09-15 11:05:00'),
    ( 3, 1, 'ADMIN', 'CONFLICT_CREATE',       'supplement_conflict',  1,   '칼슘 500mg × 철분 18mg',                                      '2026-09-16 10:00:00'),
    ( 4, 1, 'ADMIN', 'QNA_ANSWER',            'qna',                  1,   '공복 섭취 문의 답변',                                         '2026-10-21 15:30:00'),
    ( 5, 1, 'ADMIN', 'DELIVERY_TRACKING_SET', 'delivery',             1,   '운송장 1234567890 (샌드박스택배)',                            '2026-10-22 08:30:00'),
    ( 6, 1, 'ADMIN', 'DELIVERY_NEXT_STEP',    'delivery',             1,   '배송중 -> 배송완료',                                          '2026-10-23 15:20:00'),
    ( 7, 1, 'ADMIN', 'PRODUCT_UPDATE',        'product',             13,   '판매가 1100 -> 1200',                                         '2026-10-25 10:00:00'),
    ( 8, 2, 'DEV',   'FORCE_DELIVERY_STATUS', 'delivery',             2,   '상품준비 -> 배송중 (강제)',                                   '2026-10-28 08:10:00'),
    ( 9, 1, 'ADMIN', 'ORDER_CANCEL',          'orders',               3,   '관리자 취소 처리, 환불 완료',                                 '2026-10-28 19:30:00'),
    (10, 2, 'DEV',   'SHOP_STATUS_TOGGLE',    'system_config',     NULL,   'MAINTENANCE -> OPEN',                                         '2026-10-28 22:00:00');
