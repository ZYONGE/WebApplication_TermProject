# 현황 보드

> PM만 수정한다. 트랙별 상세는 `docs/tracks/*.md`.

## 트랙

| 트랙 | 작업 위치 | 상태 |
|---|---|---|
| UI | 샌드박스 | CSS 청사진 17개 · 목업 8개 작성, 역할별 헤더·구매 영역 작업 중 |
| JSP 배선 | 샌드박스 | 전 화면 배선, 3역할 격리 반영, 화면 목록·URL 기록 완료 |
| ERD | 샌드박스 | 17테이블 · 뷰 5 · CRUD 매트릭스 |
| DB Schema | 샌드박스 | DDL · 샘플 데이터 적용 (스키마 `webapplication_sandbox`) |

`main` 은 골격과 규약만 있다.

## PM 결정 대기

**환경 · 운영**

| # | 항목 | 요청 |
|---|---|---|
| P1 | main 스키마명 (제안 `shopdb`) — 관리자 계정으로 DB 생성·GRANT 1회 필요 | DB |
| P2 | 샌드박스 `data.sql` 재실행 승인 — 역할 위반 4행 제거, 테스터 데이터도 지워짐 | DB · ERD |
| P3 | 샌드박스 미커밋 변경의 커밋 주체와 푸시 시점 | 전 트랙 |
| P4 | 비밀번호 평문 vs 해시 (ERD는 해시, 구현·데이터는 평문) | JSP · DB |
| P5 | 샘플 이미지 404 — 공백 유지 / `thumbnail` NULL+기본 이미지 / 파일 준비 | JSP · DB · UI |

**계약**

| # | 항목 | 요청 |
|---|---|---|
| C1 | 화면 목록 · URL 매핑 확정 (JSP 기록 vs `CRUD.md` 표기 대조) | JSP · ERD |
| C2 | 팝업 방식 — 현재 `<dialog>` 인페이지 / 별창 | JSP · ERD |
| C3 | doctype — 현재 HTML5 / 4.01 Transitional | JSP |
| C4 | DTO 계약: `OrderDTO` 조인 필드 4개 추가, EL 예약어 프로퍼티명 금지 | JSP |
| C5 | CSS 클래스 계약 보완: 부록 B 밖 클래스, `notice_*` 접두어, `qna_answer` 귀속, `popup_sub` 신설 | JSP · UI |
| C6 | `soldout` 을 장바구니·찜의 "재고 없음" 전용으로 변경 | UI · ERD |
| C7 | `pageCss` 설정을 JSP 쪽으로 단일화 (서블릿 호출 제거) | JSP |

**설계 · 규칙**

| # | 항목 | 요청 |
|---|---|---|
| D1 | 상품은 리프 카테고리에만 등록 가능하게 할지 | JSP |
| D2 | 고객 주문 목록 N+1 해소용 뷰 추가 (선택) | JSP |
| D3 | 우편번호는 직접 입력 (외부 API 금지) | ERD |
| D4 | 반응형 — D-06(고정폭 1200px) 유지 / 테스터 지적 반영 | UI |
| D5 | JSP 직접 접근 차단 — 필터 유지 / `/WEB-INF/views/` 이동 | JSP |

**main 반영 후보**

| # | 항목 | 요청 |
|---|---|---|
| M1 | 규약 편입: 서블릿 `/xxx/*` 매핑 금지, `<jsp-config>` UTF-8, JDBC `connectionCollation=utf8mb4_unicode_ci` | JSP · DB |
| M2 | 제출 전 제거: `error500.jsp` 예외 노출, 로그인 화면 테스트 계정 안내 | JSP |
| M3 | Build & Run 문서: CLI로 컨텍스트 선언 시 `context.xml` 무시됨 | JSP |
| M4 | 샌드박스 ERD 판단의 main 환원 범위 (DATETIME, 3역할 격리, 품절 제거, 제약) | ERD |

## PM 전달 대기 (결정 불필요)

- ERD → JSP: `admin_log.action` 어휘 `REVIEW_DELETE` · `CONFIG_UPDATE` 확정. 설정 변경이 아직 `SHOP_STATUS_TOGGLE` 로 기록되는 중.
- DB → JSP: 취소 주문의 배송 상태는 `order_status` 를 먼저 보고 렌더 (delivery에 '취소' 없음). 판매 집계는 6일 범위로 자름. qna 답변·취소·환불·배송중 전환은 관련 컬럼을 같은 UPDATE에서 함께 기록.
- ERD → DB: `product.status` 품절 제거 DDL 반영 확인 필요.
- ERD → JSP: 회원관리 역할 토글 제거(`admin/members.jsp`), 역할별 헤더 이식.
- JSP → UI: 임시 `js/ui.js` 인수 (`openPopup` · `closePopup` 두 함수만 사용).
- ERD의 `mermaid.min.js` 요청은 무효 (ERD 다이어그램은 이미지로 넣기로 결정).
- `error404.jsp` 에 공통 레이아웃이 없는 것은 의도일 수 있음 (DB 장애 시 헤더 조회 실패 방지). 통일 전 확인.

## 인지 사항

- 로컬 MySQL root 비밀번호가 빈 값이다. 머신 전체에 영향.
- Homebrew mysql 클라이언트 기본 문자셋이 latin1이라 한글이 이중 인코딩된다. `~/.my.cnf` 에 utf8mb4 설정 완료.
