# LLM API (LLM 브랜치 전용)

> 소유: LLM API 세션 · 상태: 🟡 기능 3종 골격 · 학교 서버 k8s 중계 검증 완료 · 갱신 2026-11-09
> `LLM_integration` 전용. 산출물은 `main` 으로 병합하지 않고 PM 이 다시 구현한다. 세션 규칙 `src/main/java/shop/llm/CLAUDE.md`, 소유 범위 루트 `CLAUDE.md` § 3.

## 1. 범위

LLM 모델 이식 · 기능 로직 · JSP 연결만 한다. ERD · DDL · 샘플 데이터 · CSS · 목업, LLM 과 무관한 배선은 하지 않는다 — 기반 구현(샌드박스 `a835e26`)은 읽기만 한다.

## 2. 파일 지도

흐름: `JSP → js/llm.js → Llm{Desc|Review|Related}Servlet → 같은 이름 Service → shop.llm(캐시 · 프롬프트 · 클라이언트) → LLM`

| 파일 | 담당 | 상태 |
|---|---|---|
| `shop/llm/OllamaClient` · `LlmException` | Ollama 호출 · 화면용 오류. JSON 형식 옵션은 2-0 에서 추가 | 🟢 |
| `shop/llm/LlmCache` | 메모리 캐시 · 동시 2건 제한 | ⬜ 골격 |
| `shop/llm/prompt/{Desc,Review,RelatedSearch}…Prompt` | F1 · F2 · F3 프롬프트 | ⬜ 골격 |
| `LlmService` · `LlmServlet` | 2-0 에서 기능별 3쌍으로 나누고 지운다 (지금 501 응답) | ⬜ 골격 |
| `Llm{Desc,Review,Related}Service` · `…Servlet` | `/llm/desc` · `/llm/review` · `/llm/related` | ⬜ 2-0 |
| `shop/dao/LlmProductDAO` | F3 후보 — 판매중 상품 전체(id · 이름 · 카테고리) | ⬜ 2-0 |
| `js/llm.js` · `product/view.jsp` · `product/list.jsp` | 공통 로더 · 진입점 | ⬜ 2-0 |
| `k8s/llm-relay.yaml` · `tools/llm/start-relay.sh` | 학교 서버 중계 파드 · port-forward 시작 (§ 8) | 🟢 검증 |
| `tools/llm/check-server.sh` | 연결 확인 | 🟢 검증 |
| `tools/llm/test-f{1,2,3}.sh` | 기능별 시험 스크립트 | ⬜ 2-F* |

`WebContents/llm/` 은 라우팅 페이지 자리다. 기능 3개가 모두 기존 상품 화면에 붙어 지금은 비어 있다.

## 3. 현재 상태

- 공급자 · 모델: 수업 Ollama 서버 · `gemma3:12b`, 키 없음 (L1). gson 2.15.0 추가.
- 설정: `context.xml` `<Environment>` `llm/ollamaUrl`(= `localhost:31434`) · `llm/ollamaModel`.
- 학교 서버 경로: 수업 GPU 클러스터의 중계 파드 + `kubectl port-forward` (L4, § 8). 점검도 이 경로로 한다. 로컬 LLM 점검(USB 모델)은 2026-11-09 말소.
- 전체 소스 javac 0 오류. JSP 연결 착수 전.

## 4. 수업 실습 코드에서 가져온 것

실습 `my.ai.OllamaClient` · `OllamaClientMySQL` · JSP 2개를 분석해 반영했다.

- **유지**: `/api/generate` 요청(`model` · `prompt` · `stream=false`), 타임아웃 10초 · 180초, `response` 검사, UTF-8, 제출 버튼 잠금, 인터럽트 처리. 프롬프트 패턴 `역할 → 근거 범위 → 수치 정확 → 추측 금지 → 데이터 속 문자열 명령 금지 → [데이터 JSON] → [질문]`.
- **변경**: 주소 · 모델 상수 → JNDI. `DriverManager` + 소스 계정 → `DBUtil` · 기존 DAO. 오류 본문 화면 노출 → `LlmException` 안내 + 로그. 매번 `new HttpClient` → 공유. 스크립틀릿 · 인라인 CSS → 서블릿 MVC · 기존 클래스.

## 5. 기능 (L2)

셋 다 공개 상품 화면, 비로그인 · USER 가 본다.

| # | 기능 | 화면 · 주소 | 생략 조건 | 프롬프트 · 검증 |
|---|---|---|---|---|
| F1 | 상품 설명 요약 | `/product/view` 설명 위 · `/llm/desc?productId=` | 설명 150자 이하 → 원문 | 설명만 근거 · 3문장 이내 · 사양 추측 금지. 넘치면 앞 3문장 |
| F2 | 리뷰 요약 | `/product/view` 리뷰 위 · `/llm/review?productId=` | 리뷰 2건 미만 | 별점 · 본문만(작성자 제외) · 다수 의견 위주. 평균 · 건수는 DB 값 |
| F3 | 연관 검색 | `/product/list?keyword=` 결과 아래 · `/llm/related?keyword=` | 검색어 없음 | 후보 JSON → id 배열, `format: "json"`. 후보 밖 id · LIKE 결과 중복 제거, 최대 8개 |

**공통 설계**
- 응답이 최대 180초라 렌더 중에 부르지 않는다. 페이지는 즉시 그리고 `js/llm.js` 가 비동기로 채운다. 실패하면 자리만 숨긴다.
- 캐시는 메모리(`ConcurrentHashMap`). 키 F1 = id + 설명 해시, F2 = id + 리뷰 수 + 최신 시각, F3 = 정규화 검색어. 재시작 시 사라진다.
- 동시 2건 제한(`Semaphore`) · 같은 키 중복 생성 방지 (L3 제안).

## 6. 기존 파일 수정 기록

진입점을 붙이려고 기반 구현 파일을 고친 경우만 적는다. `main` 재구현 때 이식 목록이 된다.

| 파일 | 수정 내용 | 커밋 |
|---|---|---|
| — | — | — |

## 7. 요청 · 미결

| # | 내용 | 상태 |
|---|---|---|
| L3 | 호출 제한 — 메모리 캐시 + 동시 2건 | 승인 대기 |
| L4 | 쿠버네티스 구성 — 중계만 클러스터에, 접근은 port-forward 로만 (§ 8) | 결정 (2026-11-09) |

## 8. 학교 서버 접속 (L4)

학교 Ollama 서버(`220.149.119.234:31434`)는 학교 IP 요청만 받는다. 수업 GPU 클러스터에 중계 파드만 올려 푼다. 앱은 Mac 의 Tomcat 에 그대로 두므로 "배포 금지" 원칙은 바뀌지 않는다.

```
앱(llm/ollamaUrl) → localhost:31434 → kubectl port-forward → llm-relay 파드(studXX namespace, socat) → 220.149.119.234:31434
```

- 중계: `k8s/llm-relay.yaml` — Deployment 1개(`alpine/socat`), Service 없음. 클러스터 밖으로 포트를 열지 않고, port-forward 는 Mac 의 `127.0.0.1` 에만 묶는다.
- 준비(1회): 서버에서 받은 kubeconfig 를 `~/.kube/config` 에 두고 cluster 이름은 학번, `server` 는 `https://localhost:36443` 으로 바꾼다(`tls-server-name` 은 유지). `kubectl` 은 Docker Desktop 에 들어 있는 것을 쓴다. 클러스터 API(6443)도 학교망 밖에서 막혀 있어 SSH 터널로 연다.

```bash
ssh -N -o ExitOnForwardFailure=yes -o ServerAliveInterval=30 -L 36443:220.149.119.234:6443 studXX@220.149.119.234   # 클러스터 API 터널, 켜 둔다
tools/llm/start-relay.sh       # 중계 파드 apply + 127.0.0.1:31434 port-forward, Ctrl+C 로 port-forward 종료
tools/llm/check-server.sh      # 연결 확인
tools/llm/start-relay.sh down  # 중계 파드 삭제
```

- 쓸 수 있는 모델(2026-11-09 `/api/tags`): `gemma3:12b` · `qwen3:8b` · 임베딩 `bge-m3` · `nomic-embed-text`. 공용 서버라 다른 사용자 부하에 따라 응답이 늦어질 수 있다.
- 학생 계정은 namespace 안 권한만 있다(노드 조회 불가). GPU 는 Ollama 서버가 쓰므로 중계 파드는 GPU 를 요청하지 않는다.

검증(2026-11-09, macOS arm64, context `stud08`): 클러스터 안 파드에서 `/api/tags` · `gemma3:12b` 생성 응답, 중계 파드 + port-forward 로 `check-server.sh` 3단계 PASS(짧은 요청 0.1~0.2초, 모델이 내려가 있으면 첫 요청에 로딩 약 3.6초). `start-relay.sh` → `check-server.sh` PASS → `start-relay.sh down` 정리까지 확인.

## 9. 합병 대비 — 다른 트랙 작업

샌드박스는 분기 뒤 관련 파일 변경이 없어 충돌 위험이 낮다(2026-11-03, 이식 직전 재확인). `main` 은 기반 구현이 먼저 들어가야 한다.

| 트랙 | 작업 |
|---|---|
| DB | `data.sql` — 150자 넘는 설명, 상품당 리뷰 여러 건(`review.item_id` 1:1 이라 주문 · 결제 · 배송 함께), 상품 수 확대. **없으면 F1 · F2 시연 불가.** k8s 면 MySQL 초기 적재 순서. 요약 영속화 시 테이블 |
| ERD | `CRUD.md` · URL 계약에 `/llm/desc` · `/llm/review` · `/llm/related`. LLM DTO 를 만들면 DTO 계약 |
| JSP | `shop/llm/**` · `Llm*` 편입(JSP 소유), `LlmProductDAO` 를 `ProductDAO` 로 합칠지, 진입점 재적용(§ 6), `llm.js` 소유, `<Environment>` 이식, 점검 모드 응답 처리 |
| UI | 요약 상자 · 생성 중 · AI 생성 표시 · 연관 상품 구역 클래스와 목업. 카드는 `prodcard` · `prodlist` 재사용 |
| PM | 원칙 개정(외부 API 예외 · gson · Docker/k8s · 배포), lib · `.gitignore`, `k8s/` 를 `main` 에 둘지, L3 · L4 이관 |

순서: DB 샘플 → UI 클래스 계약 → ERD · JSP → PM 원칙.

## 10. 개발 단계와 병렬 분담

두 단계를 병렬로, 단계 2 의 기능 3개도 병렬로 개발한다 (2026-11-04). 두 단계는 `llm/ollamaUrl` 하나로만 만난다 — k8s 중계(§ 8)가 `localhost:31434` 를 연다. 형식은 Ollama `/api/generate` 그대로다.

| 단계 | 소유 | 끝난 상태 |
|---|---|---|
| 1 실제 연결 | `k8s/**` · `tools/llm/start-relay.sh` · `context.xml.example` | 🟢 학교 Gemma 가 `check-server.sh` 를 통과 (2026-11-09) |
| 2-0 공통 선행 | `OllamaClient` JSON 옵션 · `LlmCache` · `llm.js` · 진입점 3곳 · 기능별 파일 분리 | 커밋되면 2-F* 시작 |
| 2-F1 | `DescSummaryPrompt` · `LlmDesc*` · `test-f1.sh` | 학교 서버로 화면까지 동작 |
| 2-F2 | `ReviewSummaryPrompt` · `LlmReview*` · `test-f2.sh` | 〃 |
| 2-F3 | `RelatedSearchPrompt` · `LlmRelated*` · `LlmProductDAO` · `test-f3.sh` | 〃 |

- 모두 이 폴더 · 이 브랜치에서 일한다. 자기 표의 파일만 고치고 경로로 커밋한다. 표 밖 파일은 § 7 에 요청한다. 푸시는 PM.
- 시험 스크립트는 생략 조건 · 정상 · 결과 검증 · 서버 오류를 `curl` 로 확인한다. 지금 샘플 데이터로는 F1 · F2 가 생략 경로만 타므로 정상 경로는 스크립트에 입력을 직접 넣어 확인한다.