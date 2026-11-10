# DB Schema 트랙 — 현황 (샌드박스)

> 소유: DB Schema 세션 · 샌드박스 체크아웃 전용 (main 과 내용이 다름) · 갱신 2026-11-10
> 상태: 🟡 **파일 갱신 완료 · DB 미적용** — 커스텀 영양제 컨셉 (CONCEPT K2 로 재적용 보류)

## 현재 상태

| 항목 | 값 |
|---|---|
| 스키마 | `webapplication_sandbox` (utf8mb4 / utf8mb4_unicode_ci) · 계정 `2023314009`@`localhost` |
| 파일 기준 구성 | 테이블 21 · 뷰 5 · FK 28 · CHECK 10 · UNIQUE 11 — 근거 `docs/ERD.md`, 정의는 `sql/schema.sql` |
| 로컬 DB 실제 구성 | 전환 전 그대로 (17 테이블 · 의류 샘플). 재적용 전까지 파일과 다르다 |
| 이번 변경 | 신설 `track_item` · `supplement_conflict` · `combo` · `combo_item`, `product.product_type` + `idx_product_type_status`, `order_item.track_snapshot`. 뷰 5개 정의는 그대로 |
| 샘플 | 계정 ADMIN 1 · DEV 1 · USER 3 (전과 같음, 비밀번호 `test1234` 평문). 카테고리 대분류 2(낱개 / 트랙) · 소분류 5, 낱개 12 · 트랙 4, 트랙 구성 14행, 금지 조합 4쌍, 저장 조합 3(`출근 전 루틴` · `운동 후 회복` · `JY package`), 주문 4건(금액 재계산) |
| 시연 표본 | 재고 0 = 9번 유산균 · 재고 부족(≤30포) = 6번 아연 · 15번 뼈 건강 트랙 · 숨김 = 12번 코엔자임Q10 · 중복 섭취 = 홍길동 장바구니(데일리 트랙 + 비타민C) · 트랙 펼침 경고 = 김영희 장바구니(여성 활력 트랙의 철분 × 칼슘) · 낱개끼리 경고 = 조합 `운동 후 회복`(칼슘 × 아연) · 가격 변경 스냅샷 = 주문 1 의 데일리 트랙 1100원(현재 1200원) |
| 검증 (2026-11-10) | 임시 DB `webapplication_sandbox_verify` 에 `USE` 줄을 뺀 두 파일을 적용 → 검사 → DROP. 실제 `webapplication_sandbox` 는 손대지 않음(17 테이블 그대로). 결과: 2회 연속 실행 성공 · 구성 21 / 5 / 28 / 10 / 11 일치 · 불변식 1 · 2 · 8 ~ 14 위반 0 · 배송비 · 스냅샷 문자열 · 일수 상한 일치 · 제약 거부 시험 10건 전부 거부 (`CHECK (product_a < product_b)` 는 RESTRICT FK 와 함께 정상 동작) · CASCADE(트랙 → 구성, 조합 → 구성) 확인 · 경고 시연 표본 3종 · 재고 없는 조합 구성 · 조합 탭 노출 목록 기대대로 · 한글 바이트 정상 |
| 접속 설정 | `META-INF/context.xml` (git 제외) → `webapplication_sandbox` |

## 적용 방법 — PM 승인 후 (K2 · P2)

```
mysql -D webapplication_sandbox < sql/schema.sql
mysql -D webapplication_sandbox < sql/data.sql
```

재실행 가능. 실행하면 DB 전체가 샘플 상태로 돌아간다(화면에서 생긴 데이터 소멸). `sql/00_create_database.sql` 은 실행 완료 — 다시 돌리지 않는다.
기존 JSP 코드는 적용 후에도 동작한다 — `product_type` 은 기본값 `SINGLE`, `track_snapshot` 은 NULL 허용이고 DAO INSERT 는 컬럼을 명시한다.

## 배선이 지켜야 하는 DB 규칙

| 규칙 | 어기면 |
|---|---|
| 상품 노출 = `status='판매중' AND stock > 0` (장바구니·찜·내 조합은 예외). 조합 탭 낱개 목록은 여기에 `product_type='SINGLE'` | 재고 없는 상품 노출 |
| `qna.answer` · `answered_at` 은 같은 UPDATE 로 함께 쓴다 | CHECK 에러 |
| 취소→`canceled_at`, 환불→`refunded_at`, 배송중·완료→`courier`·`tracking_no` 함께 기록 | CHECK 에러 |
| `supplement_conflict` 는 작은 id 를 `product_a` 로 정렬해 INSERT, 조회는 `LEAST` · `GREATEST` | CHECK 에러 · 경고 누락 |
| 금지 조합 · 트랙 구성에 걸린 낱개는 DELETE 불가 — 먼저 쌍 · 구성을 지운다 | FK 에러 (RESTRICT) |
| `combo` 조회 · 수정 · 삭제는 `AND member_id = 세션 회원` 을 함께 건다 | 남의 조합 열람 |
| 같은 회원이 같은 이름으로 `combo` INSERT | UNIQUE 에러 — 덮어쓰기면 UPDATE |
| `cart.quantity` · `combo.days` ≤ `MAX_INTAKE_DAYS` (DB 는 `> 0` 만 강제) | 상한 초과 저장 |

## 열린 요청 · 미결

| 대상 | 내용 |
|---|---|
| PM | `schema.sql` · `data.sql` 재적용 승인 — 컨셉 전환분 + 기존 역할 위반 4행 제거. 테스터 데이터가 지워진다 (K2 · P2) |
| PM · JSP | 비밀번호 평문 vs 해시 — 해시로 가면 샘플 값만 교체, DDL 변경 없음 (P4) |
| PM · JSP · UI | 샘플 이미지 404 — 썸네일 경로를 영양제용으로 바꿨다 (`/upload/sample-vitc.png` 등 16개 + 갤러리 5개). 파일은 없다 (P5) |
