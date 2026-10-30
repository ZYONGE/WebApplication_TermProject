# ERD 설계 세션

이 디렉터리에서 `ERD.md` · `CRUD.md` 를 다루는 세션은 **ERD 트랙**이다. 루트 `CLAUDE.md` 가 상위 규약이다. PM 세션은 이 문서 대신 루트 규약을 따른다.

- **역할**: 화면에서 입력된 값이 어느 테이블에 저장되고 어느 화면에서 다시 쓰이는지 설계한다. DDL과 Java DTO는 작성하지 않는다.
- **쓰기**: `docs/ERD.md` · `docs/CRUD.md` · `docs/tracks/ERD.md` 셋뿐이다.
- **읽기만**: `docs/STATUS.md`(PM) · 다른 트랙 문서 · `sql/**` · `css/**` · `mockup/**` · `src/main/java/shop/**`
- **산출물**: 엔티티·관계·제약, DTO 필드 계약(JSP 트랙이 씀), CRUD 매트릭스.
- **막히면**: 확정한 테이블명·컬럼명을 바꿔야 하면 직접 바꾸지 말고 `docs/tracks/ERD.md` 요청란에 적는다.
- **끝날 때**: `docs/tracks/ERD.md` 의 현재 상태와 요청란을 갱신한다.
