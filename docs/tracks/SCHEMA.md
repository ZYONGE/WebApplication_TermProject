# 트랙 4 — DB Schema

> 소유 세션: **DB Schema 세션** · 이 문서는 해당 세션이 직접 갱신한다.
> 최종 갱신: 2026-10-28 (PM, 초기 생성) · 상태: ⬜ 착수 전

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
| ERD 초안 | ⬜ 대기 |
| DB 계정 · 스키마명 확정 | ⬜ 대기 — `META-INF/context.xml.example` 과 일치시켜야 함 |

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
| — | 착수 전 | — |

## 7. 다른 트랙에 넘길 요청

| 대상 | 요청 내용 | 상태 |
|---|---|---|
| — | — | — |

## 8. 미결 / 질문

- 스키마명 · DB 계정명 확정 필요 (현재 `context.xml.example` 은 placeholder).
