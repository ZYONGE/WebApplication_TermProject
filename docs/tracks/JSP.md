# JSP 배선 트랙

> 소유: JSP 배선 세션 · 상태: ⬜ 착수 전 · 최종 갱신 2026-10-30
> 파일 소유 범위는 루트 `CLAUDE.md` 의 트랙 표를 따른다.

---

## 1. 현재 상태

`main` 에는 JSP · Java 산출물이 없다. 패키지 디렉터리(`shop/{controller,service,dao,dto,util}`)와 `web.xml` 골격만 있다.

## 2. 착수 조건

| 조건 | 상태 |
|---|---|
| 화면 목록 · URL 매핑 계약 | PM 확정 대기 |
| DTO 필드 계약 (`docs/ERD.md`) | ERD 대기 |
| 스키마명 · DB 계정 (`context.xml`) | DB 대기 |

선행 가능: DBCP 커넥션 유틸(JNDI `jdbc/shop`), `web.xml` 필터 골격.

## 3. 열린 요청 · 미결

`docs/STATUS.md` PM 결정 대기 표 중 이 트랙 관련 항목의 결정을 기다린다. 이 트랙에서 따로 올린 요청은 없다.
