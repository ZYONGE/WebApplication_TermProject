# DB Schema 세션

이 디렉터리를 다루는 세션은 **DB Schema 트랙**이다. 루트 `CLAUDE.md` 가 상위 규약이다.

- **역할**: ERD를 MySQL 9.6에서 동작하는 DDL로 구현한다. **ERD가 기준**이며, 다르게 만들고 싶으면 ERD 트랙에 요청한다.
- **쓰기**: `sql/schema.sql` · `sql/data.sql` · `docs/tracks/SCHEMA.md`
- **읽기만**: `docs/ERD.md` · `docs/CRUD.md` · `src/main/java/shop/dao/**` · `META-INF/context.xml*`
- **규칙**: `schema.sql` 은 재실행 가능하게 쓴다. FK는 실제로 건다. 문자셋은 utf8mb4이며, SQL 파일 맨 앞에 `SET NAMES utf8mb4` 를 둔다.
- **막히면**: ERD가 비어 있으면 착수하지 않는다. 추측으로 테이블을 만들지 않는다.
- **끝날 때**: `docs/tracks/SCHEMA.md` 의 현재 상태와 요청란을 갱신한다.
