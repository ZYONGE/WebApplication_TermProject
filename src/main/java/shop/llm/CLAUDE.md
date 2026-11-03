# LLM API 세션

이 디렉터리와 LLM 연동 파일을 다루는 세션은 **LLM API 세션**이다. `zyonge_LLMIntegration` 브랜치에서 PM 외에 일하는 유일한 세션이며, 루트 `CLAUDE.md` 가 상위 규약이다.

- **역할**: LLM API 모델을 이식하고, 그 기능 로직을 짜고, JSP 화면에 연결한다. ERD · DB 스키마 · UI 디자인은 하지 않는다.
- **쓰기**: `src/main/java/shop/llm/**` · `Llm*` 접두어의 controller · service · dto · `WebContents/llm/**` · `js/llm.js` · `web.xml` · `META-INF/context.xml(.example)` · `docs/tracks/LLM.md`
- **최소 수정 허용**: 기존 JSP · 서블릿에 LLM 진입점(링크 · 팝업 · 호출 한 줄)을 붙이는 것. 고친 파일은 `docs/tracks/LLM.md` 의 기존 파일 수정 기록에 남긴다.
- **읽기만**: `docs/ERD.md` · `docs/CRUD.md` · `sql/**` · `css/**`(클래스명은 `css/DECISIONS.md`) · `mockup/**` · `js/ui.js` · 기존 Java · JSP (기반 구현)
- **규칙**
  - 라이브러리를 추가하지 않는다. HTTP 호출은 JDK 17 `java.net.http.HttpClient`, JSON 조립 · 파싱은 이 패키지 안에서 직접 한다.
  - API 키 · 모델명 · 엔드포인트는 `META-INF/context.xml` 의 `<Environment>` 에서 JNDI 로 읽는다. 소스 · 커밋 · 문서 · 로그에 키를 쓰지 않는다.
  - 외부 호출은 이 패키지 안에서만 한다. 서블릿 · JSP 는 `LlmService` 를 거친다.
  - 테이블 · 컬럼을 새로 만들지 않는다. 저장이 필요하면 세션 · 메모리로 버티고 요청란에 적는다.
  - 새 CSS 를 쓰지 않는다. 기존 클래스와 `<dialog>` 팝업 패턴만 쓴다.
  - 호출 실패(키 없음 · 타임아웃 · 4xx/5xx)는 화면을 깨뜨리지 않고 안내 문구로 대신한다.
- **막히면**: 스키마 · 디자인 변경이 필요하면 직접 하지 말고 `docs/tracks/LLM.md` 요청란에 적는다.
- **끝날 때**: `docs/tracks/LLM.md` 의 현재 상태 · 기존 파일 수정 기록 · 요청란을 갱신한다.
