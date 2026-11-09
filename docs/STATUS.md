# 현황 보드

> PM 만 수정한다. 이 브랜치는 LLM 개발 전용이다. 상세는 `docs/tracks/LLM.md`, 기반 구현 트랙의 현황은 샌드박스 `docs/STATUS.md`.

## LLM API

- 모델: 수업 Ollama · `gemma3:12b`. 연결은 GPU 클러스터 중계 파드 + port-forward(`tools/llm/start-relay.sh`) — `check-server.sh` PASS (2026-11-09).
- 진행: `OllamaClient` 이식 · 기능 3종 골격 · 단계 1(실제 연결) 완료. 다음은 2-0(공통 선행), 이어 2-F1~F3 병렬 (§ 10).

## PM 결정 대기

| # | 항목 |
|---|---|
| L3 | 호출 제한 — 제안: 메모리 캐시 + 동시 2건 (§ 5) |

## 결정 기록

| # | 결정 |
|---|---|
| L1 | 수업 Ollama · `gemma3:12b`, gson 2.15.0 추가 (이 브랜치 한정) |
| L2 | 기능 3종 — 상품 설명 요약 · 리뷰 요약 · 연관 검색, 공개 상품 화면 |
| L4 | 수업 GPU 클러스터(학번 namespace)에 중계 파드만 올리고 port-forward 로만 접근. 앱은 로컬 Tomcat 유지라 "배포 금지" 그대로 (2026-11-09, § 8) |
| L5 | ~~점검은 USB 소형 모델~~ → 폐기. 로컬 LLM 점검은 전부 말소하고 점검도 학교 서버로 한다 (2026-11-09) |

## 합병 대비

DB 샘플 보강(없으면 요약 2종 시연 불가) → UI 클래스 계약 → ERD · JSP 이식 → PM 원칙 개정. 상세 § 9.

## 인지 사항

- LLM 서버는 학교 IP 전용 · 평문 HTTP. 개인정보는 프롬프트에 넣지 않는다.
- DB 스키마 `webapplication_sandbox` 를 샌드박스와 함께 쓴다. LLM 은 읽기만.
- 로컬 MySQL root 비밀번호가 빈 값. Homebrew mysql 클라이언트는 `~/.my.cnf` 로 utf8mb4 설정 완료.
