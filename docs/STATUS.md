# 현황 보드

> PM만 수정한다. 이 브랜치(`zyonge_LLMIntegration`)는 LLM 개발만을 위해 존재하므로 LLM API 세션만 다룬다. 상세는 `docs/tracks/LLM.md`.
> 기반 구현(ERD · DB · JSP · UI)은 샌드박스 `a835e26` 그대로이며, 그 트랙들의 현황과 결정 대기는 샌드박스 `docs/STATUS.md` 에 있다.

## LLM API

| 항목 | 상태 |
|---|---|
| 작업 폴더 | `src/main/java/shop/llm/` · `WebContents/llm/` |
| 공급자 · 모델 | 수업 실습용 Ollama 서버 · `gemma3:12b` (키 없음) |
| 접속 | Docker + 쿠버네티스로 개발 예정 (구성은 강의 안내 대기). 임시로 Docker 터널 골격 `docker/llm-tunnel` → `localhost:31434` |
| 진행 | 🟡 `OllamaClient` 이식 · gson 추가 완료, 기능 3종 파일 골격 완료 · 구현 전 |

## PM 결정 대기

| # | 항목 | 요청 |
|---|---|---|
| L4 | Docker + 쿠버네티스 구성 — 클러스터 제공처 · 올릴 대상(앱 전체 / LLM 중계) · 접속 방법. 앱 전체면 "배포 금지" 원칙 개정 필요 | LLM · PM |
| L3 | 호출 횟수 제한 방식 — 제안: 메모리 캐시 + 동시 2건 제한 (`docs/tracks/LLM.md` § 5) | LLM |

## 합병 대비 (샌드박스 · main 재구현 시)

트랙별 작업 목록은 `docs/tracks/LLM.md` § 9. 순서: DB 샘플 보강 → UI 클래스 계약 → ERD · JSP 이식 → PM 원칙 개정.

| 트랙 | 요약 |
|---|---|
| DB | `data.sql` 에 긴 상품 설명 · 상품당 리뷰 여러 건(주문 체인 포함) · 상품 수 확대 — 없으면 상품 요약 · 리뷰 요약 시연 불가 |
| ERD | `CRUD.md` · URL 계약에 `/llm/summary` · `/llm/related` 추가 |
| JSP | LLM 코드 편입, `ProductDAO` 후보 조회 메서드, 상품 화면 진입점 재적용 |
| UI | 요약 상자 · 생성 중 · AI 생성 표시 · 연관 상품 구역 클래스 계약과 목업 |
| PM | 원칙 개정(외부 API 예외 · gson · Docker/k8s · 배포), lib · `.gitignore` |

## 결정 기록

| # | 결정 |
|---|---|
| L1 | 공급자 · 모델 = 수업 실습용 Ollama 서버 · `gemma3:12b`. JSON 처리를 위해 gson 2.15.0을 `WEB-INF/lib` 에 추가 (이 브랜치 한정) |
| L2 | 기능 3종 = ① 상품 설명 요약(3문장 이내) ② 리뷰 요약(다수 의견 정리) ③ 연관 검색(키워드에 해당 · 인접한 상품). 셋 다 공개 상품 화면 |

## 인지 사항

- LLM 서버는 학교 IP 요청만 받는 평문 HTTP 서버다. 학교망 밖 개발은 Docker + 쿠버네티스 환경으로 해결할 예정이고, 개인정보는 프롬프트에 넣지 않는다.

- 기반 구현의 DB 스키마는 `webapplication_sandbox` 다. 샌드박스와 같은 DB를 쓰므로 LLM 기능은 데이터를 읽기만 한다.
- 로컬 MySQL root 비밀번호가 빈 값이다. 머신 전체에 영향.
- Homebrew mysql 클라이언트 기본 문자셋이 latin1이라 한글이 이중 인코딩된다. `~/.my.cnf` 에 utf8mb4 설정 완료.
