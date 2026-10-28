# JSP 배선 세션 — 역할과 권한 범위

> 이 디렉터리에서 작업하는 세션은 **JSP 배선(Wiring / Server) 트랙**이다.
> 상세 작업 지시·진행 로그·인계는 `docs/tracks/JSP.md`, 프로젝트 전체 규약은 루트 `CLAUDE.md`.
> PM 세션이면 이 문서 대신 루트 규약을 따른다.

---

## 1. 이 세션은 무엇인가

**페이지와 기능과 데이터를 연결하는 배선 담당.** (공지 항목 6)
화면을 예쁘게 만드는 것도, 테이블을 설계하는 것도 이 세션의 일이 아니다.
**UI가 만든 마크업**과 **ERD가 설계한 데이터**를 받아 **실제로 동작하게** 잇는 것이 전부다.

이 트랙은 **두 디렉터리 트리에 걸쳐 있다.**
- `src/main/java/shop/**` — Servlet · Service · DAO · DTO · Util
- `WebContents/{member,product,cart,order,mypage,admin,common}/*.jsp` + `index.jsp`

---

## 2. 즉시 시작 체크리스트

1. `docs/tracks/JSP.md` §3 착수 조건과 §7 넘어온 요청을 읽는다.
2. `WebContents/css/DECISIONS.md` 를 읽는다 — **마크업에 쓸 클래스명이 여기 있다.** 없으면 아직 UI 트랙이 확정 전이다.
3. `docs/ERD.md` 를 읽는다 — 비어 있으면 DTO를 **확정하지 말고**, 선행 가능한 작업(§4)만 한다.
4. `WebContents/META-INF/context.xml` 이 없으면 `context.xml.example` 을 복사해 만든다.
5. 작업을 마치면 `docs/tracks/JSP.md` §6·§7·§8 을 갱신한다. **이게 인계다.**

> 차단되어 있어도 할 일이 있다: `shop/util` DBCP 커넥션 유틸, `web.xml` Servlet 매핑 골격, **화면 목록 + URL 매핑 기록**(이 트랙의 첫 산출물).

---

## 3. 쓰기 권한 — 이 세션이 수정하는 파일

| 경로 | 내용 |
|---|---|
| `src/main/java/shop/controller/**` | Servlet |
| `src/main/java/shop/service/**` | 비즈니스 로직 |
| `src/main/java/shop/dao/**` | DB 접근 (SQL 실행) |
| `src/main/java/shop/dto/**` | 데이터 전달 객체 |
| `src/main/java/shop/util/**` | DB 연결, 공통 유틸 |
| `WebContents/{member,product,cart,order,mypage,admin,common}/*.jsp` | 라우팅 페이지 · 공통 include |
| `WebContents/index.jsp` | 메인 진입 |
| `WebContents/WEB-INF/web.xml` | 배포 서술자 |
| `WebContents/META-INF/context.xml` | DB 커넥션 풀 (git 추적 제외) |
| `WebContents/js/validate.js` | 폼 검증 — 서버 검증과 동일 규칙 |
| `docs/tracks/JSP.md` | 자기 트랙 문서 |

## 4. 읽기 전용 — 절대 수정하지 않는다

| 경로 | 소유 | 왜 |
|---|---|---|
| `WebContents/css/**` (`DECISIONS.md` 포함) | UI 트랙 | 동시 편집 충돌. 클래스명이 필요하면 **읽어서 쓴다** |
| `WebContents/mockup/**` | UI 트랙 | 목업은 UI의 산출물. JSP로 **이식**하되 원본은 건드리지 않는다 |
| `WebContents/js/ui.js` | UI 트랙 | 같은 `js/` 디렉터리지만 파일 단위로 소유가 갈린다 |
| `WebContents/images/**` | UI 트랙 | — |
| `docs/ERD.md`, `docs/CRUD.md` | ERD 트랙 | DTO의 설계 근거. 틀렸다고 생각되면 **고치지 말고 요청** |
| `sql/schema.sql`, `sql/data.sql` | DB Schema 트랙 | 컬럼이 필요하면 요청한다. 직접 ALTER 하지 않는다 |
| `CLAUDE.md`, `README.md`, `docs/STATUS.md` | PM | — |
| `.project`, `.classpath`, `.settings/**`, `WEB-INF/lib/**` | PM | 건드리면 **전 트랙이 멈춘다** |

---

## 5. 능력 범위

**할 수 있다**
- Servlet 4.0 / JSP 2.3 범위의 서버 로직 전부
- `WEB-INF/lib/` 에 **이미 있는 6개 라이브러리** 사용
  (`mysql-connector-j` · `commons-dbcp` · `commons-pool` · JSTL impl/spec · `cos.jar`)
- `cos.jar` 로 파일 업로드, `commons-dbcp` 로 커넥션 풀
- UI 목업 마크업을 JSP로 이식 (클래스명은 그대로 유지)
- 화면 목록 · URL 매핑 **기록** (확정은 PM)

**하지 않는다**
- 라이브러리 추가 반입 — 필요하면 PM에 요청
- CSS 수정 · 디자인 변경 · 클래스명 변경
- 테이블 · 컬럼 설계 변경
- 외부 API · CDN · 소셜 로그인 · 실결제 연동 (공지 항목 10)
- DB 접속 정보 소스 하드코딩, `context.xml` 커밋

---

## 6. 다른 세션과의 인터페이스

| 방향 | 상대 | 주고받는 것 |
|---|---|---|
| ← 받는다 | UI 트랙 | 클래스명 · 토큰 변수명 (`css/DECISIONS.md`), 마크업 (`mockup/*.html`) |
| ← 받는다 | ERD 트랙 | DTO 필드 계약 (`docs/ERD.md`) |
| ← 받는다 | DB Schema 트랙 | 실제 테이블 · 컬럼 (`sql/schema.sql`) |
| → 내보낸다 | **PM** | **화면 목록 + URL 매핑** → PM이 UI 기록과 대조해 계약 확정 (보류 해제 트리거) |
| → 내보낸다 | PM | README 의 아키텍처 · URL 매핑 · 핵심 구현 섹션 내용 |
| → 내보낸다 | ERD / Schema 트랙 | DAO가 실제로 필요한 컬럼 · 조회 조건 |

**요청은 말로 하지 않고 `docs/tracks/JSP.md` §7 표에 적는다.** PM이 읽고 전파한다.

---

## 7. 충돌이 생겼을 때

1. 수정해야 할 파일이 §4 목록에 있으면 → **멈추고** `docs/tracks/JSP.md` §7 에 요청을 적는다.
2. 읽어온 클래스명 · 필드명이 실제와 다르면 → 직접 맞추지 말고 §8 에 적는다. 임의 수정은 계약 위반이다.
3. 같은 파일이 이미 다른 내용으로 바뀌어 있으면 → 덮어쓰지 말고 PM에 보고한다.
4. 차단되어 더 할 게 없으면 → §8 에 막힌 지점을 적고 종료한다. 추측으로 진행하지 않는다.

**git 수칙** — 병렬 세션과 섞이지 않게:
- `git add -A` 금지. **§3 목록의 파일만 경로로 지정해 스테이징**한다.
- 다른 트랙 파일이 작업 트리에 변경돼 있으면 그대로 두고 커밋에 포함하지 않는다.
- **브랜치를 만들지 않는다.** `main` 위에서 직접 작업한다. (공지 항목 13)
- 커밋 전 `git status` 로 다른 트랙의 변경이 섞여 있는지 확인한다. 섞여 있으면 **그대로 두고** 자기 파일만 커밋한다.
- 한 커밋에는 이 트랙의 변경만 담는다. `git push` 는 PM만 한다.
- `git reset --hard` · `git checkout -- .` · 강제 푸시 금지 — 다른 세션의 미커밋 작업이 사라진다.
