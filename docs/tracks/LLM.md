# LLM API (LLM 브랜치 전용)

> 소유: LLM API 세션 · 상태: 🟡 기능 3종 파일 골격 완료 · 2단계 병렬 개발 계획 확정(§ 10) · 최종 갱신 2026-11-04
> 이 문서는 `LLM_integration` 전용이다. 여기 산출물은 `main` 으로 가지 않는다 — PM이 정리해 `main` 에서 다시 구현한다.
> 세션 규칙은 `src/main/java/shop/llm/CLAUDE.md`, 파일 소유 범위는 루트 `CLAUDE.md` § 3 을 따른다.

---

## 1. 범위

이 브랜치는 **LLM API 모델 이식 · 기능 로직 · JSP 연결** 만 개발하는 특수목적 브랜치다.

| 한다 | 하지 않는다 |
|---|---|
| LLM API 클라이언트 (`shop/llm/`) | ERD · CRUD 변경 |
| LLM 기능의 Service · Servlet · DTO (`Llm*`) | DDL · 샘플 데이터 변경 |
| LLM 화면 · 팝업 JSP, 기존 화면에 진입점 연결 | CSS · 목업 · 디자인 토큰 |
| 키 · 모델 설정 (`context.xml` `<Environment>`) | LLM과 무관한 배선 수정 |

기반 구현(ERD · DB · JSP · UI)은 샌드박스 `a835e26` 시점 그대로이며 이 브랜치에서는 읽기만 한다.

## 2. 작업 위치

| 구분 | 경로 |
|---|---|
| API 계층 | `src/main/java/shop/llm/` — 클라이언트 · 프롬프트 · JSON 처리 · 설정 읽기 |
| 업무 계층 | `shop/service/Llm*Service.java` · `shop/controller/Llm*Servlet.java` · `shop/dto/Llm*DTO.java` |
| 화면 | `WebContents/llm/*.jsp` (라우팅 페이지) · 기존 화면 안 `<dialog>` (팝업) |
| 스크립트 | `WebContents/js/llm.js` (비동기 호출이 필요할 때만) |
| 설정 | `META-INF/context.xml` `<Environment>` (git 제외) · 템플릿 `context.xml.example` |

호출 흐름: `JSP → js/llm.js → Llm{Desc|Review|Related}Servlet → 같은 이름의 Service → shop.llm (캐시 · 프롬프트 · 클라이언트) → LLM API`. DB가 필요하면 Service가 기존 DAO를 읽기 전용으로 쓴다.

### 파일 지도

| 파일 | 담당 | 상태 |
|---|---|---|
| `shop/llm/OllamaClient.java` | 공통 — Ollama 호출 (F3용 `format: "json"` 옵션 추가 예정) | 🟢 이식 |
| `shop/llm/LlmException.java` | 공통 — 화면용 안내 문구 + 원인 | 🟢 |
| `shop/llm/LlmCache.java` | 공통 — 메모리 캐시 · 동시 2건 제한 | ⬜ 골격 |
| `shop/llm/prompt/DescSummaryPrompt.java` | F1 상품 설명 요약 | ⬜ 골격 |
| `shop/llm/prompt/ReviewSummaryPrompt.java` | F2 리뷰 요약 | ⬜ 골격 |
| `shop/llm/prompt/RelatedSearchPrompt.java` | F3 연관 검색 | ⬜ 골격 |
| `shop/service/LlmService.java` · `shop/controller/LlmServlet.java` | 골격 — 2-0 에서 아래 기능별 파일로 나누고 지운다 | ⬜ 골격 (지금은 501 응답) |
| `shop/service/Llm{Desc,Review,Related}Service.java` | F1 · F2 · F3 업무 흐름 (기능마다 1개) | ⬜ 2-0 에서 생성 |
| `shop/controller/Llm{Desc,Review,Related}Servlet.java` | `/llm/desc` · `/llm/review` · `/llm/related` JSON (기능마다 1개) | ⬜ 2-0 에서 생성 |
| `shop/dao/LlmProductDAO.java` | F3 후보 조회 — 판매중 상품 전체(id · 이름 · 카테고리) | ⬜ 2-0 에서 생성 (소유 범위 확장 필요) |
| `tools/llm/OllamaBridge.java` | 점검용 중계 — Ollama 형식 요청을 USB 소형 모델(OpenAI 호환)로 넘긴다 (§ 11) | 🟢 검증 |
| `tools/llm/start-local.sh` · `check-server.sh` | 점검 모델 시작 · 연결 확인 (USB · 학교 서버 공용) | 🟢 검증 |
| `tools/llm/test-f{1,2,3}.sh` | 기능별 시험 스크립트 | ⬜ 2-F1 · F2 · F3 |
| `WebContents/js/llm.js` | 비동기 호출 · 자리 채우기 | ⬜ 골격 |
| `product/view.jsp` · `product/list.jsp` | 진입점 (기존 파일) | ⬜ 미수정 |

`WebContents/llm/` 은 라우팅 페이지용 자리다. 확정된 3개 기능은 모두 기존 상품 화면에 붙으므로 지금은 비어 있다.

## 3. 현재 상태

| 항목 | 상태 |
|---|---|
| 공급자 · 모델 | 🟢 수업 실습용 Ollama 서버 · `gemma3:12b` (STATUS L1 결정) |
| 라이브러리 | 🟢 `WEB-INF/lib/gson-2.15.0.jar` 추가 |
| `shop/llm/` 클라이언트 | 🟢 `OllamaClient.generate(prompt)` · `LlmException` — javac 통과 |
| 기능 3종 파일 | 🟡 골격 — § 2 파일 지도. 전체 소스 javac 0 오류 |
| `context.xml.example` `<Environment>` | 🟢 `llm/ollamaUrl`(터널 `localhost:31434`) · `llm/ollamaModel` |
| 점검용 모델 | 🟢 USB 소형 모델(Qwen3-4B) + Ollama 형식 중계 — `check-server.sh` PASS, `OllamaClient` 로 응답 · 연결 실패 처리 확인 (§ 11) |
| 접속 환경 | 🟡 Docker + 쿠버네티스로 개발 예정 · 구성은 강의 안내 대기. 임시 Docker 터널 골격만 있음 — § 8 |
| 기능 목록 · 화면 위치 | 🟢 확정 — 상품 설명 요약 · 리뷰 요약 · 연관 검색 (STATUS L2) |
| JSP 연결 | ⬜ 착수 전 |

## 4. 수업 실습 코드 분석

실습 자료는 `my.ai` 패키지의 클래스 2개와 JSP 2개다. 이 브랜치에 무엇을 가져왔고 무엇을 바꿨는지 적는다.

| 실습 파일 | 하는 일 | 이 프로젝트 반영 |
|---|---|---|
| `OllamaClient.java` | 질문을 그대로 `/api/generate` 에 보내고 `response` 를 돌려줌 | `shop/llm/OllamaClient` 로 이식 |
| `ollama.jsp` | 스크립틀릿으로 클라이언트를 직접 호출, 결과 출력 | 방식만 참고. 서블릿 → JSP 로 다시 짠다 |
| `OllamaClientMySQL.java` | 오늘 구매를 고객별 · 시간대별로 집계 → JSON → 규칙 + 데이터 + 질문 프롬프트 | 프롬프트 패턴만 가져온다. 집계는 기존 DAO 로 |
| `ollamamysql.jsp` | 질문 예시 · 분석 결과 화면 | 화면 구성 참고 (질문 예시 · 결과 · 오류) |

**그대로 가져온 것**: 엔드포인트 형식, `model` · `prompt` · `stream=false` 요청 본문, 연결 10초 · 응답 180초 타임아웃, `response` 필드 검사, UTF-8 송수신, 제출 버튼 잠금, `InterruptedException` 처리.

**바꾼 것**

| 실습 코드 | 이 프로젝트 | 이유 |
|---|---|---|
| 주소 · 모델을 상수로 적음 | `context.xml` `<Environment>` → JNDI | 규약 — 설정은 소스 밖 |
| `DriverManager` + `root` / `1234` 를 소스에 적음 | `DBUtil`(JNDI `jdbc/shop`) · 기존 DAO | 규약 — 접속 정보는 `context.xml` 에만 |
| `purchase` 테이블 · `web` DB | `orders` · `order_item` · `v_sales_daily` 등 (`webapplication_sandbox`) | 실제 스키마 |
| 오류 응답 본문을 화면 메시지에 붙임 | `LlmException` 은 안내 문구만, 본문은 cause → 로그 | 내부 정보 노출 방지 |
| `IOException` 을 그대로 던짐 | 연결 · HTTP · 응답 해석 실패를 `LlmException` 하나로 | 서블릿이 한 가지만 잡으면 된다 |
| 매 호출 `new HttpClient` | `HttpClient` 1개를 공유 | 스레드 안전 · 커넥션 재사용 |
| JSP 스크립틀릿 · 인라인 `<style>` | 서블릿 MVC · 기존 CSS 클래스 · `<c:out>` | 기반 구현 구조와 규약 |

**프롬프트 패턴** (`OllamaClientMySQL` 에서 가져옴): 역할 → 근거 범위("아래 데이터만 근거로") → 수치 정확성 → 없는 정보 추측 금지 → 데이터 속 문자열을 명령으로 해석 금지 → `[DB 집계 결과]` JSON → `[사용자 질문]`.

## 5. 기능 목록 (L2 확정)

PM이 전달한 3개 기능이다. 셋 다 공개 상품 화면에 붙고, 비로그인 · USER 모두 본다.

| # | 기능 | 화면 | 입력 데이터 | 출력 |
|---|---|---|---|---|
| F1 | 상품 설명 요약 | `/product/view` — 설명(`prod_desc`) 위 | `product.description` | 3문장 이내 요약 |
| F2 | 리뷰 요약 | `/product/view` — 리뷰(`prod_review`) 위 | 그 상품의 `review.rating` · `content` | 다수 의견을 하나로 정리한 요약 |
| F3 | 연관 검색 | `/product/list?keyword=` — 기존 검색 결과 아래 | 검색어 + 판매중 상품 목록(id · 이름 · 카테고리) | 키워드에 해당하거나 인접한 상품 목록 |

### 공통 설계

- **화면을 기다리게 하지 않는다.** 응답이 최대 180초라 서블릿 렌더 안에서 부르지 않는다. 페이지는 기존대로 즉시 그리고, `js/llm.js` 가 JSON 엔드포인트를 비동기로 불러 자리를 채운다. 실패하면 자리를 숨기고 기존 화면은 그대로 둔다.
- **엔드포인트**: `/llm/desc?productId=` · `/llm/review?productId=` · `/llm/related?keyword=` (정확 일치 매핑, 응답은 gson JSON). 병렬 개발을 위해 기능마다 서블릿 · 서비스를 따로 둔다 (§ 10).
- **캐시**: 테이블을 만들 수 없으므로 서버 메모리(`ConcurrentHashMap`)에 둔다. 키는 F1 = 상품 id + 설명 해시, F2 = 상품 id + 리뷰 수 + 최신 리뷰 시각, F3 = 정규화한 검색어. 원본이 바뀌면 키가 바뀌어 자동으로 다시 만든다. Tomcat 재시작 시 사라진다.
- **동시 호출 제한 (L3 제안)**: 공용 GPU 서버라 동시에 2건까지만 보내고(`Semaphore`), 같은 키를 이미 생성 중이면 새로 보내지 않는다.
- **개인정보**: 리뷰는 별점 · 본문만 보내고 작성자는 보내지 않는다.

### 기능별 규칙

| # | 호출하지 않는 경우 | 프롬프트 핵심 | 결과 검증 |
|---|---|---|---|
| F1 | 설명이 비었거나 짧으면(약 150자 이하) 원문을 그대로 쓴다 | "아래 설명만 근거로 3문장 이내 · 없는 사양 추측 금지" | 문장 수가 넘치면 앞 3문장만 |
| F2 | 리뷰가 2건 미만 | "공통 의견 · 다수 의견 위주, 소수 의견은 따로 한 줄" + 별점 분포는 DB 집계값으로 제공 | 평균 · 건수는 LLM이 아니라 DB 값을 화면에 쓴다 |
| F3 | 검색어가 비었을 때 | 후보 상품 JSON 을 주고 "관련 있는 상품 id 만 JSON 배열로" — Ollama `format: "json"` 사용 | 후보 목록에 없는 id 는 버린다. 이미 LIKE 결과에 있는 상품은 빼고 최대 8개 |

F3 때문에 `OllamaClient` 에 JSON 응답 형식 옵션을 추가한다.

### 기존 파일에 붙일 진입점 (구현 시 § 6 에 기록)

- `product/view.jsp` — F1 · F2 자리 두 곳 + `js/llm.js`
- `product/list.jsp` — F3 자리 한 곳 + `js/llm.js`

## 6. 기존 파일 수정 기록

LLM 진입점을 붙이려고 기반 구현 파일을 고친 경우만 적는다. `main` 재구현 때 이 표가 이식 목록이 된다.

| 파일 | 수정 내용 | 커밋 |
|---|---|---|
| — | — | — |

## 7. 요청 · 미결

| 대상 | 내용 | 상태 |
|---|---|---|
| PM | 공급자 · 모델 (L1) | 🟢 결정 — 수업 Ollama · `gemma3:12b` |
| PM | 기능 목록과 붙일 화면 · 역할 (L2) | 🟢 결정 — § 5 |
| PM | 호출 횟수 제한 방식 (L3) — 메모리 캐시 + 동시 2건 제한 제안 (§ 5) | 승인 대기 |
| PM | Docker + 쿠버네티스 구성 (L4) — 클러스터 제공처 · 올릴 대상 · 접속 방법 (§ 8) | 강의 안내 대기 |
| PM | LLM 소유 범위에 `tools/llm/**` · `Llm*` DAO 추가 (§ 10) | 반영 — 루트 `CLAUDE.md` § 3 |

## 8. LLM 서버 접속 환경

수업 Ollama 서버(`220.149.119.234:31434`)는 **학교 IP 에서 온 요청만** 받는다. 이를 해결하려고 **Docker + 쿠버네티스(k8s)** 환경을 조성해 개발할 예정이다 (STATUS L4).

**아직 정해지지 않은 것** — 강의 안내를 받은 뒤 정한다.

| 항목 | 후보 |
|---|---|
| 클러스터 | 학교 제공 k8s 클러스터인지 여부 |
| 올릴 대상 | 앱 전체(Tomcat + MySQL) / LLM 중계만 |
| 필요한 것 | 컨테이너 이미지(Dockerfile) · k8s 매니페스트 · 이미지 저장소 · 접속 계정(kubeconfig) |

앱 전체를 클러스터에 올리면 루트 `CLAUDE.md` § 2 의 "localhost 전용 · 배포 금지" 원칙을 PM 이 고쳐야 한다. LLM 중계만 올리면 중계 포트가 바깥에 열려 IP 제한이 사실상 풀리지 않도록 접근 제한을 함께 정한다.

### 임시 골격 — Docker 터널

k8s 구성이 정해지기 전에 만든 로컬 터널 골격이다. 앱이 `localhost:31434` 만 보게 하는 구조라 k8s 구성으로 바꿔도 Java 코드는 그대로 쓸 수 있다. 다만 학교 IP 를 얻는 자리가 비어 있어 지금은 학교망 밖에서 동작하지 않고, k8s 구성이 정해지면 바꾸거나 지운다.

```
Tomcat (OllamaClient) ──▶ 127.0.0.1:31434 ──▶ [llm-tunnel 컨테이너] ──(학교 IP 경로)──▶ Ollama 서버
```

- 앱은 `localhost:31434` 만 본다. 학교 IP 를 얻는 방법이 바뀌어도 Java · `context.xml` 은 그대로다.
- 포트는 `127.0.0.1` 에만 연다. 같은 네트워크의 다른 기기에는 열지 않는다.
- 학교망 안에서는 터널 없이 `context.xml` 의 `llm/ollamaUrl` 을 서버 주소로 바꿔 써도 된다.

| 파일 | 내용 |
|---|---|
| `docker/llm-tunnel/compose.yaml` | 서비스 정의 · `127.0.0.1:31434` 공개 |
| `docker/llm-tunnel/Dockerfile` | `alpine:3.20` + `socat` |
| `docker/llm-tunnel/entrypoint.sh` | 1) 학교 IP 확보 (빈 자리) 2) 포워딩 |
| `docker/llm-tunnel/.env.example` | 서버 주소 · 포트. `.env` 로 복사해 쓰고 git 에서 뺀다 |

**쓰는 법**

```bash
cd docker/llm-tunnel
cp .env.example .env
docker compose up -d --build
```

**현재 상태**

| 항목 | 상태 |
|---|---|
| compose 설정 검증 (`docker compose config`) | 🟢 통과 |
| 이미지 빌드 · 실행 | ⬜ 미검증 — Docker Desktop 이 꺼져 있었다 |
| 학교 IP 확보 방법 | ⬜ Docker + 쿠버네티스 구성 대기 (STATUS L4). 지금은 이 PC 의 IP 로 그대로 나가므로 학교망 밖에서는 응답이 없다 |

k8s 구성이 정해지면 이 골격을 그에 맞게 바꾸거나 지운다.

## 9. 합병 대비 — 다른 트랙에 넘길 작업

LLM 브랜치 결과는 병합하지 않고 PM이 샌드박스 · `main` 에서 다시 구현한다 (루트 `CLAUDE.md` § 4). 그때 각 트랙이 할 작업을 미리 적는다. 판단 시점 2026-11-03.

**기준점**

- 샌드박스: 분기(`a835e26`) 뒤 커밋은 문서 1건뿐이다. 상품 화면 · DAO · `data.sql` · CSS · 라이브러리 변경과 미커밋 변경이 없어 충돌 위험이 낮다. 이식 직전에 다시 확인한다.
- `main`: 골격만 있다. LLM 기능은 상품 · 리뷰 화면, DAO, 스키마 위에 얹히므로 4개 트랙의 기반 구현이 `main` 에 들어간 뒤에 옮긴다.

**트랙별 작업**

| 트랙 | 작업 | 시점 |
|---|---|---|
| DB | `data.sql` 샘플 보강 — 150자 넘는 상품 설명, 같은 상품의 리뷰 여러 건, 상품 수 확대. 리뷰는 `item_id` 가 구매 항목과 1:1 이라 `orders` · `order_item` · `payment` · `delivery` 를 맞춰 함께 늘린다 | 공통 · **없으면 F1 · F2 시연 불가** (현재 설명 9~14자, 리뷰 2건이 서로 다른 상품) |
| DB | 쿠버네티스로 가면 MySQL 컨테이너 첫 기동 시 `schema.sql` · `data.sql` 자동 적재 순서 정리 | L4 결정 후 |
| DB | (선택) 요약을 재시작 후에도 남기면 요약 저장 테이블 DDL | 저장 방식 결정 시 |
| ERD | `CRUD.md` 에 `/llm/desc`(product R) · `/llm/review`(review R) · `/llm/related`(product · category R) 행 추가 | 공통 |
| ERD | URL 매핑 계약(C1)에 `/llm/*` 두 주소 추가. LLM 응답 DTO 를 만들면 `ERD.md` DTO 계약에 등재 | 공통 |
| JSP | `shop/llm/**` · `Llm*` Service · Servlet · DAO 이식. `main` 에는 LLM 트랙이 없으므로 JSP 트랙 소유로 편입 | 공통 |
| JSP | `LlmProductDAO` 를 `ProductDAO` 로 합칠지 결정 — 연관 검색 후보용 "판매중 상품 전체(id · 이름 · 카테고리)" 조회 메서드 추가 | 공통 |
| JSP | `product/view.jsp` · `product/list.jsp` 진입점 재적용 — § 6 기존 파일 수정 기록이 체크리스트 | 공통 |
| JSP | `js/llm.js` 소유자 지정, `context.xml.example` 의 `<Environment>` 이식, 점검 모드에서 `/llm/*` 가 HTML 로 돌아올 때 `llm.js` 가 실패로 처리하는지 확인 | 공통 |
| UI | 요약 상자 · "생성 중" 표시 · "AI 생성" 표시 · 연관 상품 구역 클래스 신설 → `DECISIONS.md` 계약 추가. 연관 상품 카드는 기존 `prodcard` · `prodlist` 재사용 | 공통 |
| UI | `mockup/product_view.html` 에 요약 자리 2곳, 상품 목록 목업에 연관 검색 자리 추가 | 공통 |
| PM | `CLAUDE.md` 원칙 개정 — 외부 API 예외(Gemma 서버), 라이브러리 7개(gson), Docker · 쿠버네티스, 앱을 클러스터에 올리면 "배포 금지" 개정 | 공통 |
| PM | `WEB-INF/lib` gson 추가, `.gitignore` 에 `docker/llm-tunnel/.env`. `docker/` · k8s 파일을 `main` 에 넣을지 결정. L3 · L4 이관 | 공통 · L4 결정 후 |

**순서**: ① DB 샘플 보강 (시연 여부가 걸리고 주문 데이터까지 얽혀 가장 크다) → ② UI 클래스 계약 (JSP 진입점 마크업이 이 이름을 쓴다) → ③ ERD · JSP 이식 → ④ PM 원칙 개정 (L4 에 묶임).

## 10. 개발 단계와 병렬 분담

PM 지시(2026-11-04): 개발을 두 단계로 나누고 **두 단계를 병렬로**, 단계 2 안의 기능 3개도 **병렬로** 개발한다.

| 단계 | 목표 | 끝난 상태 |
|---|---|---|
| **1. 실제 LLM 연결** | 학교 Gemma 서버에 실제로 닿는 경로를 만든다 (Docker + 쿠버네티스, STATUS L4) | `llm/ollamaUrl` 로 보낸 요청에 Gemma 가 답한다 · 연결 확인 스크립트 통과 |
| **2. LLM 가정 구현** | 학교 서버가 있다고 가정하고(Ollama 형식) 기능 3개를 구현하고 시험 스크립트를 확보한다. 점검은 USB 소형 모델로 한다 (§ 11) | USB 모델로 F1 · F2 · F3 가 화면까지 동작 · 기능별 시험 스크립트 통과 |

### 두 단계를 잇는 계약

두 단계는 **`llm/ollamaUrl` 하나로만** 만난다. 단계 2 는 USB 모델 중계를, 단계 1 은 진짜 경로를 같은 주소(`http://localhost:31434/api/generate`)에 연다. 둘 다 끝나면 중계를 끄고 터널(또는 k8s 경로)을 켜는 것만으로 바뀐다 — Java · JSP 는 그대로다.

- 요청 · 응답 형식은 Ollama `/api/generate` 그대로다 (`model` · `prompt` · `stream=false`, F3 는 `format: "json"`). 중계도 이 형식을 지킨다.
- 같은 포트를 쓰므로 중계와 터널은 동시에 켜지 않는다.
- 개발 기준은 학교 서버(Ollama · `gemma3:12b`)다. USB 모델에 맞춰 형식이나 프롬프트를 바꾸지 않는다.

### 단계 1 — 실제 LLM 연결

| 소유 | 작업 |
|---|---|
| `docker/**` · (예정) `k8s/**` · `context.xml.example` · `tools/llm/check-server.sh` | L4 구성 확정 → 터널 또는 k8s 경로 구현 → `check-server.sh` 로 `/api/tags` 와 짧은 생성 요청 확인 |

Java 파일은 건드리지 않는다. L4(강의 안내)에 막혀 있다. 확인 스크립트 `check-server.sh` 는 만들어 USB 중계로 검증했고, 학교 서버 경로가 생기면 같은 스크립트로 확인한다.

### 단계 2 — LLM 가정 구현

**2-0 공통 선행** — 병렬로 들어가기 전에 한 번, 한 세션이 한다. 여러 기능이 같이 쓰는 파일을 여기서 끝내 두어 뒤에서 겹치지 않게 한다.

| 파일 | 작업 |
|---|---|
| `shop/llm/OllamaClient.java` | `generate(prompt, json)` — JSON 응답 형식 옵션 추가 |
| `shop/llm/LlmCache.java` | 메모리 캐시 · 동시 2건 제한 · 같은 키 중복 생성 방지 구현 |
| `js/llm.js` | 공통 로더 — `data-llm-url` 이 붙은 자리를 찾아 비동기로 채우고, 실패하면 숨긴다. 기능별 코드가 이 파일을 고치지 않게 한다 |
| `product/view.jsp` · `product/list.jsp` | F1 · F2 · F3 자리 3곳과 `llm.js` 를 붙인다 (§ 6 에 기록) |
| `LlmService` · `LlmServlet` 골격 | 기능별 파일 3쌍으로 나누고 지운다 |

**2-F1 · 2-F2 · 2-F3 병렬** — 서로 파일이 겹치지 않는다.

| 기능 | 소유 파일 | 시험 스크립트 |
|---|---|---|
| F1 상품 설명 요약 | `prompt/DescSummaryPrompt` · `LlmDescService` · `LlmDescServlet`(`/llm/desc`) | `tools/llm/test-f1.sh` |
| F2 리뷰 요약 | `prompt/ReviewSummaryPrompt` · `LlmReviewService` · `LlmReviewServlet`(`/llm/review`) | `tools/llm/test-f2.sh` |
| F3 연관 검색 | `prompt/RelatedSearchPrompt` · `LlmRelatedService` · `LlmRelatedServlet`(`/llm/related`) · `LlmProductDAO` | `tools/llm/test-f3.sh` |

- 시험 스크립트는 `start-local.sh` 로 USB 모델을 켠 상태에서 엔드포인트를 `curl` 로 불러 호출 생략 조건 · 정상 응답 · 결과 검증(F1 문장 수, F3 후보 밖 id 제거) · 서버 오류 시 응답을 확인한다. 단계 1 이 끝나면 같은 스크립트를 진짜 서버로 다시 돌린다.
- 샘플 데이터는 바꿀 수 없다(§ 9 DB). 지금 데이터로는 F1 · F2 가 "호출 생략" 경로만 탄다. 정상 경로는 USB 모델과 시험 스크립트(입력을 스크립트에 직접 넣는다)로 확인하고, 화면 시연은 § 9 의 DB 보강 뒤로 미룬다.

### 병렬 진행 규칙

- 단계 1 · 2-0 은 동시에 시작한다. 2-F1 · 2-F2 · 2-F3 는 2-0 이 커밋된 뒤 동시에 시작한다.
- 모두 이 폴더 · 이 브랜치 하나에서 일한다 (새 브랜치 · worktree 금지, 루트 `CLAUDE.md` § 4). 그래서 **자기 표의 파일만** 고치고, 커밋은 자기 파일만 경로로 지정한다. 푸시는 PM 이 한다.
- 표 밖의 파일(특히 공통 파일)을 고쳐야 하면 직접 고치지 말고 § 7 요청란에 적는다.

### 필요한 PM 결정

| # | 내용 |
|---|---|
| 소유 범위 | 루트 `CLAUDE.md` § 3 의 LLM 소유에 `tools/llm/**` 와 `Llm*` 접두어 DAO 를 더한다 |

## 11. 점검용 로컬 모델 (USB)

PM 지시(2026-11-04): 학교 서버(외부 API)를 받기 전까지, 구현한 기능의 점검은 **USB(`KM__JY`) 의 `LLM/` 폴더에 있는 소형 모델**로 한다. 기능 개발은 나중에 연결할 학교 서버 방식(Ollama `/api/generate`)을 그대로 따른다.

| 항목 | 값 (USB `LLM/README.md` · `VERSIONS.md` 기준) |
|---|---|
| 모델 | Qwen3-4B-Instruct-2507 Q4_K_M (약 2.5GB, 한국어 가능, Apache 2.0) |
| 실행기 | llama.cpp `llama-server` b12101 — USB 안의 실행 파일, PC 설치 없음 |
| API | OpenAI 호환 `POST /v1/chat/completions` · 모델 이름 `local-test-4b` · `127.0.0.1` 전용 |
| 메모리 | 약 5GB (모델 + 16K 컨텍스트) |

USB 모델은 OpenAI 형식만 말하고 Ollama 주소(`/api/generate` · `/api/tags`)는 404 다 (실측). 그래서 앱 코드를 바꾸지 않고 **중계 하나**를 둔다.

```
OllamaClient ──▶ 127.0.0.1:31434 /api/generate ──▶ [OllamaBridge] ──▶ 127.0.0.1:8081 /v1/chat/completions (USB 모델)
```

- 포트: USB 모델 서버의 기본 8080 은 Tomcat(그리고 지금 이 PC 에서는 Docker)과 겹쳐 **8081** 로 띄운다. 중계는 `llm/ollamaUrl` 의 31434 에 연다 — `context.xml` 은 그대로다.
- 변환: `prompt` → `messages[user]`, `format: "json"` → `response_format: json_object`, `temperature 0 · seed 42 · max_tokens 512` (USB README § 6), 응답 `choices[0].message.content` → `response`. 앱이 보내는 `gemma3:12b` 는 `local-test-4b` 로 바꾼다.
- 상류 오류(컨텍스트 초과 HTTP 400 등)는 상태 코드를 그대로 넘겨 앱의 오류 처리를 점검할 수 있게 한다. USB 서버가 꺼져 있으면 502.
- 중계는 점검 도구다. 앱(WAR)에 들어가지 않고, 라이브러리도 앱의 gson 을 빌려 쓴다.

**쓰는 법**

```bash
tools/llm/start-local.sh     # USB 모델(8081) + 중계(31434), Ctrl+C 로 둘 다 종료
tools/llm/check-server.sh    # 다른 터미널에서 연결 확인
```

처음 쓰는 맥이면 USB `LLM/README.md` 의 한 번만 할 절차(`chmod` · `xattr`)를 먼저 한다. USB 경로가 다르면 `LLM_USB=... tools/llm/start-local.sh`.

**검증 기록 (2026-11-04, macOS arm64)**

| 항목 | 결과 |
|---|---|
| USB 모델 기동 (`PORT=8081`) | 🟢 `/health` ok |
| 중계 `/api/tags` · `/api/generate` | 🟢 200 — 짧은 상품 설명 3문장 요약 약 2.4초 |
| `format: "json"` | 🟢 `{"ids":[1]}` 형태 JSON 응답 |
| `OllamaClient.generate()` 를 중계 주소로 호출 | 🟢 답변 수신 |
| 서버가 꺼진 주소로 호출 (USB 시나리오 3) | 🟢 `LlmException`("AI 서버에 연결하지 못했습니다.") + cause `ConnectException` |
| `start-local.sh` → `check-server.sh` | 🟢 PASS, 종료 시 두 프로세스 모두 내려감 |

**한계** — 점검은 "형식대로 동작하는가"까지다. 4B 모델이라 요약 품질 · 연관 검색 정확도는 학교 Gemma(12B)와 다르다. 품질 판단은 단계 1 이 끝난 뒤 같은 시험 스크립트를 학교 서버로 다시 돌려서 한다.
