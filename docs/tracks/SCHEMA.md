# DB Schema 트랙 — 현황 (샌드박스)

> 소유: DB Schema 세션 · 샌드박스 체크아웃 전용 (main 과 내용이 다름) · 상태: 🟢 **구축 완료**

## 현재 상태

| 항목 | 값 |
|---|---|
| 스키마 | `webapplication_sandbox` (utf8mb4 / utf8mb4_unicode_ci) · 계정 `2023314009`@`localhost` |
| 구성 | 테이블 17 · 뷰 5 · FK 21 · CHECK 8 · UNIQUE 9 — 근거 `docs/ERD.md`, 정의는 `sql/schema.sql` |
| 샘플 | 계정 ADMIN 1 (`admin`) · DEV 1 (`devops`) · USER 3 (`hong`, `kim`, `leaver`=탈퇴) — 비밀번호 전부 `test1234` (평문) |
| 검증 | ERD 대조 불일치 0 · 제약 위반 시험 전부 거부 · 역할-데이터 정합 3건 PASS · 한글 바이트 정상 |
| 접속 설정 | `META-INF/context.xml` (git 제외) → `webapplication_sandbox` |

## 적용 방법

```
mysql -D webapplication_sandbox < sql/schema.sql
mysql -D webapplication_sandbox < sql/data.sql
```

재실행 가능. 실행하면 DB 전체가 샘플 상태로 돌아간다(화면에서 생긴 데이터 소멸). `sql/00_create_database.sql` 은 실행 완료 — 다시 돌리지 않는다.

## 배선이 지켜야 하는 DB 규칙

| 규칙 | 어기면 |
|---|---|
| 상품 노출 = `status='판매중' AND stock > 0` (장바구니·찜은 예외) | 재고 없는 상품 노출 |
| `qna.answer` · `answered_at` 은 같은 UPDATE 로 함께 쓴다 | CHECK 에러 |
| 취소→`canceled_at`, 환불→`refunded_at`, 배송중·완료→`courier`·`tracking_no` 함께 기록 | CHECK 에러 |

## 열린 요청 · 미결

| 대상 | 내용 |
|---|---|
| PM | `data.sql` 재실행 승인 — 현재 DB 에 `admin` 소유 구매 행 4건(역할 위반). JSP `UserFilter` 반영 완료 후 실행 (`docs/STATUS.md` P2) |
| PM · JSP | 비밀번호 평문 vs 해시 — 해시로 가면 샘플 값만 교체, DDL 변경 없음 (P4) |
| PM · JSP · UI | 샘플 이미지 404 — 결정되면 `data.sql` 의 `thumbnail` · `product_image` 반영 (P5) |
