# 현황 보드

> PM만 수정한다. 이 브랜치(`zyonge_LLMIntegration`)는 LLM 개발만을 위해 존재하므로 LLM API 세션만 다룬다. 상세는 `docs/tracks/LLM.md`.
> 기반 구현(ERD · DB · JSP · UI)은 샌드박스 `a835e26` 그대로이며, 그 트랙들의 현황과 결정 대기는 샌드박스 `docs/STATUS.md` 에 있다.

## LLM API

| 항목 | 상태 |
|---|---|
| 작업 폴더 | `src/main/java/shop/llm/` · `WebContents/llm/` |
| 공급자 · 모델 | 수업 실습용 Ollama 서버 · `gemma3:12b` (키 없음) |
| 진행 | 🟡 `OllamaClient` 이식 · gson 추가 완료, 기능 연결 대기 |

## PM 결정 대기

| # | 항목 | 요청 |
|---|---|---|
| L2 | 기능 목록과 붙일 화면 · 역할 (USER / ADMIN / DEV) | LLM |
| L3 | 호출 횟수 제한 방식 (공용 GPU 서버, 응답 최대 180초) | LLM |

## 결정 기록

| # | 결정 |
|---|---|
| L1 | 공급자 · 모델 = 수업 실습용 Ollama 서버 · `gemma3:12b`. JSON 처리를 위해 gson 2.15.0을 `WEB-INF/lib` 에 추가 (이 브랜치 한정) |

## 인지 사항

- LLM 서버는 학교 외부 주소 · 평문 HTTP다. 학교망 밖에서는 접속되지 않을 수 있고, 개인정보는 프롬프트에 넣지 않는다.

- 기반 구현의 DB 스키마는 `webapplication_sandbox` 다. 샌드박스와 같은 DB를 쓰므로 LLM 기능은 데이터를 읽기만 한다.
- 로컬 MySQL root 비밀번호가 빈 값이다. 머신 전체에 영향.
- Homebrew mysql 클라이언트 기본 문자셋이 latin1이라 한글이 이중 인코딩된다. `~/.my.cnf` 에 utf8mb4 설정 완료.
