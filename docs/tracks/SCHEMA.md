# 트랙 4 — DB Schema

> 소유 세션: **DB Schema 세션** · 이 문서는 해당 세션이 직접 갱신한다.
> 세션 생성 직후 읽을 역할·권한 안내: `sql/CLAUDE.md` (작업 디렉터리에서 자동 로드)
> 최종 갱신: 2026-10-29 (DB Schema 세션, UTF-8 인코딩 고정) · 상태: 🟢 **1차 완료 — 미결 0건**
> ⚠️ 이 문서는 **샌드박스 체크아웃 전용**이다. main 의 같은 파일과 내용이 다르다. (§ 9)

---

## 1. 소유 파일 — 이 트랙만 수정한다

| 경로 | 내용 |
|---|---|
| `sql/schema.sql` | 테이블 생성 DDL |
| `sql/data.sql` | 샘플 데이터 |

## 2. 읽기만 하는 파일

- `docs/ERD.md`, `docs/CRUD.md` — **설계 근거. 수정 금지** (ERD 트랙 소유)
- `src/main/java/shop/dao/**` — DAO가 기대하는 컬럼 확인. **수정 금지**

## 3. 착수 조건

| 조건 | 상태 |
|---|---|
| ERD | 🟢 **완료 · 인계 수령** — `docs/ERD.md` (17테이블 + 뷰 5개 + DTO 계약). 짝 문서 `docs/CRUD.md` |
| DDL 작성 근거 | 🟢 **전부 확보** — § 2.1~2.17 컬럼·제약 / § 2.18 인덱스 9개 / **§ 2.19 FK 의존 생성 순서** / § 3.1~3.5 뷰 |
| DB 계정 · 스키마명 확정 | 🟢 **완료** — `webapplication_sandbox` / `2023314009`@`localhost` (§ 9.1) |
| 스키마 생성 · 권한 부여 | 🟢 **완료** — `sql/00_create_database.sql` 1회 실행됨 |

**차단 요인 없음.** 지시가 오면 § 9.4 순서로 착수한다.

## 4. 작업 규칙

- **ERD를 그대로 반영한다.** 스키마와 ERD가 어긋나면 ERD를 기준으로 맞춘다. (임의 변경 금지)
- 담당 범위는 **데이터의 저장**이다. (공지 항목 8)
- MySQL 9.6.0 기준. 문자셋은 `utf8mb4` / `utf8mb4_unicode_ci` 권장.
- `schema.sql`은 **재실행 가능**하게 작성한다. (`DROP TABLE IF EXISTS` → `CREATE TABLE`)
- FK 제약을 실제로 선다. 참조 무결성을 애플리케이션에만 맡기지 않는다.
- `data.sql`은 화면 확인에 충분한 최소 샘플만. 실제 개인정보·외부 데이터 사용 금지.
- 스키마명 / 계정 / 비밀번호는 `WebContents/META-INF/context.xml` 과 **반드시 일치**시키고, 값은 `context.xml.example`에 placeholder로만 남긴다.

## 5. Done 기준

- [x] `schema.sql` 전체 테이블 DDL — 재실행 시 오류 없음 (3회 반복 확인)
- [x] `data.sql` 샘플 데이터 — § 2.19 FK 의존 순서 준수
- [x] 실제 DB 적용 성공 — `webapplication_sandbox` (CLI 검증. DBeaver 확인은 사용자 몫)
- [x] ERD ↔ 스키마 1:1 대조 완료 — **불일치 0건** (§ 9.5)
- [x] `context.xml` 의 스키마명·계정과 일치 확인
- [ ] README `DDL` · `테이블 명세` 섹션 내용 PM에 인계 — 샌드박스는 main README 에 반영하지 않는다

## 6. 진행 상황 로그

| 날짜 | 작업 | 산출물 |
|---|---|---|
| 2026-10-28 | 세션 개설. 역할·권한 범위 학습 (`CLAUDE.md`, `sql/CLAUDE.md`, 이 문서), 착수 조건 점검 — `docs/ERD.md` · `docs/CRUD.md` 모두 0 byte 확인. 체크리스트 §2-2 에 따라 **DDL 미착수** | 이 문서 §3·§7·§8 갱신 |
| 2026-10-29 | 로컬 MySQL 권한 실측 — 개발 계정의 전역 권한이 `USAGE` 뿐이라 `CREATE DATABASE` 가 `ERROR 1044 (42000)` 으로 거부되는 것을 재현 | 이 문서 § 9.2 |
| 2026-10-29 | root 비밀번호 재설정 후 전용 스키마 `webapplication_sandbox` 생성 + 개발 계정에 권한 부여 | `sql/00_create_database.sql` |
| 2026-10-29 | 샌드박스 접속 설정을 전용 스키마로 전환. main · 수업 실습 DB(`web`) 와 완전 분리 | `META-INF/context.xml`, `context.xml.example` |
| 2026-10-29 | ERD 트랙이 `docs/ERD.md` 를 17테이블 + 뷰 5개로 확정한 것을 확인. 착수 조건 전부 충족 | 이 문서 § 3 |
| 2026-10-29 | **구축 수행.** `web` 의 `sb_*` 6개 삭제 → `docs/ERD.md` 기준 17테이블 + 인덱스 + 뷰 5개 DDL 작성 → 샘플 데이터 작성 → `webapplication_sandbox` 적용 · 재실행 검증 | `sql/schema.sql`, `sql/data.sql` |
| 2026-10-29 | ERD § 9.3~9.5 추가 제약 7건 반영 — CHECK 5건 · UNIQUE 2건 · FK 21개 `ON UPDATE RESTRICT` 명시. 위반 데이터 9종으로 작동 시험 | `sql/schema.sql` |
| 2026-10-29 | `product.status` 를 `ENUM('판매중','숨김')` 2값으로 전환. `v_low_stock` 조건·샘플 데이터·노출 규칙 주석 동반 수정 후 재적용 | `sql/schema.sql`, `sql/data.sql` |
| 2026-10-29 | 🔴 **한글 이중 인코딩 발견·복구.** 클라이언트 기본 문자셋이 `latin1` 이라 전 테이블 한글이 깨진 채 저장돼 있었다. 세 지점에 UTF-8 을 못 박고 전면 재적재 | `sql/**`, `META-INF/context.xml*`, `~/.my.cnf` |

## 7. 다른 트랙에 넘길 요청

| 대상 | 요청 내용 | 상태 |
|---|---|---|
| ERD 트랙 (완료) | § 9.1 "전용 스키마 불가" 전제 해소 전파 — 접두사 불필요 | ✅ 반영됨 |
| ERD 트랙 (완료) | `idx_qna_answer` 키 길이 초과 → `idx_qna_unanswered (answered_at)` 대체 | ✅ 반영됨 |
| ERD 트랙 (완료) | § 3.4 `v_admin_member.qna_waiting` 을 `answered_at IS NULL` 로 수정 | ✅ 반영됨 |
| ERD 트랙 (완료) | § 9.3~9.5 추가 제약 7건 수령 — 전부 `schema.sql` 에 반영 | ✅ 반영됨 |
| ERD 트랙 (완료) | `product.status` 의 `'품절'` 제거 확정 수령 → `ENUM('판매중','숨김')` 반영. 노출 규칙·`v_low_stock` 조건까지 함께 적용 | ✅ 반영됨 |

## 8. 미결 / 질문

- ✅ **미결 사항 없음.** `product.status` 2값 전환까지 반영 완료 (§ 9.8).
- ℹ️ ERD § 9.6 의 **DB가 강제할 수 없는 불변식 7건**은 서비스 계층(JSP 배선 트랙) 책임이다.
  DDL 로 막을 수 없는 것들이므로 `data.sql` 샘플은 전부 그 불변식을 지켜 작성했다 — 배선 트랙이
  참고할 수 있는 정상 표본이 된다.
- ℹ️ CASCADE 연쇄 2단(`orders` → `order_item` → `review`)이 있다. `/dev/seed` 로 주문을 지우면
  리뷰까지 사라진다. 정상 동작이며 시연 시 알고 있어야 한다. (ERD § 9.3)

## 9. 샌드박스 DB 구축 기록 (zyonge_Sandbox 전용 · 2026-10-29)

> 이 섹션은 **실험 체크아웃에만 존재한다.** `main` 에 반영하지 않는다. (CLAUDE.md § 4.5)

### 9.1 대상 DB — `webapplication_sandbox` (전용 스키마)

| 항목 | 값 |
|---|---|
| 스키마명 | `webapplication_sandbox` |
| 문자셋 / 콜레이션 | `utf8mb4` / `utf8mb4_unicode_ci` |
| 접속 계정 | `2023314009`@`localhost` |
| 접속 설정 | `WebContents/META-INF/context.xml` (git 추적 제외) |
| 생성 스크립트 | `sql/00_create_database.sql` — 관리자 계정으로 **1회 실행 완료** |

**main 과 공유하지 않는다.** 실험이 어떤 경로로도 main 의 데이터에 닿지 않도록 스키마 단위로 끊었다.
수업 실습 DB(`web`)도 쓰지 않는다 — 실습 테이블과 섞이면 어느 쪽이 실험 산출물인지 구분되지 않는다.

### 9.2 해소된 제약 — 전용 스키마를 쓸 수 있다

초기에 `CREATE DATABASE` 가 거부됐다.

```
ERROR 1044 (42000): Access denied for user '2023314009'@'localhost'
                    to database 'webapplication_sandbox'
```

원인은 OS 소유권이 아니라 **MySQL 계정 권한**이었다. 개발 계정의 전역 권한이 `USAGE` 뿐이라
어떤 이름으로도 스키마를 만들 수 없었다. root 비밀번호를 재설정해 관리자 접근을 확보하고,
전용 스키마 생성 + 권한 부여를 1회 수행해 해소했다.

현재 권한:

```
GRANT USAGE ON *.* TO `2023314009`@`localhost`
GRANT ALL PRIVILEGES ON `web`.* TO `2023314009`@`localhost`
GRANT ALL PRIVILEGES ON `webapplication_sandbox`.* TO `2023314009`@`localhost`
```

전역 `CREATE` 는 **의도적으로 부여하지 않았다.** 스키마를 또 만들 일은 관리자 계정으로 처리한다.

> ⚠️ **ERD 트랙에 전파 필요** — `docs/tracks/ERD.md` § 9.1 이 "전용 스키마를 못 쓴다"를 전제로
> 테이블명에 `sb_` 접두사를 붙일지 묻고 있다. **그 전제는 해소됐다. 접두사는 필요 없고,
> `docs/ERD.md` 의 테이블명(`member`, `orders` …)을 그대로 쓴다.**

### 9.3 처리 완료 — `web` 의 `sb_*` 6개 삭제됨

전용 스키마를 못 쓰던 시점에 `web` 안에 임시로 만든 6개 테이블(`sb_member` `sb_category`
`sb_product` `sb_cart` `sb_order` `sb_order_item`)은 **2026-10-29 삭제했다.**
`docs/ERD.md` 를 보지 못한 채 임의 설계한 것이라 설계 근거가 없었다.
`web` 은 수업 실습 테이블 5개(`book` `message` `movie` `purchase` `user`)만 남은 원래 상태다.

### 9.4 구축 결과

| 항목 | 수량 | 근거 |
|---|---|---|
| 테이블 | **17** | ERD § 2.1 ~ 2.17 |
| 뷰 | **5** | ERD § 3.1 ~ 3.5 |
| FK | **21** (CASCADE 11 / RESTRICT 9 / SET NULL 1) — 전부 `ON UPDATE RESTRICT` 명시 | ERD § 9.3 |
| CHECK | **8** (기본 3 + ERD § 9.4 권고 5) | ERD § 2.6 · 2.9 · 2.13 · § 9.4 |
| UNIQUE | **9** (기본 7 + ERD § 9.5 권고 2) | ERD § 2 · § 9.5 |
| 인덱스 | ERD § 2.18 의 **9개 전부** + FK 보조 11개 | ERD § 2.18 |
| 샘플 데이터 | 17테이블 74행 | ERD § 2.19 |

생성 순서는 ERD § 2.19 표를 그대로 따랐고, `DROP` 은 그 역순이다. 뷰는 테이블 전체 생성 후 마지막.

### 9.5 검증 결과 — 전 항목 통과

**구조 대조 (ERD ↔ 실제 DB)**

| 검증 | 결과 |
|---|---|
| 테이블 17개 · 컬럼명 · 컬럼 순서 | ✅ 불일치 **0건** |
| FK 21개 · 참조 대상 · `ON DELETE` 규칙 | ✅ ERD § 9.3 과 완전 일치 |
| ERD § 2.18 인덱스 9개 | ✅ 전부 존재 |
| `qna.answer` 인덱스 부재 (ERROR 1071 회피) | ✅ 0건 확인 |
| 뷰 5개 동작 | ✅ 전부 조회 성공 |
| `schema.sql` → `data.sql` 재실행 | ✅ 3회 반복 무오류 |

**제약 작동 시험** — 위반 데이터 9종을 넣어 전부 거부되는 것을 확인했다.

| 시험 | 결과 |
|---|---|
| `qna` answer 만 채움 | ✅ 거부 (`chk_qna_answered`) |
| `orders` 취소인데 `canceled_at` NULL | ✅ 거부 (`chk_orders_canceled`) |
| `payment` 환불인데 `refunded_at` NULL | ✅ 거부 (`chk_payment_refunded`) |
| `delivery_history` prev = new | ✅ 거부 (`chk_delivery_history_changed`) |
| `delivery` 운송장 없이 배송중 | ✅ 거부 (`chk_delivery_tracking`) |
| `product_image` `sort_order` 중복 | ✅ 거부 (`uk_product_image_sort`) |
| `category` 같은 부모 밑 동명 형제 | ✅ 거부 (`uk_category_parent_name`) |
| `cart` `quantity = 0` | ✅ 거부 (`ck_cart_quantity`) |
| `review` `rating = 6` | ✅ 거부 (`ck_review_rating`) |

**데이터 정합성** — ERD § 9.6 의 "DB가 못 막는 불변식" 을 샘플이 지키는지 질의로 확인했다.

| 검증 | 결과 |
|---|---|
| `orders.total_price` = Σ(`order_item.price` × `quantity`) | ✅ PASS |
| `delivery_fee` = 5만원 이상 무료 규칙 | ✅ PASS |
| `payment.amount` = 상품합 + 배송비 | ✅ PASS |
| 리뷰는 `배송완료` 건에만 | ✅ PASS |
| 회원당 기본 배송지 1개 이하 | ✅ PASS |
| `review.product_id` = `order_item.product_id` | ✅ PASS |

### 9.6 샘플 데이터가 담고 있는 시연 표본

화면 확인용 최소 데이터지만, 설계 의도를 눈으로 확인할 수 있는 표본을 일부러 심었다.

| 표본 | 내용 |
|---|---|
| 주문 시점 스냅샷 | 1번 주문의 티셔츠 단가 **17000** vs 현재가 **19000** — 가격이 바뀌어도 주문내역 보존 |
| 배송 4단계 | 배송완료 / 배송중 / 결제완료 2건 — 타임라인 9행 |
| `/dev` 강제 변경 | 2번 주문 이력 중 1건이 `is_forced='Y'` |
| 취소 주문 | 3번 — `order_status='취소'` + `payment.pay_status='환불'`, 배송은 `결제완료` 로 멈춰 있음 (ERD § 8 쟁점 1 재현) |
| 무료배송 분기 | 1·4번 무료(0원) / 2·3번 3000원 |
| 재고 부족 | `v_low_stock` 에 2건 — 숨김 상품은 제외됨 |
| 미답변 문의 | `qna` 3건 중 2건 미답변 (`answered_at IS NULL`) |
| 소프트 삭제 | `leaver` 계정 `status='LEFT'` |
| DEV 계정 | `devops` — 이 스크립트로만 생성됨 |

### 9.7 적용 방법

```
mysql -D webapplication_sandbox < sql/schema.sql
mysql -D webapplication_sandbox < sql/data.sql
```

`context.xml` 이 이미 `webapplication_sandbox` 를 가리키므로 JSP 배선 트랙은 추가 설정이 필요 없다.
`sql/00_create_database.sql` 은 **이미 실행됐다.** 다시 돌릴 필요 없다.

### 9.8 `product.status` 2값 전환 (2026-10-29)

ERD 트랙이 `'품절'` 제거를 확정해(정본 § 2.4 갱신) 그대로 반영했다.

```sql
status ENUM('판매중', '숨김') NOT NULL DEFAULT '판매중'
```

**왜 뺐나** — `'품절'` 이 남아 있으면 `status` 와 `stock` 이 같은 사실을 두 번 말하게 된다.
동기화를 빠뜨린 코드 한 곳이 `status='품절'` + `stock=10` 인 모순 행을 만들고, DB 는 그걸 막지 못한다.
값을 없애면 **지킬 불변식 자체가 생기지 않는다.** ERD § 9.6 의 불변식은 7건 그대로다.

**노출 규칙** — 품절 배지를 띄우는 대신 탐색 경로에서 제외한다.

```sql
WHERE status = '판매중' AND stock > 0   -- 목록 · 검색 · 상품상세(직접 URL 포함)
```

장바구니 · 찜은 예외다. 사용자 자기 기록이므로 행은 보여주고 주문 대상에서만 뺀다.
주문내역은 `order_item` 스냅샷을 쓰므로 무관하다. **배선 트랙이 지켜야 하는 규약이다.**

**`숨김` 과 `stock=0` 은 다르다 — 합치면 안 된다**

| | `status='숨김'` | `stock=0` |
|---|---|---|
| 성격 | 관리자 의도 | 판매의 결과 |
| 재고가 들어오면 | **자동 복귀하지 않는다** | **자동으로 다시 노출된다** |

합치면 숨겨 둔 상품이 입고만으로 다시 노출돼 관리자 결정이 뒤집힌다.

**동반 수정**

| 대상 | 변경 |
|---|---|
| `schema.sql` `product` | ENUM 2값 + 노출 규칙 주석 |
| `schema.sql` `v_low_stock` | `p.status <> '숨김'` → `= '판매중'` (2값에서 동치, 의미가 명확한 쪽) |
| `data.sql` 3번 상품 | `'품절'` → `'판매중'` + `stock=0` 유지 (노출 규칙 시연 표본) |
| `data.sql` 7번 상품 | `stock` 25 → 5 (임계값 경계 표본 추가) |

**검증**

| 항목 | 결과 |
|---|---|
| 실제 ENUM 정의 | ✅ `enum('판매중','숨김')` |
| `'품절'` INSERT 시도 | ✅ 거부 (`ERROR 1265 Data truncated`) |
| 탐색 경로 노출 | ✅ 6건 — 3번(재고 0)·8번(숨김) 제외 |
| `v_low_stock` | ✅ 3건 — 3번(0) · 5번(3) · 7번(5). 8번은 숨김이라 제외 |
| `schema` → `data` 재실행 | ✅ 2회 무오류 |

`v_low_stock` 이 **선택 기능이 아니게 됐다.** 재고 0 상품은 사용자 화면에서 조용히 사라지므로,
관리자가 그 사실을 아는 유일한 경로가 이 뷰다.

### 9.9 🔴 한글 이중 인코딩 — 발견과 복구 (2026-10-29)

**증상** — DBeaver 에서 한글이 깨져 보였다. CLI 에서는 멀쩡해 보였다.

**진단** — 저장된 바이트가 실제로 깨져 있었다.

| 값 | 정상 | 실제 (복구 전) |
|---|---|---|
| `'베이식 반팔 티셔츠'` 길이 | 10자 / 26 byte | **26자 / 55 byte** |
| 첫 글자 HEX | `EBB2A0` (`베`) | **`C3AB`** (`ë`) |

**원인** — 접속 문자셋이 `latin1` 이었다.

```
character_set_client     latin1      <- 범인
character_set_connection latin1
character_set_results    latin1
character_set_database   utf8mb4     <- DB 는 정상
```

Homebrew `mysql` 클라이언트의 **컴파일 기본값**이 `latin1` 이다. 설정 파일 문제가 아니라
`~/.my.cnf` 에 `default-character-set` 이 **없어서** 기본값이 적용된 것이다.

UTF-8 파일을 그 접속으로 밀어 넣으면 서버가 바이트를 latin1 로 해석해 utf8mb4 로 **다시**
인코딩한다. `베`(`EB B2 A0`) → `ë`+`²`+`NBSP` → `C3AB C2B2 C2A0`. 이중 인코딩이다.

**CLI 에서 멀쩡해 보인 이유** — 같은 latin1 접속으로 읽으면 변환이 정확히 역으로 일어나
원래 글자로 되돌아온다. **왕복이 성립해서 깨진 걸 모른다.** UTF-8 로 접속하는 DBeaver·JDBC 에서만
정체가 드러난다. 이 조합이 가장 위험하다 — 검증을 CLI 로만 하면 통과한다.

**조치 — 세 지점에 고정**

| 지점 | 조치 | 효과 |
|---|---|---|
| `sql/schema.sql` · `sql/data.sql` | 선두에 `SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;` | **누가 어떤 클라이언트로 돌려도** 세션 문자셋이 고정된다. 가장 중요한 방어선 |
| `~/.my.cnf` `[client]` | `default-character-set=utf8mb4` | CLI 접속 전반 |
| `META-INF/context.xml` · `.example` | URL 에 `connectionCollation=utf8mb4_unicode_ci` 추가 | JDBC 커넥션 풀 |

`SET NAMES` 를 SQL 파일에 넣은 것이 핵심이다. `~/.my.cnf` 는 이 머신·이 계정에만 적용되지만,
SQL 파일 안의 선언은 **파일을 따라다닌다.** DBeaver·HeidiSQL 로 열어 실행해도 동일하게 보장된다.

**복구 검증**

| 항목 | 결과 |
|---|---|
| 전면 재적재 후 바이트 | ✅ `EBB2A0` / 10자 / 26 byte — 정상 |
| 12개 테이블 텍스트 컬럼 mojibake 스캔 | ✅ **0건** |
| 한글 ENUM (`판매중` `배송완료` `간편결제` …) | ✅ 정상 집계 |
| 한글 `LIKE '%스니커즈%'` 검색 | ✅ 정상 |
| 뷰 한글 집계 (`v_delivery_status_count`) | ✅ 정상 |
