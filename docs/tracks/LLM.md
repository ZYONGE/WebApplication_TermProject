# LLM API (LLM 브랜치 전용)

> 소유: LLM API 세션 · 상태: 🟡 기능 3종 파일 골격 완료 · 구현 전 · 최종 갱신 2026-11-03
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

호출 흐름: `JSP → js/llm.js → LlmServlet → LlmService → shop.llm (캐시 · 프롬프트 · 클라이언트) → LLM API`. DB가 필요하면 Service가 기존 DAO를 읽기 전용으로 쓴다.

### 파일 지도

| 파일 | 담당 | 상태 |
|---|---|---|
| `shop/llm/OllamaClient.java` | 공통 — Ollama 호출 (F3용 `format: "json"` 옵션 추가 예정) | 🟢 이식 |
| `shop/llm/LlmException.java` | 공통 — 화면용 안내 문구 + 원인 | 🟢 |
| `shop/llm/LlmCache.java` | 공통 — 메모리 캐시 · 동시 2건 제한 | ⬜ 골격 |
| `shop/llm/prompt/DescSummaryPrompt.java` | F1 상품 설명 요약 | ⬜ 골격 |
| `shop/llm/prompt/ReviewSummaryPrompt.java` | F2 리뷰 요약 | ⬜ 골격 |
| `shop/llm/prompt/RelatedSearchPrompt.java` | F3 연관 검색 | ⬜ 골격 |
| `shop/service/LlmService.java` | F1 · F2 · F3 업무 흐름 | ⬜ 골격 |
| `shop/controller/LlmServlet.java` | `/llm/summary` · `/llm/related` JSON | ⬜ 골격 (지금은 501 응답) |
| `WebContents/js/llm.js` | 비동기 호출 · 자리 채우기 | ⬜ 골격 |
| `product/view.jsp` · `product/list.jsp` | 진입점 (기존 파일) | ⬜ 미수정 |

`WebContents/llm/` 은 라우팅 페이지용 자리다. 확정된 3개 기능은 모두 기존 상품 화면에 붙으므로 지금은 비어 있다.

## 3. 현재 상태

| 항목 | 상태 |
|---|---|
| 공급자 · 모델 | 🟢 수업 실습용 Ollama 서버 · `gemma3:12b` (STATUS L1 결정) |
| 라이브러리 | 🟢 `WEB-INF/lib/gson-2.15.0.jar` 추가 |
| `shop/llm/` 클라이언트 | 🟢 `OllamaClient.generate(prompt)` · `LlmException` — javac 통과 |
| 기능 3종 파일 | 🟡 골격 — § 2 파일 지도. 전체 소스 javac 0 오류 |
| `context.xml.example` `<Environment>` | 🟢 `llm/ollamaUrl` · `llm/ollamaModel` |
| 기능 목록 · 화면 위치 | 🟢 확정 — 상품 설명 요약 · 리뷰 요약 · 연관 검색 (STATUS L2) |
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

## 5. 기능 목록 (L2 확정)

PM이 전달한 3개 기능이다. 셋 다 공개 상품 화면에 붙고, 비로그인 · USER 모두 본다.

| # | 기능 | 화면 | 입력 데이터 | 출력 |
|---|---|---|---|---|
| F1 | 상품 설명 요약 | `/product/view` — 설명(`prod_desc`) 위 | `product.description` | 3문장 이내 요약 |
| F2 | 리뷰 요약 | `/product/view` — 리뷰(`prod_review`) 위 | 그 상품의 `review.rating` · `content` | 다수 의견을 하나로 정리한 요약 |
| F3 | 연관 검색 | `/product/list?keyword=` — 기존 검색 결과 아래 | 검색어 + 판매중 상품 목록(id · 이름 · 카테고리) | 키워드에 해당하거나 인접한 상품 목록 |

### 공통 설계

- **화면을 기다리게 하지 않는다.** 응답이 최대 180초라 서블릿 렌더 안에서 부르지 않는다. 페이지는 기존대로 즉시 그리고, `js/llm.js` 가 JSON 엔드포인트를 비동기로 불러 자리를 채운다. 실패하면 자리를 숨기고 기존 화면은 그대로 둔다.
- **엔드포인트**: `/llm/summary?type=desc|review&productId=` · `/llm/related?keyword=` (정확 일치 매핑, 응답은 gson JSON). 서블릿 `LlmServlet`, 업무 `LlmService`.
- **캐시**: 테이블을 만들 수 없으므로 서버 메모리(`ConcurrentHashMap`)에 둔다. 키는 F1 = 상품 id + 설명 해시, F2 = 상품 id + 리뷰 수 + 최신 리뷰 시각, F3 = 정규화한 검색어. 원본이 바뀌면 키가 바뀌어 자동으로 다시 만든다. Tomcat 재시작 시 사라진다.
- **동시 호출 제한 (L3 제안)**: 공용 GPU 서버라 동시에 2건까지만 보내고(`Semaphore`), 같은 키를 이미 생성 중이면 새로 보내지 않는다.
- **개인정보**: 리뷰는 별점 · 본문만 보내고 작성자는 보내지 않는다.

### 기능별 규칙

| # | 호출하지 않는 경우 | 프롬프트 핵심 | 결과 검증 |
|---|---|---|---|
| F1 | 설명이 비었거나 짧으면(약 150자 이하) 원문을 그대로 쓴다 | "아래 설명만 근거로 3문장 이내 · 없는 사양 추측 금지" | 문장 수가 넘치면 앞 3문장만 |
| F2 | 리뷰가 2건 미만 | "공통 의견 · 다수 의견 위주, 소수 의견은 따로 한 줄" + 별점 분포는 DB 집계값으로 제공 | 평균 · 건수는 LLM이 아니라 DB 값을 화면에 쓴다 |
| F3 | 검색어가 비었을 때 | 후보 상품 JSON 을 주고 "관련 있는 상품 id 만 JSON 배열로" — Ollama `format: "json"` 사용 | 후보 목록에 없는 id 는 버린다. 이미 LIKE 결과에 있는 상품은 빼고 최대 8개 |

F3 때문에 `OllamaClient` 에 JSON 응답 형식 옵션을 추가한다.

### 기존 파일에 붙일 진입점 (구현 시 § 6 에 기록)

- `product/view.jsp` — F1 · F2 자리 두 곳 + `js/llm.js`
- `product/list.jsp` — F3 자리 한 곳 + `js/llm.js`

## 6. 기존 파일 수정 기록

LLM 진입점을 붙이려고 기반 구현 파일을 고친 경우만 적는다. `main` 재구현 때 이 표가 이식 목록이 된다.

| 파일 | 수정 내용 | 커밋 |
|---|---|---|
| — | — | — |

## 7. 요청 · 미결

| 대상 | 내용 | 상태 |
|---|---|---|
| PM | 공급자 · 모델 (L1) | 🟢 결정 — 수업 Ollama · `gemma3:12b` |
| PM | 기능 목록과 붙일 화면 · 역할 (L2) | 🟢 결정 — § 5 |
| PM | 호출 횟수 제한 방식 (L3) — 메모리 캐시 + 동시 2건 제한 제안 (§ 5) | 승인 대기 |
