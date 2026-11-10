# 트랙 3 — ERD 설계 (샌드박스)

> 소유: ERD 세션 · 쓰기: `docs/ERD.md` · `docs/CRUD.md` · 이 문서 · 갱신 2026-11-10

## 현재 상태

| 항목 | 상태 |
|---|---|
| 컨셉 | **커스텀 영양제** 로 전환 반영 (`docs/CONCEPT.md` · `docs/IMPACT.md` § 2) |
| ERD | 21 테이블 · 뷰 5 · FK 28 · CHECK 10 · 인덱스 10. 신설 4: `track_item` · `supplement_conflict` · `combo` · `combo_item`. 변경: `product.product_type`, `order_item.track_snapshot`, `system_config` (`LOW_STOCK_THRESHOLD`=30, `MAX_INTAKE_DAYS`=90), `admin_log` 어휘 4개 |
| 규칙 | § 7.1 영양제 규칙 16행 · § 8 불변식 9 ~ 15 추가 |
| CRUD | 화면 41 × 21 테이블. 신설 37 조합 탭 · 38 조합 담기 · 39 관리자 조합 관리 · 40 조합 저장 · 41 내 조합 |
| DTO 계약 | 신설 `TrackItemDTO` · `ConflictDTO` · `IntakeWarningDTO` · `ComboDTO` · `ComboItemDTO`, 필드 추가 `ProductDTO` · `CartItemDTO` · `WishItemDTO` · `OrderItemDTO` |
| DB 반영 | DDL · 샘플 작성 완료 (`sql/*.sql`). 로컬 DB 재적용은 CONCEPT K2 로 보류 |
| main | `docs/ERD.md` · `docs/CRUD.md` 미작성 — 환원 범위 결정 대기 (STATUS M4) |

## 이번 전환에서 정한 것 — PM 확인 필요

| # | 내용 | 근거 |
|---|---|---|
| K3 | **조합 저장은 DB** — `combo` · `combo_item` 테이블, 마이페이지 "내 조합". IMPACT 추천안(URL 쿼리)이 아니라 대안을 택했다. URL 쿼리 공유는 선택 사항으로 남김 | 사용자 지시 2026-11-10: "이름을 붙여 저장했다가 불러올 수 있어야 함" (예: `JY package`) |
| K4 · K5 · K6 · K7 · K10 | IMPACT § 6 추천안 그대로 — 일수 덮어쓰기 · 트랙 자체 재고 · `order_item.track_snapshot` · `/admin/conflicts` + `CONFLICT_*` 로그 · `MAX_INTAKE_DAYS` | 사용자 지시 2026-11-10 "추천안 채택" |
| — | `supplement_conflict` 는 두 FK 를 RESTRICT 로 두고 `CHECK (product_a < product_b)` 를 건다 (IMPACT § 2.1 ⚠️ 의 선택지 중 DB 강제 쪽) | ERD 트랙 판단 |
| — | 화면 번호: 39 = 관리자 조합 관리(IMPACT 번호 유지), 40 · 41 = 조합 저장 · 내 조합 | ERD 트랙 판단 |

## 열린 요청 — 다른 트랙이 처리

| 대상 | 내용 | STATUS |
|---|---|---|
| PM | `CONCEPT.md` § 4 에 K3(DB 저장) · K4 ~ K7 · K10 결정 기록, § 6 ERD 행에 `combo` · `combo_item` 추가 | 신규 |
| PM | `README.md` mermaid ERD 를 `docs/ERD.md` § 2 와 맞춤 (엔티티 4 · 관계 7 추가) | 신규 |
| JSP | `src/main/java/shop/util/erd.mmd` 를 `docs/ERD.md` § 2 와 맞춤 (`/dev/erd` 원본) | 신규 |
| JSP | 신설 화면 37 ~ 41 · DTO 5개 · `IntakeService` (경고 계산은 ERD § 7.1). 40 · 41 은 `/mypage/*`, 38 은 `/cart/*` 라 `web.xml` 수정 불필요 | 신규 |
| JSP | `DevDAO` 화이트리스트 21 테이블 · TRUNCATE 목록에 신설 4개, `dev/index.jsp` "17 테이블" 문구 | 신규 |
| JSP | `CartDAO` 담기를 일수 덮어쓰기로, 헤더 배지 `COUNT(*)`, `CartService.MAX_QUANTITY` → `MAX_INTAKE_DAYS`, `ConfigService` 재고부족 기본값 5 → 30 | 신규 |
| JSP | `OrderDAO` `order_item` INSERT 에 `track_snapshot` | 신규 |
| JSP | `admin_log.action` 에 `REVIEW_DELETE` · `CONFIG_UPDATE` 반영. 현재 설정 변경이 `SHOP_STATUS_TOGGLE` 로 기록됨 | 전달 사항 |
| UI ↔ JSP | ADMIN·DEV 구매 안내문 클래스 불일치 — 목업 `empty` / `product/view.jsp` `field_help` | 미등재 (C5 와 함께 처리 요망) |

## PM 결정 대기 중 ERD 문서에 닿는 것

| STATUS | 항목 | 결정 후 ERD 쪽 작업 |
|---|---|---|
| P2 · K2 | 샌드박스 `schema.sql` · `data.sql` 재적용 | 없음 |
| P4 | 비밀번호 해시 vs 평문 | `member.password` 설명 수정 |
| C1 | 화면 목록 · URL 매핑 | `CRUD.md` URL 열 교체 (37 ~ 41 포함) |
| C2 | 팝업 방식 | 별창이면 `CRUD.md` 에 팝업 행 추가 |
| C4 | `OrderDTO` 조인 필드 4개 · EL 예약어 프로퍼티명 금지 | `ERD.md` § 9 DTO 계약 갱신 |
| D1 | 상품은 리프 카테고리에만 등록 | `ERD.md` § 7 규칙 한 줄 |
| K8 | 분류 탭당 선택 수 | 없음 (화면 규칙) |
| K12 | 계약 변경 승인 (URL · DTO · 클래스 접두어) | 승인 내용대로 § 9 · CRUD URL 정리 |
| M4 | main 환원 범위 | main 문서 작성 |
