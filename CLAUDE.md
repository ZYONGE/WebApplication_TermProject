# CLAUDE.md — 프로젝트 규약

> **이 폴더는 샌드박스(`zyonge_Sandbox`)다.** Eclipse 프로젝트명과 Context Root가 `WebApplication_TermProject_Sandbox` 라서 메인과 동시에 Import할 수 있다. 여기서는 `zyonge_Sandbox` 만 푸시한다.

웹프로그래밍응용 기말프로젝트 · JSP 쇼핑몰 · 3인(Jiyong Kim, Hawon Yoon, Minjun Kim). 규약은 PM 세션이 관리하며, 충돌이 있으면 PM에 확인한다.

## 1. 환경

- Eclipse Dynamic Web Project · JDK 17 · Tomcat 9.0 (Servlet 4.0 / JSP 2.3) · MySQL 9.6 · Connector/J 9.7
- 웹 루트 `WebContents/` · 소스 `src/main/java/shop/{controller,service,dao,dto,util}` · 출력 `build/classes`
- Context Root `WebApplication_TermProject_Sandbox` → `http://localhost:8080/WebApplication_TermProject_Sandbox/`
- 라이브러리는 `WEB-INF/lib` 의 6개만 쓴다 (connector-j · dbcp · pool · JSTL · cos.jar)
- 스펙 기준은 `README.md`

## 2. 원칙

- localhost 전용. 외부 API · CDN · 원격 폰트 · 실결제 · 소셜 로그인 · 배포는 금지한다.
- CSS/HTML은 순수 코드로, 손으로 쓴 듯 읽기 쉽게 쓴다. 빌드 도구 · npm · Sass는 쓰지 않는다.
- 평가 기준은 **프론트 → 배선 → DB 흐름이 끊기지 않는 것**이다.
- 디자인 컨셉은 PM이 전달한다. 그 전에는 토큰 **이름**만 확정하고 값은 확정하지 않는다.
- UI는 라우팅 페이지와 팝업을 구분해 만든다.

## 3. 트랙과 파일 소유권

4개 트랙이 각자 세션에서 병렬로 작업한다. **자기 소유 파일만 수정**하고 나머지는 읽기만 한다. 다른 트랙 파일을 고쳐야 하면 자기 트랙 문서의 요청란에 적어 PM을 거친다.

| 트랙 | 소유 파일 | 트랙 문서 |
|---|---|---|
| UI | `css/**` · `mockup/**` · `images/**` · `js/ui.js` | `docs/tracks/UI.md`, 결정 기록 `css/DECISIONS.md` |
| JSP 배선 | `**/*.jsp` · `src/main/java/shop/**` · `web.xml` · `META-INF/context.xml` · `js/validate.js` | `docs/tracks/JSP.md` |
| ERD | `docs/ERD.md` · `docs/CRUD.md` | `docs/tracks/ERD.md` |
| DB Schema | `sql/schema.sql` · `sql/data.sql` | `docs/tracks/SCHEMA.md` |
| PM | `CLAUDE.md` · `docs/STATUS.md` · `README.md` · `.gitignore` · Eclipse 설정 · `WEB-INF/lib` | — |

- 목업은 `mockup/` 에만 둔다. JSP 디렉터리와 섞지 않는다.
- `common/header.jsp` · `footer.jsp` 는 JSP 소유다. UI는 `mockup/_layout.html` 로 마크업을 제시한다.
- 업로드는 `WebContents/upload/` 에 저장하고 `/upload/<파일명>` 으로 참조한다.
- DB 접속 정보는 `META-INF/context.xml` 에만 둔다 (git 제외, 템플릿 `context.xml.example`).
- 트랙 사이의 계약(화면 목록·URL 매핑, DTO 필드, CSS 클래스·토큰 이름, 팝업 패턴)은 PM을 거쳐 바꾼다.

## 4. 브랜치와 git

| 브랜치 | 폴더 | 용도 |
|---|---|---|
| `main` | `웹프로그래밍응용_기말프로젝트` | 제출 산출물 |
| `zyonge_Sandbox` | `웹프로그래밍응용_기말프로젝트_Sandbox` (형제 폴더) | 실험. origin에 올라가 있다 |

- 두 브랜치 외에는 브랜치 · worktree를 만들지 않는다. 각 폴더는 자기 브랜치만 체크아웃하고 자기 브랜치만 푸시한다.
- 샌드박스 결과는 `main` 에 **병합하지 않는다.** PM이 판단으로 정리해 `main` 에서 다시 구현한다.
- 커밋 전 `git status` 로 확인하고, 자기 파일만 경로로 지정해 스테이징한다. `git add -A` · `git add .` 은 쓰지 않는다. 한 커밋에는 한 트랙만 담는다.
- `git reset --hard` · `git checkout -- .` · 강제 푸시는 금지한다. 같은 폴더의 다른 세션 작업이 사라진다.
- 커밋 메시지에 `Co-Authored-By` 등 작성 도구 표기 줄을 넣지 않는다.
- 푸시는 PM만 한다.

## 5. 현재 상태

- `main`: 골격과 규약만 있다. ERD · 스키마 · JSP · CSS는 비어 있다.
- `zyonge_Sandbox`: 4개 트랙 모두 여기서 작업 중이다. 샌드박스의 `docs/ERD.md` 는 실험용 ERD다.
- 트랙별 진행과 PM 결정 대기 항목은 `docs/STATUS.md` 에 있다.
