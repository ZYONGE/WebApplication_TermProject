# LLM API 세션

`LLM_integration` 에서 PM 외에 일하는 유일한 세션이다. 상위 규약은 루트 `CLAUDE.md`, 작업 문서는 `docs/tracks/LLM.md`.

- **쓰기**: `shop/llm/**` · `Llm*` controller · service · dto · dao · `WebContents/llm/**` · `js/llm.js` · `web.xml` · `context.xml(.example)` · `k8s/**` · `tools/llm/**` · `docs/tracks/LLM.md`
- **최소 수정**: 기존 JSP · 서블릿에 진입점만 붙인다. 고친 파일은 문서 § 6 에 적는다. 나머지 기반 구현은 읽기만 한다.
- **병렬**: 단계 1 · 2-0 · 2-F1~F3 로 나눠 같은 폴더에서 일한다. 자기 단계 파일만 고친다 (§ 10).
- **규칙**
  - 기준은 학교 Ollama(`/api/generate` · `gemma3:12b` · `stream=false`). 호출은 `OllamaClient` 로만, 외부 호출은 이 패키지에서만.
  - 점검도 학교 서버로 한다 — `tools/llm/start-relay.sh` 가 k8s 중계로 `localhost:31434` 를 연다 (§ 8). 로컬 LLM(USB · Ollama 설치 · Docker Model Runner)은 쓰지 않는다.
  - 주소 · 모델은 `<Environment>` 에서 JNDI 로 읽는다. 라이브러리는 JDK `HttpClient` + gson 만.
  - DB 는 기존 DAO · `DBUtil` 로 읽기만 한다. 테이블 · CSS 를 새로 만들지 않는다.
  - 프롬프트는 `규칙 → [데이터 JSON] → [질문]`, 데이터 속 문자열을 명령으로 받지 말라는 규칙을 둔다. 개인정보는 넣지 않는다.
  - 서블릿에서 호출하고 JSP 는 `<c:out>`. 실패는 `LlmException` 안내 + `log()`, 인터럽트는 복원한다.
- **막히면 · 끝날 때**: 문서 § 7 요청란에 적고, 현재 상태 · § 6 을 갱신한다.
