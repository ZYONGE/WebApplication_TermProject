# 트랙 2 — JSP 배선 (Wiring / Server)

> 소유 세션: **JSP 배선 세션** · 이 문서는 해당 세션이 직접 갱신한다.
> 최종 갱신: 2026-10-28 (PM, 초기 생성) · 상태: ⬜ 착수 전

---

## 1. 소유 파일 — 이 트랙만 수정한다

| 경로 | 내용 |
|---|---|
| `WebContents/{member,product,cart,order,mypage,admin}/*.jsp` | 라우팅 페이지 JSP |
| `WebContents/common/*.jsp` | header / footer 등 공통 include |
| `WebContents/index.jsp` | 메인 진입 페이지 |
| `src/main/java/shop/controller/**` | Servlet |
| `src/main/java/shop/service/**` | 비즈니스 로직 |
| `src/main/java/shop/dao/**` | DB 접근 (SQL 실행) |
| `src/main/java/shop/dto/**` | 데이터 전달 객체 |
| `src/main/java/shop/util/**` | DB 연결, 공통 유틸 |
| `WebContents/WEB-INF/web.xml` | 배포 서술자 |
| `WebContents/META-INF/context.xml` | DB 커넥션 풀 (git 추적 제외) |
| `WebContents/js/validate.js` | 폼 검증 — 서버 검증과 **동일 규칙** 유지 |

## 2. 읽기만 하는 파일

- `WebContents/css/**`, `WebContents/mockup/**` — 마크업·클래스명 가져오기용. **수정 금지** (UI 트랙 소유)
- `WebContents/js/ui.js` — **수정 금지** (UI 트랙 소유)
- `docs/ERD.md`, `sql/schema.sql` — DTO·DAO 설계 근거. **수정 금지**

## 3. 착수 조건

| 조건 | 상태 |
|---|---|
| 화면 목록 + URL 매핑 계약 | ⬜ 이 세션에서 기록 → PM 재개 |
| DTO 필드 계약 (ERD 트랙과 합의) | ⬜ 대기 |
| UI 목업 마크업 | ⬜ 대기 (없어도 Servlet·DAO 선행 가능) |

> **선행 가능한 작업**: `shop/util`의 DBCP 커넥션 유틸, `web.xml` Servlet 매핑 골격, `META-INF/context.xml` 구성.

## 4. 작업 규칙

- 라이브러리는 `WebContents/WEB-INF/lib/`에 **이미 확보된 6개만** 사용한다. 추가 반입 금지.
  - `mysql-connector-j-9.7.0.jar` · `commons-dbcp-1.4.jar` · `commons-pool-1.5.7.jar`
  - `taglibs-standard-impl-1.2.5.jar` · `taglibs-standard-spec-1.2.5.jar` · `cos.jar`
- 파일 업로드는 `cos.jar`, DB 커넥션 풀은 `commons-dbcp` 사용.
- 업로드 저장 경로는 `WebContents/upload/`, 웹 참조 경로는 `/upload/<파일명>` 으로 고정.
- Servlet 4.0 / JSP 2.3 범위를 벗어나는 문법 금지. (Tomcat 9.0.117)
- 외부 API·소셜 로그인·실결제 연동 금지. (공지 항목 10)
- 공통 JSP는 `WebContents/common/`에 두고 모든 페이지가 include한다.
- `common/header.jsp`의 레이아웃 마크업은 UI 트랙의 `mockup/_layout.html`을 이식해 쓴다. CSS를 직접 수정하지 않고, 필요하면 §7로 요청한다.
- DB 접속 정보는 `context.xml`에만 둔다. 소스에 하드코딩 금지, git 커밋 금지.

## 5. Done 기준

- [ ] 화면 목록 + URL 매핑 기록 완료 (PM 계약 재개 트리거)
- [ ] `shop/util` DB 커넥션 유틸 동작 확인
- [ ] DTO / DAO / Service / Controller 4계층 배선 완료
- [ ] 모든 라우팅 페이지 JSP가 실제 데이터로 렌더링
- [ ] 팝업이 페이지와 연동되어 동작
- [ ] 프론트 → 배선 → DB 왕복 흐름 확인 (공지 항목 11)

## 6. 진행 상황 로그

| 날짜 | 작업 | 산출물 |
|---|---|---|
| — | 착수 전 | — |

## 7. 다른 트랙에 넘길 요청

| 대상 | 요청 내용 | 상태 |
|---|---|---|
| — | — | — |

## 8. 미결 / 질문

- 화면 목록·URL 매핑 기록이 이 트랙의 첫 산출물이다.
