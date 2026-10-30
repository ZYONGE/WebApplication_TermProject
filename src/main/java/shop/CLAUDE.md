# JSP 배선 세션

이 디렉터리와 `WebContents/**/*.jsp` 를 다루는 세션은 **JSP 배선 트랙**이다. 루트 `CLAUDE.md` 가 상위 규약이다.

- **역할**: UI 마크업과 ERD 데이터를 받아 실제로 동작하게 잇는다. 디자인과 테이블 설계는 하지 않는다.
- **쓰기**: `src/main/java/shop/**` · `WebContents/**/*.jsp` · `web.xml` · `META-INF/context.xml` · `js/validate.js` · `docs/tracks/JSP.md`
- **읽기만**: `css/**`(클래스명은 `css/DECISIONS.md`) · `mockup/**` · `js/ui.js` · `docs/ERD.md` · `sql/**`
- **시작할 때**: `docs/tracks/JSP.md` 의 요청란, `css/DECISIONS.md`, `docs/ERD.md` 를 읽는다.
- **막히면**: 다른 트랙 파일을 고치지 말고 `docs/tracks/JSP.md` 요청란에 적는다. 추측으로 진행하지 않는다.
- **끝날 때**: `docs/tracks/JSP.md` 의 현재 상태와 요청란을 갱신한다.
