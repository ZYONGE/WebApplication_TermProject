# 현황 보드

> PM만 수정한다. 이 브랜치(`zyonge_LLMIntegration`)는 LLM 개발만을 위해 존재하므로 LLM API 세션만 다룬다. 상세는 `docs/tracks/LLM.md`.
> 기반 구현(ERD · DB · JSP · UI)은 샌드박스 `a835e26` 그대로이며, 그 트랙들의 현황과 결정 대기는 샌드박스 `docs/STATUS.md` 에 있다.

## LLM API

| 항목 | 상태 |
|---|---|
| 작업 폴더 | `src/main/java/shop/llm/` · `WebContents/llm/` |
| 진행 | ⬜ 착수 전 |

## PM 결정 대기

| # | 항목 | 요청 |
|---|---|---|
| L1 | LLM 공급자 · 모델 · 키 발급 주체 | LLM |
| L2 | 기능 목록과 붙일 화면 · 역할 (USER / ADMIN / DEV) | LLM |
| L3 | 호출 비용 · 횟수 제한 방식 | LLM |

## 인지 사항

- 기반 구현의 DB 스키마는 `webapplication_sandbox` 다. 샌드박스와 같은 DB를 쓰므로 LLM 기능은 데이터를 읽기만 한다.
- 로컬 MySQL root 비밀번호가 빈 값이다. 머신 전체에 영향.
- Homebrew mysql 클라이언트 기본 문자셋이 latin1이라 한글이 이중 인코딩된다. `~/.my.cnf` 에 utf8mb4 설정 완료.
