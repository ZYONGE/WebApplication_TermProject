# CLAUDE.md — 프로젝트 규약

> 웹프로그래밍응용 기말프로젝트 / 쇼핑몰
> 이 문서는 프로젝트 매니저(PM) 세션이 공지한 개발 계획을 기록한 규약이다.
> 작업 전 반드시 이 문서를 기준으로 판단하고, 충돌이 있으면 PM에게 확인한다.

---

## 1. 프로젝트 성격

- **형식**: Eclipse Dynamic Web Project (Servlet 4.0 / Dynamic Web Module 4.0)
- **산출물**: JSP 기반 쇼핑몰 웹 애플리케이션
- **스펙 출처**: `README.md` — 스펙은 README에 명시된 것을 **단일 기준(Source of Truth)** 으로 삼는다.
- **범위**: 이 프로젝트는 **localhost 범위를 벗어나지 않는다.**

### 확정 환경 (README Prerequisites)

| 항목 | 버전 |
|---|---|
| JDK | OpenJDK 17.0.19 |
| Apache Tomcat | 9.0.117 (Servlet 4.0 / JSP 2.3) |
| Dynamic Web Module | 4.0 |
| MySQL | 9.6.0 |
| MySQL Connector/J | 9.7.0 |
| Eclipse | IDE for Enterprise Java and Web Developers 4.39 (2026-03) |
| DB Client | DBeaver, HeidiSQL |

- **Context Root**: `WebApplication_TermProject`
- **웹 루트**: `WebContents/` (표준 `WebContent`가 아님 — 경로 주의)
- **Java 소스**: `src/main/java/shop/...` → 출력 `build/classes`
- **접속 URL**: `http://localhost:8080/WebApplication_TermProject/`

---

## 2. 작업 원칙 (공지 사항)

1. README에 명시된 스펙에 따라 Eclipse Dynamic Web Project 형식으로 쇼핑몰을 제작한다.
2. FrontEnd 편집은 원래 DreamWeaver(CSS/HTML)가 기본이나, **본 프로젝트는 Claude Code를 통한 편집·삽입 방식을 채택**한다.
   - 따라서 CSS/HTML은 손으로 짠 것처럼 **읽기 쉽고 일관된 코드**여야 한다. 빌드 도구·전처리기·npm 의존성은 도입하지 않는다.
3. 전체 컨셉, 디자인 테마, 인터랙션 방식은 **하나의 컨셉으로 통일**하여 PM이 일괄 전달한다.
   - ⚠️ **컨셉 전달 전에는 디자인 토큰(`css/base/variables.css`)과 페이지 스타일을 임의로 확정하지 않는다.**
4. 개발 분야는 **UI/UX(CSS·HTML) / JSP / ERD / DB Schema** 총 4단계로 나눠 진행한다. 단계 번호는 분야 구분이며, 실제 작업은 **항목별 별도 세션에서 병렬 진행**한다 (§ 4 참조).
5. UI 분야는 **라우팅 페이지**와 **팝업**을 구분지어 개발한다.
6. JSP는 각 페이지와 기능의 **배선(wiring)** 작업을 담당한다.
7. ERD는 입력받은 각 데이터의 **저장과 사용**을 설계한다.
8. DB Schema는 **데이터의 저장**을 담당한다.
9. 이 프로젝트는 localhost 범위를 벗어나지 않는다.
10. 외부 사이트·외부 서버·클라이언트 피드백 등은 고려하지 않는다. **무조건 내수(내부) 작업.**
    - 외부 API 호출, CDN 의존, 결제 PG 실연동, 소셜 로그인, 배포/도메인 작업은 **범위 외**.
11. 프로젝트 완성도는 **프론트엔드 → 배선&서버 → DB** 의 원활한 흐름으로 평가된다.
    - 즉, 어느 한 계층만 화려한 것보다 **세 계층이 끊기지 않고 연결되는 것**이 최우선 평가 기준이다.
12. 개발 인원은 3명(Jiyong Kim, Hawon Yoon, Minjun Kim)이며, **이 세션은 프로젝트 매니저를 맡는다.**
13. **추가 브랜치·피처 브랜치를 생성하지 않는다. 오로지 `main` 브랜치 위에서만 작업한다.** (§ 4.4)

---

## 3. 4단계 개발 분야

> 아래 Stage 1~4는 **동시에 진행되는 병렬 트랙**이다. 번호는 생산 순서가 아니라 단지 분야 식별자다.

### Stage 1 — UI/UX (CSS, HTML)
- 담당 범위: `WebContents/css/**`, `WebContents/mockup/**`(정적 HTML 목업), `WebContents/images/**`, `WebContents/js/ui.js` — 상세는 `docs/tracks/UI.md`
- **라우팅 페이지**와 **팝업**을 분리해 개발한다.
  - 라우팅 페이지 → `css/pages/*.css` (main, member, product, cart, order, mypage, admin)
  - 팝업/모달/토스트 → `css/components/popup.css`
- 공통 규약
  - `css/base/variables.css` = 디자인 토큰(`:root`) **단일 정의 지점**. 색·폰트·간격은 여기서만 선언하고 나머지는 `var()`로 참조한다.
  - `css/base/reset.css` → `variables.css` → `layout.css` → `components/*` → `pages/*` 순서로 로드.
  - 컴포넌트 클래스는 재사용 가능하게, 페이지 전용 스타일은 페이지 CSS에만 둔다.

### Stage 2 — JSP (배선 / 서버)
- 담당 범위: `WebContents/**/*.jsp`, `src/main/java/shop/{controller,service,dao,dto,util}`, `WebContents/js/validate.js` — 상세는 `docs/tracks/JSP.md`
- 페이지 ↔ 기능 ↔ 데이터의 **연결**을 책임진다.
- 공통 JSP(header/footer)는 `WebContents/common/`에 두고 모든 페이지가 include한다.
- 라이브러리는 이미 `WebContents/WEB-INF/lib/`에 확보된 것만 사용한다.
  (mysql-connector-j, commons-dbcp, commons-pool, JSTL 1.2.5, cos.jar)
- 파일 업로드는 `cos.jar`, DB 커넥션 풀은 `commons-dbcp` 사용.

### Stage 3 — ERD
- 담당 범위: `docs/ERD.md`, `docs/CRUD.md` — 상세는 `docs/tracks/ERD.md`
- 각 데이터의 **저장과 사용**을 설계한다. 화면에서 입력받는 값 → 어디에 저장되고 → 어느 화면에서 쓰이는지를 모두 추적한다.
- `docs/CRUD.md`는 화면×테이블 CRUD 매트릭스로 유지한다. (배선 누락 검출용)

### Stage 4 — DB Schema
- 담당 범위: `sql/schema.sql` (DDL), `sql/data.sql` (샘플 데이터) — 상세는 `docs/tracks/SCHEMA.md`
- ERD를 그대로 반영한다. 스키마와 ERD가 어긋나면 ERD를 기준으로 맞춘다.

---

## 4. 운영 모델 — 항목별 병렬 개발

- 공지된 4단계 번호는 **담당 분야의 구분**이며 **시간순이 아니다. 모든 항목은 병렬로 개발한다.**
- **항목별로 별도 세션을 연다.** 세션 개설은 사용자의 요청에 따른다.
  - ERD 설계 세션
  - 디자인 테마 / UI 세션
  - JSP 배선 세션
- **이 세션(PM)의 역할**
  - 요청이 들어올 때마다 각 항목의 개발 상황을 확인하고 관리한다.
  - 항목 간 충돌·공백·불일치를 찾아내 조정한다.
  - 필요하면 임의로 서브 세션을 열어 확인·점검 작업을 수행한다.
  - 직접 구현보다 **계약(인터페이스) 확정과 통합 점검**을 우선한다.
  - 진행 현황은 `docs/STATUS.md`에 기록한다.

### 4.1 세션별 파일 소유권 (동시 편집 충돌 방지)

병렬 작업의 유일한 실패 요인은 두 세션이 같은 파일을 동시에 건드리는 것이다. 아래 소유권을 지킨다.

| 소유 세션 | 전담 파일 / 디렉터리 | 트랙 문서 |
|---|---|---|
| UI / 디자인 테마 | `WebContents/css/**` (결정 기록: `css/DECISIONS.md`), `WebContents/mockup/**`, `WebContents/images/**`, `WebContents/js/ui.js` | `docs/tracks/UI.md` |
| JSP 배선 | `WebContents/**/*.jsp`, `src/main/java/shop/**`, `WebContents/WEB-INF/web.xml`, `WebContents/META-INF/context.xml`, `WebContents/js/validate.js` | `docs/tracks/JSP.md` |
| ERD | `docs/ERD.md`, `docs/CRUD.md` | `docs/tracks/ERD.md` |
| DB Schema | `sql/schema.sql`, `sql/data.sql` | `docs/tracks/SCHEMA.md` |
| PM (이 세션) | `CLAUDE.md`, `docs/STATUS.md`, `docs/tracks/**`(템플릿), `README.md`, `.gitignore`, Eclipse 설정 파일 | — |

**규칙**

- 자기 소유가 아닌 파일은 **읽기만** 한다. 수정이 필요하면 트랙 문서 §7에 적고 PM을 거쳐 소유 세션에 넘긴다.
- 각 트랙 세션은 작업 시작 시 자기 트랙 문서를 읽고, 끝낼 때 §6 진행 로그와 §7 요청을 **직접 갱신**한다. 이것이 세션 간 인계 경로다.
- UI 트랙의 결정(토큰 변수명·클래스명·팝업 패턴)은 `WebContents/css/DECISIONS.md`에 기록된다. **JSP 배선 트랙은 마크업을 쓰기 전 이 문서를 읽는다.**
- `README.md`는 모든 트랙이 채워야 하므로 **PM이 취합**한다. 각 세션은 내용을 PM에 전달하고 직접 쓰지 않는다.
- Eclipse 설정(`.project`, `.classpath`, `.settings/**`)과 `WEB-INF/lib/**`는 **PM만** 손댄다. Dynamic Web Project 구성이 깨지면 전 트랙이 멈춘다.

**작업 디렉터리별 세션 안내** — 새 세션이 열리는 즉시 자기 역할·권한을 알도록, 각 작업 디렉터리에 중첩 `CLAUDE.md` 를 둔다. 루트 규약과 함께 자동으로 읽힌다.

| 작업 디렉터리 | 세션 안내 | 대상 트랙 |
|---|---|---|
| `src/main/java/shop/` | `src/main/java/shop/CLAUDE.md` | JSP 배선 (`*.jsp` 포함, 두 트리에 걸침) |
| `docs/` | `docs/CLAUDE.md` | ERD 설계 (PM 파일과 공유하는 디렉터리) |
| `sql/` | `sql/CLAUDE.md` | DB Schema |
| `WebContents/css/` | — | UI/UX는 세션 안내 없이 `css/DECISIONS.md`(결정 기록) + `docs/tracks/UI.md` 운용 |

각 세션 안내는 ①세션 정체성 ②즉시 시작 체크리스트 ③쓰기 권한 ④읽기 전용 ⑤능력 범위 ⑥타 세션 인터페이스 ⑦충돌 수칙·git 수칙 구성이다.

**공유 지점 해소 규칙** — 두 트랙의 관심이 겹치는 곳은 아래로 확정한다.

| 지점 | 결정 |
|---|---|
| 정적 목업 vs JSP | 목업은 `WebContents/mockup/*.html` 전용 디렉터리. JSP는 `WebContents/{member,product,cart,order,mypage,admin,common}/*.jsp`. **같은 디렉터리를 공유하지 않는다.** |
| `common/header.jsp` · `footer.jsp` | 소유는 JSP 트랙. UI 트랙은 `mockup/_layout.html`로 마크업을 **제시**하고, 이식은 JSP 트랙이 한다. |
| `WebContents/js/` | 파일 단위로 분리. `ui.js` = UI 트랙(팝업·탭·드롭다운), `validate.js` = JSP 트랙(폼 검증 — 서버 검증과 동일 규칙). |
| 업로드 이미지 경로 | 저장은 `WebContents/upload/`, 웹 참조는 `/upload/<파일명>`. 양 트랙 공통 계약으로 고정. |
| DB 접속 정보 | `META-INF/context.xml`(git 제외)은 JSP 트랙 소유. 스키마명·계정은 DB Schema 트랙과 일치시킨다. 템플릿은 `context.xml.example`. |

### 4.2 병렬 개발을 성립시키는 계약(Contract)

ERD 없이 배선을, 컨셉 없이 UI를 동시에 진행하려면 다음 **계약을 먼저 고정**해야 한다. 계약이 서면 각 세션은 서로를 기다리지 않는다.

1. **화면 목록 + URL 매핑** — 어떤 라우트가 존재하고 각 라우트가 어떤 데이터를 필요로 하는지. (UI ↔ 배선 접점)
   - ⏸ **현재 보류.** PM이 선행 확정하지 않는다. **프론트엔드 세션과 JSP 배선 세션이 각자 트랙 문서에 화면·라우트를 기록한 뒤**, PM이 두 기록을 대조해 `README.md` URL 매핑 명세로 확정하며 재개한다.
2. **DTO 필드 계약** — 각 화면이 소비하는 데이터 객체의 필드명 / 타입. (배선 ↔ ERD 접점)
3. **CSS 클래스 네이밍 + 디자인 토큰 변수명** — 색·폰트 **값**은 컨셉 전달 후 확정하되, **변수명과 클래스명은 먼저 고정**한다. 그러면 UI 세션은 값만 채워 넣고, JSP 세션은 마크업을 먼저 쓸 수 있다.
4. **팝업 패턴** — 라우팅 페이지와 팝업을 구분해 개발한다(항목 5). 팝업은 `components/popup.css` + 공통 마크업 패턴으로 통일한다.

계약 변경은 PM을 거친다. 계약이 바뀌면 영향받는 세션 전부에 전파한다.

### 4.3 통합 점검

- 항목 11의 평가 기준에 따라, 병렬 산출물이 쌓이면 **프론트 → 배선 → DB 왕복 흐름 통합 점검**을 PM 세션이 수행한다.
- 모든 트랙이 `main` 위에서 작업하므로 **병합 단계가 없다.** 통합 점검은 현재 `main` 상태를 그대로 대상으로 한다.
- 통합 점검 시점에 미완 상태로 섞인 변경이 있으면, 되돌리지 않고 해당 트랙 세션에 완료를 요청한다.

### 4.4 브랜치 전략 — `main` 단일 브랜치

- **추가 브랜치 · 피처 브랜치 · worktree 를 만들지 않는다. 모든 작업은 `main` 위에서 직접 한다.** (공지 항목 13)
- PM이 서브 세션을 열 때도 별도 worktree를 쓰지 않고 **같은 작업 폴더를 공유**한다.
- 결과: **브랜치 격리라는 안전망이 없다.** 충돌을 막는 것은 § 4.1 파일 소유권과 아래 수칙뿐이다.

**동시 작업 수칙** (`main` 공유 전제)

| 수칙 | 이유 |
|---|---|
| 커밋 전 반드시 `git status` 로 변경 목록을 확인한다 | 다른 세션의 작업이 같은 트리에 섞여 있을 수 있다 |
| `git add -A` · `git add .` **금지.** 자기 소유 파일만 경로로 지정해 스테이징 | 타 트랙 변경을 끌어가면 그 세션의 작업이 미완 상태로 커밋된다 |
| 자기 소유가 아닌 파일이 변경돼 있으면 **그대로 둔다** | 다른 세션이 진행 중이라는 신호다 |
| 한 커밋에는 **한 트랙의 변경만** 담는다 | 되돌릴 때 트랙 단위로 분리된다 |
| **한 트랙당 세션 하나만** 연다 | 같은 트랙을 두 세션이 동시에 열면 파일 소유권 규약이 무의미해진다 |
| 충돌 · 덮어쓰기 위험이 보이면 멈추고 PM에 보고 | `main` 위에서는 되돌리기가 전원에게 영향을 준다 |
| `git push` 는 **PM만** 수행한다 | 원격 `main` 이력을 단일 창구로 관리 |
| `git reset --hard` · `git checkout -- .` · 강제 푸시 금지 | 다른 세션의 미커밋 작업이 소멸한다 |

---

## 5. 금지 / 범위 외

- 외부 서버 배포, 도메인, HTTPS, 클라우드
- 외부 API / CDN / 폰트 원격 로드 (오프라인 localhost에서 깨지면 안 됨)
- npm, webpack, Sass 등 프런트 빌드 체인
- 실결제 연동, 외부 인증(OAuth/소셜 로그인)
- 클라이언트 피드백·사용자 테스트 기반 의사결정
- 추가 브랜치 · 피처 브랜치 · worktree 생성 (공지 항목 13)

---

## 6. 산출물 관리

- `README.md`의 빈 섹션(아키텍처 / DB 설계 / 기능 / 핵심 구현 / URL 매핑 / UI / Build & Run)은 **각 단계가 끝날 때마다 채운다.** 최종 제출물의 얼굴이다.
- DB 접속 정보는 `WebContents/META-INF/context.xml`에 두고 git 추적하지 않는다. (`.gitignore` 반영됨)
  - 템플릿은 `WebContents/META-INF/context.xml.example` — 복사해서 값을 채운다.
- 업로드 이미지(`WebContents/upload/`)는 런타임 산출물이므로 커밋하지 않는다.
- 트랙별 진행 상황과 인계 사항은 `docs/tracks/*.md`에, 전체 현황은 `docs/STATUS.md`에 기록한다.

---

## 7. 디렉터리 구조 — 병렬 작업용 확정안

Dynamic Web Project 구성(웹 루트 `WebContents`, 소스 `src/main/java`, 출력 `build/classes`, `WEB-INF`/`META-INF`)은 **그대로 유지**하고, 세션 간 충돌만 제거하기 위해 아래 3개를 추가했다.

```
WebContents/
├── mockup/                      # [신설] UI 트랙 전용 정적 HTML 목업
│   ├── _layout.html             #   header/footer 마크업 제시용
│   └── *.html                   #   라우팅 페이지 + 팝업 목업
├── css/                         # UI 트랙
│   └── DECISIONS.md             # [신설] UI/UX 결정 기록 (타 트랙 읽기 전용)
├── js/
│   ├── ui.js                    # UI 트랙 (팝업·탭·드롭다운)
│   └── validate.js              # JSP 배선 트랙 (폼 검증)
├── images/                      # UI 트랙
├── upload/                      # 런타임 업로드 (git 제외)
├── {member,product,cart,order,mypage,admin,common}/   # JSP 배선 트랙 (*.jsp)
├── META-INF/
│   ├── MANIFEST.MF
│   └── context.xml.example      # [신설] DB 커넥션 풀 템플릿
└── WEB-INF/
    ├── lib/                     # PM 전담 (6개 고정)
    └── web.xml                  # JSP 배선 트랙

src/main/java/shop/
└── CLAUDE.md                    # [신설] JSP 배선 세션 안내

sql/
└── CLAUDE.md                    # [신설] DB Schema 세션 안내

docs/
├── CLAUDE.md                    # [신설] ERD 설계 세션 안내
├── STATUS.md                    # PM 현황 보드
├── ERD.md, CRUD.md              # ERD 트랙 산출물
└── tracks/                      # [신설] 트랙별 작업 지시 + 진행 로그 + 인계
    ├── UI.md
    ├── JSP.md
    ├── ERD.md
    └── SCHEMA.md
```

**검토 결과 — 변경하지 않는 것**

| 항목 | 판단 |
|---|---|
| 웹 루트명 `WebContents` | 유지. `.settings/org.eclipse.wst.common.component`·`.classpath`와 이미 정합. 변경 시 Eclipse 재설정 비용만 발생 |
| Context Root `WebApplication_TermProject` | 유지 |
| `src/main/java` 단일 소스 폴더 | 유지. 패키지 `shop.{controller,service,dao,dto,util}` 로 트랙 내부 분리 충분 |
| `build/`, `WEB-INF/lib` | 유지. `build/`는 git 비추적 상태 확인 완료 |
| `.project` / `.classpath` / `.settings` | 건드리지 않음. Import 재현성 유지 |

> 신설 항목은 모두 웹 루트 하위 일반 디렉터리 또는 저장소 문서이므로 Dynamic Web Project 구성에 영향이 없다.
