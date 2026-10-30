# DB Schema 트랙 — 현황

> 소유: DB Schema 세션 · 상태: ⬜ **착수 전** (ERD 대기)

## 현재 상태

| 항목 | 상태 |
|---|---|
| `sql/schema.sql` · `sql/data.sql` | 비어 있음 — `docs/ERD.md` 가 비어 있어 착수하지 않음 |
| 스키마 | 미생성 — 이름 미확정 (제안 `shopdb`) |
| DB 권한 | 개발 계정 `2023314009`@`localhost` 는 전역 권한이 `USAGE` 뿐 → 스키마 생성·GRANT 는 관리자 계정으로 1회 실행 |
| 문자셋 | `utf8mb4` / `utf8mb4_unicode_ci`. SQL 파일 맨 앞에 `SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;` (클라이언트 기본값이 `latin1` 이라 없으면 한글이 이중 인코딩됨) |

## 적용 방법 (착수 후)

```
-- 관리자 1회
CREATE DATABASE <스키마> DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
GRANT ALL PRIVILEGES ON <스키마>.* TO '2023314009'@'localhost';
-- 개발 계정
mysql -D <스키마> < sql/schema.sql
mysql -D <스키마> < sql/data.sql
```

## 열린 요청 · 미결

| 대상 | 내용 |
|---|---|
| ERD | `docs/ERD.md` 초안 — 착수 조건 |
| PM | 스키마명 확정 (`docs/STATUS.md` P1) |
| JSP | `META-INF/context.xml.example` URL 에 `connectionCollation=utf8mb4_unicode_ci` 추가 (한글 깨짐 방지) |
