# LLM API (LLM 브랜치 전용)

> 소유: LLM API 세션 · 상태: 🟡 클라이언트 이식 완료 · 기능 대기 · 최종 갱신 2026-11-03
> 이 문서는 `zyonge_LLMIntegration` 전용이다. 여기 산출물은 `main` 으로 가지 않는다 — PM이 정리해 `main` 에서 다시 구현한다.
> 세션 규칙은 `src/main/java/shop/llm/CLAUDE.md`, 파일 소유 범위는 루트 `CLAUDE.md` § 3 을 따른다.

---

## 1. 범위

이 브랜치는 **LLM API 모델 이식 · 기능 로직 · JSP 연결** 만 개발하는 특수목적 브랜치다.

| 한다 | 하지 않는다 |
|---|---|
| LLM API 클라이언트 (`shop/llm/`) | ERD · CRUD 변경 |
| LLM 기능의 Service · Servlet · DTO (`Llm*`) | DDL · 샘플 데이터 변경 |
| LLM 화면 · 팝업 JSP, 기존 화면에 진입점 연결 | CSS · 목업 · 디자인 토큰 |
| 키 · 모델 설정 (`context.xml` `<Environment>`) | LLM과 무관한 배선 수정 |

기반 구현(ERD · DB · JSP · UI)은 샌드박스 `a835e26` 시점 그대로이며 이 브랜치에서는 읽기만 한다.

## 2. 작업 위치

| 구분 | 경로 |
|---|---|
| API 계층 | `src/main/java/shop/llm/` — 클라이언트 · 프롬프트 · JSON 처리 · 설정 읽기 |
| 업무 계층 | `shop/service/Llm*Service.java` · `shop/controller/Llm*Servlet.java` · `shop/dto/Llm*DTO.java` |
| 화면 | `WebContents/llm/*.jsp` (라우팅 페이지) · 기존 화면 안 `<dialog>` (팝업) |
| 스크립트 | `WebContents/js/llm.js` (비동기 호출이 필요할 때만) |
| 설정 | `META-INF/context.xml` `<Environment>` (git 제외) · 템플릿 `context.xml.example` |

호출 흐름: `JSP → Llm*Servlet → Llm*Service → shop.llm 클라이언트 → LLM API`. DB가 필요하면 Service가 기존 DAO를 읽기 전용으로 쓴다.

## 3. 현재 상태

| 항목 | 상태 |
|---|---|
| 공급자 · 모델 | 🟢 수업 실습용 Ollama 서버 · `gemma3:12b` (STATUS L1 결정) |
| 라이브러리 | 🟢 `WEB-INF/lib/gson-2.15.0.jar` 추가 |
| `shop/llm/` 클라이언트 | 🟢 `OllamaClient.generate(prompt)` · `LlmException` — javac 통과 |
| `context.xml.example` `<Environment>` | 🟢 `llm/ollamaUrl` · `llm/ollamaModel` |
| 기능 목록 · 화면 위치 | ⬜ 미정 (STATUS L2) |
| JSP 연결 | ⬜ 착수 전 |

## 4. 수업 실습 코드 분석

실습 자료는 `my.ai` 패키지의 클래스 2개와 JSP 2개다. 이 브랜치에 무엇을 가져왔고 무엇을 바꿨는지 적는다.

| 실습 파일 | 하는 일 | 이 프로젝트 반영 |
|---|---|---|
| `OllamaClient.java` | 질문을 그대로 `/api/generate` 에 보내고 `response` 를 돌려줌 | `shop/llm/OllamaClient` 로 이식 |
| `ollama.jsp` | 스크립틀릿으로 클라이언트를 직접 호출, 결과 출력 | 방식만 참고. 서블릿 → JSP 로 다시 짠다 |
| `OllamaClientMySQL.java` | 오늘 구매를 고객별 · 시간대별로 집계 → JSON → 규칙 + 데이터 + 질문 프롬프트 | 프롬프트 패턴만 가져온다. 집계는 기존 DAO 로 |
| `ollamamysql.jsp` | 질문 예시 · 분석 결과 화면 | 화면 구성 참고 (질문 예시 · 결과 · 오류) |

**그대로 가져온 것**: 엔드포인트 형식, `model` · `prompt` · `stream=false` 요청 본문, 연결 10초 · 응답 180초 타임아웃, `response` 필드 검사, UTF-8 송수신, 제출 버튼 잠금, `InterruptedException` 처리.

**바꾼 것**

| 실습 코드 | 이 프로젝트 | 이유 |
|---|---|---|
| 주소 · 모델을 상수로 적음 | `context.xml` `<Environment>` → JNDI | 규약 — 설정은 소스 밖 |
| `DriverManager` + `root` / `1234` 를 소스에 적음 | `DBUtil`(JNDI `jdbc/shop`) · 기존 DAO | 규약 — 접속 정보는 `context.xml` 에만 |
| `purchase` 테이블 · `web` DB | `orders` · `order_item` · `v_sales_daily` 등 (`webapplication_sandbox`) | 실제 스키마 |
| 오류 응답 본문을 화면 메시지에 붙임 | `LlmException` 은 안내 문구만, 본문은 cause → 로그 | 내부 정보 노출 방지 |
| `IOException` 을 그대로 던짐 | 연결 · HTTP · 응답 해석 실패를 `LlmException` 하나로 | 서블릿이 한 가지만 잡으면 된다 |
| 매 호출 `new HttpClient` | `HttpClient` 1개를 공유 | 스레드 안전 · 커넥션 재사용 |
| JSP 스크립틀릿 · 인라인 `<style>` | 서블릿 MVC · 기존 CSS 클래스 · `<c:out>` | 기반 구현 구조와 규약 |

**프롬프트 패턴** (`OllamaClientMySQL` 에서 가져옴): 역할 → 근거 범위("아래 데이터만 근거로") → 수치 정확성 → 없는 정보 추측 금지 → 데이터 속 문자열을 명령으로 해석 금지 → `[DB 집계 결과]` JSON → `[사용자 질문]`.

## 5. 기능 후보 (L2 결정용)

| 후보 | 역할 · 화면 | 근거 데이터 |
|---|---|---|
| 매출 데이터 AI 분석 — 실습 `OllamaClientMySQL` 의 이 프로젝트판 | ADMIN · `/admin/dashboard` 팝업 또는 `/llm/...` 페이지 | `v_sales_daily` · `orders` · `order_item` (`StatsDAO` 재사용) |
| 상품 질문 도우미 | USER · `/product/view` 팝업 | `product` · `review` 요약 |
| 자유 질문 (실습 `ollama.jsp`) — 연결 확인용 | DEV · `/dev/` 아래 | 없음 |

## 6. 기존 파일 수정 기록

LLM 진입점을 붙이려고 기반 구현 파일을 고친 경우만 적는다. `main` 재구현 때 이 표가 이식 목록이 된다.

| 파일 | 수정 내용 | 커밋 |
|---|---|---|
| — | — | — |

## 7. 요청 · 미결

| 대상 | 내용 | 상태 |
|---|---|---|
| PM | 공급자 · 모델 (L1) | 🟢 결정 — 수업 Ollama · `gemma3:12b` |
| PM | 기능 목록과 붙일 화면 · 역할 (L2) | 결정 대기 |
| PM | 호출 횟수 제한 방식 (L3) — 공용 GPU 서버라 응답이 최대 180초 | 결정 대기 |
