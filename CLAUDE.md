# CLAUDE.md — 프로젝트 규약

> **이 폴더는 LLM 연동 브랜치(`LLM_integration`)다.** Eclipse 프로젝트명과 Context Root가 `WebApplication_TermProject_LLM` 라서 메인 · 샌드박스와 동시에 Import할 수 있다. 여기서는 `LLM_integration` 만 푸시한다.
>
> **이 브랜치는 LLM 개발만을 위해 존재한다.** 3계층(ERD · DB · UI) 개발은 하지 않고, **LLM API 모델 이식 · 기능 로직 · JSP 연결** 만 개발한다. 이 브랜치에서 일하는 세션은 **PM** 과 **LLM API 트랙** 둘뿐이다.

웹프로그래밍응용 기말프로젝트 · JSP 쇼핑몰 · 3인(Jiyong Kim, Hawon Yoon, Minjun Kim). 규약은 PM 세션이 관리하며, 충돌이 있으면 PM에 확인한다.

## 1. 환경

- Eclipse Dynamic Web Project · JDK 17 · Tomcat 9.0 (Servlet 4.0 / JSP 2.3) · MySQL 9.6 · Connector/J 9.7
- 웹 루트 `WebContents/` · 소스 `src/main/java/shop/{controller,service,dao,dto,util}` · 출력 `build/classes`
- Context Root `WebApplication_TermProject_LLM` → `http://localhost:8080/WebApplication_TermProject_LLM/`
- 라이브러리는 `WEB-INF/lib` 의 7개만 쓴다 (connector-j · dbcp · pool · JSTL · cos.jar · gson). gson 2.15.0은 LLM 요청 · 응답 JSON 처리용으로 이 브랜치에서만 추가했다
- LLM 서버는 학교 IP 요청만 받는다. Docker + 쿠버네티스 환경을 조성해 개발할 예정이다(구성은 강의 안내 대기, STATUS L4). 그 전까지 임시로 `docker/llm-tunnel` 골격을 둔다
- 학교 서버를 받기 전까지 기능 점검은 USB(`KM__JY/LLM`)의 소형 모델로 한다. `tools/llm/start-local.sh` 가 USB 모델(8081)과 Ollama 형식 중계(31434)를 띄운다. 개발은 학교 서버 방식(Ollama)을 기준으로 한다
- 스펙 기준은 `README.md`

## 2. 원칙

- localhost 전용. 외부 API · CDN · 원격 폰트 · 실결제 · 소셜 로그인 · 배포는 금지한다. 예외는 `LLM_integration` 의 LLM API 호출(수업 실습용 Ollama 서버 · `gemma3:12b`) 하나뿐이다. 앱은 `llm/ollamaUrl` 로만 부르며, 지금 값은 임시 터널 `localhost:31434` 다.
- 평가 기준은 **프론트 → 배선 → DB 흐름이 끊기지 않는 것**이다. LLM 기능도 이 흐름 위에 얹는다.
- 테이블 · 컬럼 · CSS · 목업을 새로 만들지 않는다. LLM 기능은 기존 스키마와 기존 CSS 클래스(`css/DECISIONS.md`) 위에 얹는다.
- UI는 라우팅 페이지와 팝업을 구분해 만든다. 팝업은 기존 `<dialog>` 패턴을 쓴다.

## 3. 세션과 파일 소유권

이 브랜치의 세션은 둘이다. **자기 소유 파일만 수정**하고 나머지는 읽기만 한다.

| 세션 | 소유 파일 | 문서 |
|---|---|---|
| **LLM API** | `src/main/java/shop/llm/**` · `Llm*` 접두어 controller · service · dto · `WebContents/llm/**` · `js/llm.js` · `web.xml` · `META-INF/context.xml(.example)` · `docker/llm-tunnel/**` · `tools/llm/**` · `Llm*` 접두어 dao | `docs/tracks/LLM.md`, 세션 규칙 `src/main/java/shop/llm/CLAUDE.md` |
| PM | `CLAUDE.md` · `docs/STATUS.md` · `README.md` · `.gitignore` · Eclipse 설정 · `WEB-INF/lib` | — |

- 그 밖의 파일(`docs/ERD.md` · `docs/CRUD.md` · `sql/**` · `css/**` · `mockup/**` · `images/**` · 기존 Java · JSP · JS)은 샌드박스 `a835e26` 에서 가져온 **기반 구현**이다. 이 브랜치에서는 읽기만 한다.
- LLM API 세션은 LLM 진입점(링크 · 팝업 · 호출 한 줄)을 붙일 때만 기존 JSP · 서블릿을 최소한으로 고칠 수 있다. 고친 파일은 `docs/tracks/LLM.md` 의 기존 파일 수정 기록에 남긴다. LLM과 무관한 수정은 하지 않는다.
- 스키마 · 디자인 변경이 필요하면 직접 하지 말고 `docs/tracks/LLM.md` 요청란에 적어 PM을 거친다.
- 업로드는 `WebContents/upload/` 에 저장하고 `/upload/<파일명>` 으로 참조한다.
- DB 접속 정보는 `META-INF/context.xml` 에만 둔다 (git 제외, 템플릿 `context.xml.example`).

## 4. 브랜치와 git

| 브랜치 | 폴더 | 용도 |
|---|---|---|
| `main` | `웹프로그래밍응용_기말프로젝트` | 제출 산출물 |
| `sandbox` | `웹프로그래밍응용_기말프로젝트_Sandbox` (형제 폴더) | 실험. origin에 올라가 있다 |
| `LLM_integration` | `웹프로그래밍응용_기말프로젝트_LLM` (형제 폴더) | LLM API 이식 · 기능 로직 · JSP 연결 전용. 샌드박스에서 갈라졌고 origin에 올라가 있다 |

- 위 세 브랜치 외에는 브랜치 · worktree를 만들지 않는다. 각 폴더는 자기 브랜치만 체크아웃하고 자기 브랜치만 푸시한다.
- 샌드박스 · LLM 브랜치 결과는 `main` 에 **병합하지 않는다.** PM이 판단으로 정리해 `main` 에서 다시 구현한다.
- LLM 서버 주소 · 모델명은 `META-INF/context.xml` 의 `<Environment>`(`llm/ollamaUrl` · `llm/ollamaModel`)에서만 읽고 소스에 적지 않는다. 키가 생기면 같은 곳에 두고 소스 · 커밋 · 문서 · 로그에 쓰지 않는다.
- LLM 호출은 JDK 17 `java.net.http.HttpClient` + gson으로 한다. gson 외의 라이브러리는 추가하지 않는다.
- 커밋 전 `git status` 로 확인하고, 자기 파일만 경로로 지정해 스테이징한다. `git add -A` · `git add .` 은 쓰지 않는다. 한 커밋에는 한 세션의 작업만 담는다.
- `git reset --hard` · `git checkout -- .` · 강제 푸시는 금지한다. 같은 폴더의 다른 세션 작업이 사라진다.
- 커밋 메시지에 `Co-Authored-By` 등 작성 도구 표기 줄을 넣지 않는다.
- 푸시는 PM만 한다.

## 5. 현재 상태

- `main`: 골격과 규약만 있다.
- `sandbox`: 4개 트랙(UI · JSP 배선 · ERD · DB Schema)이 작업 중이다. 그 트랙들의 문서와 결정 대기 항목은 샌드박스에 있다.
- `LLM_integration`: 샌드박스 `a835e26` 에서 갈라졌다. LLM API 세션은 Ollama 클라이언트와 기능 3종 골격을 만들었다. 이후 개발은 단계 1(실제 LLM 연결)과 단계 2(가짜 서버로 기능 구현 · 시험 스크립트)를 병렬로, 단계 2 의 기능 3개도 병렬로 진행한다 (`docs/tracks/LLM.md` § 10). 학교 IP 인증 문제는 Docker + 쿠버네티스 환경으로 풀 예정이며 구성은 미정이다.
- 진행과 PM 결정 대기 항목은 `docs/STATUS.md` 에 있다.
