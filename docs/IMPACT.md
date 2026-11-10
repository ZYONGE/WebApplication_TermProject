# 컨셉 전환 영향 점검 — 계층별 수정 사항

> PM 문서 · 점검일 2026-11-10 · 기준 `sandbox` 커밋 `75317a1` · 짝 문서 `docs/CONCEPT.md`
> 점검만 했다. 코드 · SQL · CSS 는 아직 바꾸지 않았다.

## 0. 전제

- **유지**: 결제 → 주문완료 → 배송 상태 전이 → 배송조회(타임라인) · 취소/환불 · 재고 복구 흐름은 지금 그대로 쓴다.
- **추가**: 영양제 조합을 한 화면에서 고르고 조회하는 **조합 탭** (`/custom`). 레퍼런스는 다나와 "PC 견적 맞추기".

| 다나와 견적 맞추기 | 이 프로젝트의 조합 탭 |
|---|---|
| 부품 분류 탭 (CPU · 메인보드 · RAM …) | 낱개 영양제 소분류 탭 (`category` 트리) |
| 분류별 상품 목록 · 검색 · 정렬 | 탭별 낱개 영양제 목록 (1포 가격 · 별점 · 재고) |
| 견적함 — 고른 부품 목록 | 내 조합 — 고른 영양제 목록 |
| 수량 | 섭취 일수 하나를 조합 전체에 적용 |
| 합계 금액 | 1포 합계 × 일수 = 총액, 배송비 미리보기 |
| 호환성 체크 | 동시 섭취 경고 + 중복 섭취 안내 |
| 추천 견적 · 인기 견적 | 트랙 패키지 — 불러와서 고쳐 쓰기 |
| 장바구니 한 번에 담기 | 조합 전체를 낱개 여러 줄로 담기 (결정 K1) |
| 견적 저장 · 공유 | 결정 대기 (§ 6 K3) |

## 1. 요약

| 계층 | 신설 | 변경 | 그대로 |
|---|---|---|---|
| ERD | 테이블 2 (`track_item` · `supplement_conflict`), 화면 2 (조합 탭 · 관리자 조합 관리) | `product` 컬럼 1, 수량 의미, 업무 규칙 · 불변식 · DTO 계약 | 결제 · 배송 · 리뷰 · 문의 · 공지 · 역할 격리 |
| DB Schema | DDL 2 테이블, 샘플 데이터 전면 교체 | `product` ALTER, 설정값 | 뷰 5개 정의 |
| JSP 배선 | 서블릿 1~2 · 서비스 1 · DAO 2 · DTO 3 · JSP 2 · 스크립트 1 | 상품 · 장바구니 · 주문 화면 표기, 관리자 상품 등록, dev 화이트리스트 | 결제 트랜잭션 · 배송 전이 · 필터 체인 |
| UI | 페이지 CSS 1, 컴포넌트 2(탭 · 경고), 목업 1 | 접두어 표, 목업 4 | 토큰 이름 · 팝업 패턴 |

## 2. ERD (`docs/ERD.md` · `docs/CRUD.md`)

### 2.1 신설 테이블

| 테이블 | 컬럼 (안) | 제약 (안) |
|---|---|---|
| `track_item` — 트랙 구성 | `track_id` FK → product · `item_id` FK → product · `sort_order` | PK (`track_id`, `item_id`). `track_id` 삭제 시 CASCADE, `item_id` 는 RESTRICT (트랙에 들어간 낱개는 못 지움) |
| `supplement_conflict` — 동시 섭취 금지 조합 | `conflict_id` PK · `product_a` · `product_b` FK → product · `message` VARCHAR · `created_at` | UNIQUE (`product_a`, `product_b`). 같은 쌍이 뒤집혀 두 번 들어가지 않게 `a < b` 로 저장 |

- ⚠️ MySQL 은 **ON DELETE CASCADE 같은 참조 동작이 걸린 FK 컬럼에 CHECK 를 금지**한다. `CHECK (product_a < product_b)` 를 쓰려면 두 FK 를 RESTRICT 로 두고, 아니면 정렬을 DAO 에서 지킨다. ERD 트랙이 고른다.
- `track_item.track_id` 는 TRACK, `item_id` 는 SINGLE 이어야 하고, `supplement_conflict` 의 두 상품은 SINGLE 이어야 한다. 행을 넘나드는 조건이라 CHECK 로 못 막는다 → **§ 8 불변식에 추가**, 서비스가 지킨다.

### 2.2 변경

| 위치 | 현재 | 변경 |
|---|---|---|
| `product` | 구분 없음 | `product_type` ENUM(`SINGLE`, `TRACK`) 추가. 인덱스 `(product_type, status)` 검토 |
| `product.price` · `stock` | 개당 가격 · 개수 | 1포 가격 · 남은 포 수 |
| `cart.quantity` · `order_item.quantity` | 개수 | **섭취 일수 = 포 수**. 타입 · CHECK 그대로 |
| `order_item` | 상품명 · 단가 스냅샷 | 트랙 구성 스냅샷 컬럼 검토 (§ 6 K6) |
| `system_config` | `LOW_STOCK_THRESHOLD` = 5 | 단위가 포라 값 재조정. 일수 상한 키 신설 검토 (§ 6 K10) |
| `admin_log.action` 어휘 | 고정 목록 | `TRACK_ITEM_SET` · `CONFLICT_CREATE` · `CONFLICT_DELETE` 추가 (관리자 조합 관리를 두는 경우) |
| § 4.4 생성 순서 | 17 테이블 | `product` 뒤에 `track_item` · `supplement_conflict` |
| § 7 업무 규칙 | — | 조합 탭 노출(낱개 · 판매중 · 재고 > 0), 경고 계산(트랙은 구성품으로 펼침), 경고는 구매를 막지 않음 |
| § 9 DTO 계약 | — | `ProductDTO.productType` · `List<ProductDTO> trackItems`, 신설 `TrackItemDTO` · `ConflictDTO` · `IntakeWarningDTO` |
| mermaid ERD (`ERD.md` · `README.md` · `src/main/java/shop/util/erd.mmd`) | 17 엔티티 | 2 엔티티 · 관계 4개 추가. `erd.mmd` 는 `/dev/erd` 가 그리는 원본이라 JSP 트랙도 함께 고친다 |

### 2.3 CRUD 매트릭스

| # | 화면 | URL (안) | 접촉 |
|---|---|---|---|
| 37 | 조합 탭 | `/custom` | `cat` R · `prd` R · `trk` R · `cnf` R · `cfg` R |
| 38 | 조합 담기 | `/cart/addAll` | `prd` R · `crt` C/U · `cnf` R |
| 39 | 관리자 조합 관리 | `/admin/conflicts` | `prd` R · `cnf` CRUD · `log` C |

- 5번 상품상세에 `trk` R(트랙 구성) · `cnf` R(이 상품과 부딪히는 낱개) 추가. 6 · 7번 장바구니 · 주문서에 `trk` R · `cnf` R 추가 (경고).
- 23번 상품관리에 `trk` CRUD 추가.
- 약어 2개 신설 (`trk` · `cnf`), § 5 커버리지 표에 2행 추가.

## 3. DB Schema (`sql/schema.sql` · `sql/data.sql`)

| 항목 | 할 일 |
|---|---|
| DDL | `track_item` · `supplement_conflict` CREATE, DROP 목록에 역순 추가, `product.product_type` 추가 |
| 뷰 | 5개 정의는 그대로 둔다. `v_low_stock` 은 기준값만 바뀌고, `v_admin_order` 의 첫 상품명 + "외 N건" 도 그대로 맞는다 |
| `data.sql` | 의류 카테고리 · 상품 · 이미지 · 장바구니 · 찜 · 주문 상품 · 리뷰 · 문의 샘플을 영양제 · 트랙 · 금지 조합 샘플로 교체. 주문 · 결제 · 배송 · 이력 행은 구조를 그대로 두고 상품 참조만 맞춘다 |
| 시연 표본 | 지금처럼 재고 0 · 재고 임계 · 숨김 표본을 하나씩 두고, 경고가 뜨는 조합 · 트랙과 낱개가 겹치는 조합을 하나씩 둔다 |
| 적용 | K2 에 따라 로컬 DB 재적용은 보류. 파일만 고친다 |

## 4. JSP 배선 (`src/main/java/shop/**` · `**/*.jsp` · `web.xml` · `js/validate.js`)

### 4.1 신설

| 종류 | 이름 (안) | 하는 일 |
|---|---|---|
| Servlet | `CustomServlet` — `/custom` | 조합 탭. 공개 열람(다나와처럼 비로그인도 조회), 담기 버튼은 USER 에게만 |
| Servlet 동작 | `CartServlet` 에 `/cart/addAll` | 조합 전체를 같은 일수로 담는다. `/cart/*` 라 기존 `UserFilter` 를 그대로 탄다 — `web.xml` 수정 불필요 |
| Servlet | `AdminConflictServlet` — `/admin/conflicts` | 금지 조합 등록 · 삭제 (K7) |
| Service | `IntakeService` | 상품 묶음 → 트랙 펼치기 → 금지 조합 · 중복 성분 경고 목록. 조합 탭 · 상세 · 장바구니 · 주문서가 공통으로 쓴다 |
| DAO | `TrackItemDAO` · `ConflictDAO` | 구성 조회 · 저장, 금지 조합 조회 · 저장 |
| DTO | `TrackItemDTO` · `ConflictDTO` · `IntakeWarningDTO` | 경고 1건 = 종류(금지 · 중복) · 두 상품명 · 문구 |
| JSP | `custom/index.jsp` · `admin/conflicts.jsp` | 조합 탭 화면, 관리자 조합 관리 |
| 스크립트 | `js/custom.js` (소유 K11) | 탭 전환 없이 고르는 즉시 합계 · 경고를 다시 계산. 금지 조합 목록은 서버가 페이지에 데이터로 심어 준다. 담을 때 서버가 다시 검사한다 |

### 4.2 변경

| 파일 | 현재 | 변경 |
|---|---|---|
| `ProductDTO` · `ProductDAO` | 구분 없음 | `productType` 매핑, 목록 필터에 구분 추가, 조합 탭용 "낱개 · 판매중 · 재고 > 0" 조회 |
| `CartDAO.java:91` | `ON DUPLICATE KEY UPDATE quantity = quantity + ...` 로 합산 | 일수 의미에 맞게 덮어쓰기로 바꿀지 결정 (K4) |
| `CartDAO.java:70` · `BaseServlet.java:139` | 헤더 배지가 `SUM(quantity)` | 일수 합이 되어 버린다 (30일 × 3종 = 90). 줄 수 `COUNT(*)` 로 |
| `CartService.java:22` | `MAX_QUANTITY = 99`, 문구 "1~99개" | 일수 상한 · 문구 "일" 로 (K10) |
| `CartItemDTO` · `CartDAO` | 구분 없음 | `productType` 추가. `CartServlet` 이 `IntakeService` 경고를 모델에 싣는다 |
| `OrderServlet` (주문서) | — | 결제 전 경고를 다시 보여준다. 결제 트랜잭션(`OrderService.pay`)은 손대지 않는다 |
| `OrderService.pay` | 스냅샷: 이름 · 단가 | K6 를 고르면 트랙 구성 문자열도 스냅샷 |
| `AdminService` · `AdminProductServlet` · `admin/products.jsp` | 상품 등록 팝업 | 구분 선택, 트랙이면 구성 낱개 고르기 (`track_item` 저장 + `TRACK_ITEM_SET` 로그) |
| `DevDAO.java:31-64` | 화이트리스트 17 + 5, TRUNCATE 14 | 신설 테이블 추가. `dev/index.jsp:46` "17 테이블" · `dev/seed.jsp:51` "14개" 문구도 |
| `DevService.java:351` | 더미 상품에 구분 없음 | `product_type` 지정 |
| `OrderDTO.java:53` · `CategoryDAO.java:68` | 주석에 의류 예시 | 주석 정리 |
| `common/header.jsp:8,35` | 사이트명 `SHOP`, 내비 = 카테고리 트리 | 사이트명 (K9), 내비에 "조합 맞추기" · "트랙 패키지" |
| `main.jsp` | 공지 + 신상품 | 트랙 패키지 영역 · 조합 탭 진입 추가 |
| `product/list.jsp` | 카테고리 · 정렬 · 검색 | 낱개 / 트랙 구분 필터, 가격을 "1포 N원" |
| `product/view.jsp:25,46-47` | 재고 "N개", 수량 입력 | 재고 "N포", 섭취 일수 선택(빠른 선택 + 직접 입력), 트랙이면 구성표, 낱개면 함께 먹지 말 것 목록 |
| `cart/list.jsp:25,43,58` | 수량 · "재고 N개" | 섭취 일수 · "N포", 경고 박스, 일수가 서로 다른 줄 안내 |
| `order/form.jsp` · `complete.jsp` · `view.jsp` · `list.jsp` · `admin/orders.jsp` | 수량 열 | "N일분" 표기. 배송 타임라인 · 취소 버튼은 그대로 |
| `admin/dashboard.jsp:65` · `dev/state.jsp:52` · `admin/products.jsp:111` | "개" | "포" |
| `js/validate.js` | `positiveInt` | 일수 범위 규칙 추가 |

### 4.3 그대로

결제 트랜잭션 7단계 · 재고 차감 가드 · 배송 단방향 전이 · `/dev` 강제 전이 · 취소/환불 · 리뷰(배송완료 건당 1회) · 문의 · 공지 · 배송지 · 3역할 필터 체인 · 점검 모드.

## 5. UI (`css/**` · `mockup/**` · `js/ui.js` · `css/DECISIONS.md`)

| 항목 | 할 일 |
|---|---|
| 페이지 CSS | `pages/custom.css` 신설, 접두어 `custom_*` (조합 탭 3분할: 분류 탭 · 상품 목록 · 내 조합) |
| 컴포넌트 | 탭 `tab*` · 경고 박스 `warnbox*` — 상품상세 · 장바구니 · 주문서 · 조합 탭이 함께 쓰므로 `components/` 에 둔다. 새 접두어라 DECISIONS § 2 표 추가가 필요하다 (PM 계약) |
| 기존 페이지 CSS | `prod_*` 에 섭취 일수 선택 · 트랙 구성표, `cart_*` 에 경고 자리 |
| 목업 | `mockup/custom.html` 신설. `_layout`(내비 항목) · `main` · `product_view` · `cart_list` 수정 |
| `js/ui.js` | 탭 여닫기를 넣는다면 `data-tab-*` 속성 패턴 추가 (팝업과 같은 방식) |
| 토큰 | 경고 색은 기존 `--color-danger` 로 시작. 주의 단계를 따로 둔다면 `--color-warning` 신설 (이름 계약) |
| 컨셉 값 | 색 · 타이포 값은 여전히 대기. 이번 전환으로 업종이 정해졌으니 컨셉 전달 시점을 PM이 정한다 |

## 6. PM 결정 필요

| # | 항목 | 추천 | 대안 |
|---|---|---|---|
| K3 | 조합 보관 · 공유 (다나와 견적 저장) | 테이블 없이 **URL 쿼리**로 상태를 둔다 (`/custom?pick=1,4,7&days=30`). 새로고침 · 링크 공유가 되고 담으면 장바구니가 저장 역할 | `combo` · `combo_item` 테이블로 마이페이지 "내 조합" 저장 |
| K4 | 같은 상품을 다시 담을 때 | 일수 **덮어쓰기** | 기존처럼 합산 |
| K5 | 트랙 재고 | 트랙 상품 **자체 `stock`** | 구성 낱개 재고를 함께 차감 (결제 트랜잭션 변경) |
| K6 | 주문 시점 트랙 구성 보관 | `order_item` 에 구성 **스냅샷 문자열** — 기존 스냅샷 원칙과 같다 | 보관 안 함 (나중에 구성이 바뀌면 과거 주문이 달라 보임) |
| K7 | 금지 조합 관리 주체 | **관리자 화면** `/admin/conflicts` + `admin_log` | `data.sql` 로만 |
| K8 | 분류 탭당 선택 수 | **여러 개** 허용 | 다나와처럼 슬롯당 1개 |
| K9 | 사이트명 | — | 헤더 · `<title>` 의 `SHOP` 대체 |
| K10 | 섭취 일수 범위 | `system_config` 키로 상한을 둔다 (`/dev` 에서 시연 가능) | 코드 상수 |
| K11 | `js/custom.js` 소유 | **JSP 트랙** (`validate.js` 처럼 데이터 로직) | UI 트랙 |
| K12 | 계약 변경 승인 | 새 URL 3개 · DTO 3개 · 클래스 접두어 3개(`custom_` `tab` `warnbox`) | — |

## 7. 작업 순서

1. PM: § 6 결정 → `CONCEPT.md` 반영
2. ERD: 테이블 · 규칙 · 불변식 · DTO 계약 · CRUD (다른 트랙의 기준)
3. DB Schema 와 UI 목업을 병렬로 (DDL · 샘플 / `custom.html` · 경고 박스)
4. JSP 배선: DAO · DTO → `IntakeService` → 조합 탭 · 장바구니 · 상품 화면 → 관리자 · dev
5. 검증: 컴파일 (K2). DB 재적용 · 실행 확인은 별도 승인 후
