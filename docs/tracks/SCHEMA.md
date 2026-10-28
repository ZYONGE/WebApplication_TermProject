# 트랙 4 — DB Schema

> 소유 세션: **DB Schema 세션** · 이 문서는 해당 세션이 직접 갱신한다.
> 세션 생성 직후 읽을 역할·권한 안내: `sql/CLAUDE.md` (작업 디렉터리에서 자동 로드)
> 최종 갱신: 2026-10-28 (DB Schema 세션, 착수 조건 확인) · 상태: ⬜ 착수 전 — **ERD 초안 대기**

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
| ERD 초안 | ⬜ **대기** — `docs/ERD.md` 0 byte 확인 (2026-10-28). 추측 테이블 생성 금지(`sql/CLAUDE.md` §7-4) |
| DB 계정 · 스키마명 확정 | ⬜ 대기 — `META-INF/context.xml.example` 이 `CHANGE_ME_SCHEMA` / `CHANGE_ME_USER` placeholder 상태 |

## 4. 작업 규칙

- **ERD를 그대로 반영한다.** 스키마와 ERD가 어긋나면 ERD를 기준으로 맞춘다. (임의 변경 금지)
- 담당 범위는 **데이터의 저장**이다. (공지 항목 8)
- MySQL 9.6.0 기준. 문자셋은 `utf8mb4` / `utf8mb4_unicode_ci` 권장.
- `schema.sql`은 **재실행 가능**하게 작성한다. (`DROP TABLE IF EXISTS` → `CREATE TABLE`)
- FK 제약을 실제로 선다. 참조 무결성을 애플리케이션에만 맡기지 않는다.
- `data.sql`은 화면 확인에 충분한 최소 샘플만. 실제 개인정보·외부 데이터 사용 금지.
- 스키마명 / 계정 / 비밀번호는 `WebContents/META-INF/context.xml` 과 **반드시 일치**시키고, 값은 `context.xml.example`에 placeholder로만 남긴다.

## 5. Done 기준

- [ ] `schema.sql` 전체 테이블 DDL — 재실행 시 오류 없음
- [ ] `data.sql` 샘플 데이터 — FK 순서 고려한 INSERT 순서
- [ ] DBeaver / HeidiSQL 에서 실제 적용 성공
- [ ] ERD ↔ 스키마 1:1 대조 완료 (불일치 0건)
- [ ] `context.xml.example` 의 스키마명·계정과 일치 확인
- [ ] README `DDL` · `테이블 명세` 섹션 내용 PM에 인계

## 6. 진행 상황 로그

| 날짜 | 작업 | 산출물 |
|---|---|---|
| 2026-10-28 | 세션 개설. 역할·권한 범위 학습 (`CLAUDE.md`, `sql/CLAUDE.md`, 이 문서), 착수 조건 점검 — `docs/ERD.md` · `docs/CRUD.md` 모두 0 byte 확인. 체크리스트 §2-2 에 따라 **DDL 미착수** | 이 문서 §3·§7·§8 갱신 |

## 7. 다른 트랙에 넘길 요청

| 대상 | 요청 내용 | 상태 |
|---|---|---|
| ERD 트랙 | **ERD 초안 필요.** 이 트랙의 유일한 착수 조건. 최소 요구: 엔티티 목록 + 속성·타입 + PK/FK/UNIQUE/NOT NULL + 관계·카디널리티(N:M 해소 테이블 포함). 초안 단계에서도 **테이블명·컬럼명은 확정값**으로 주어야 DDL을 쓸 수 있다 | ⬜ 대기 |
| PM | **스키마명 · DB 계정명 확정 요청.** `schema.sql` 의 `CREATE DATABASE` 와 `context.xml` 의 `url`/`username` 이 일치해야 연결된다. 제안값: 스키마 `shopdb`, 계정 `shopuser` (확정은 PM) | ⬜ 대기 |
| PM (참고) | 로컬 MySQL `9.6.0` (Homebrew, arm64) 확인됨 — DDL 적용 검증을 CLI 로도 수행 가능. Done 기준의 DBeaver/HeidiSQL 적용은 별도 유지 | ℹ️ 정보 |

## 8. 미결 / 질문

- **ERD 초안 대기** — `docs/ERD.md` 가 비어 있어 착수하지 않았다. 추측으로 테이블을 만들지 않는다(`sql/CLAUDE.md` §7-4). ERD 초안이 올라오는 즉시 `schema.sql` → `data.sql` 순으로 착수한다.
- 스키마명 · DB 계정명 확정 필요 (현재 `context.xml.example` 은 placeholder). §7 로 PM에 요청했다.
- ERD 확정 전까지 보류하는 판단 항목 (ERD 초안을 받은 뒤 ERD 기준으로 맞춘다):
  - 금액 타입 — `DECIMAL(10,0)` vs `INT` (원 단위 정수 가정)
  - 주문 시점 상품명·가격의 **스냅샷 보관 여부** (상품 수정·삭제 후 주문내역 표시 문제)
  - 회원 탈퇴 처리 — 물리 삭제 vs 상태 플래그 (주문 FK 가 걸리므로 ERD 결정 사항)
  - FK `ON DELETE` / `ON UPDATE` 동작 (CASCADE / RESTRICT)
