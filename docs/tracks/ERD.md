# 트랙 3 — ERD 설계 (샌드박스)

> 소유: ERD 세션 · 쓰기: `docs/ERD.md` · `docs/CRUD.md` · 이 문서 · 갱신 2026-10-30

## 현재 상태

| 항목 | 상태 |
|---|---|
| ERD | 17 테이블 · 뷰 5 · FK 21 · CHECK 8 · 인덱스 9 — 확정, DB 구축 반영 |
| CRUD | 화면 36 × 17 테이블 · 역할별 접근 구분 |
| DTO 계약 | 20개 + `PageDTO` |
| 역할 격리 | DB · JSP · UI 반영 완료 |
| main | `docs/ERD.md` · `docs/CRUD.md` 미작성 — 환원 범위 결정 대기 (STATUS M4) |

## 열린 요청 — 다른 트랙이 처리

| 대상 | 내용 | STATUS |
|---|---|---|
| JSP | `admin_log.action` 에 `REVIEW_DELETE` · `CONFIG_UPDATE` 반영. 현재 설정 변경이 `SHOP_STATUS_TOGGLE` 로 기록됨 | 전달 사항 |
| DB | `product.status` 2값(`'품절'` 제거) DDL 반영 확인 | 전달 사항 |
| UI ↔ JSP | ADMIN·DEV 구매 안내문 클래스 불일치 — 목업 `empty` / `product/view.jsp` `field_help` | 미등재 (C5 와 함께 처리 요망) |

## PM 결정 대기 중 ERD 문서에 닿는 것

| STATUS | 항목 | 결정 후 ERD 쪽 작업 |
|---|---|---|
| P2 | 샌드박스 `data.sql` 재실행 | 없음 |
| P4 | 비밀번호 해시 vs 평문 | `member.password` 설명 수정 |
| C1 | 화면 목록 · URL 매핑 | `CRUD.md` URL 열 교체 |
| C2 | 팝업 방식 | 별창이면 `CRUD.md` 에 팝업 행 추가 |
| C4 | `OrderDTO` 조인 필드 4개 · EL 예약어 프로퍼티명 금지 | `ERD.md` § 9 DTO 계약 갱신 |
| C6 | `soldout` = 장바구니·찜 "재고 없음" 전용 | 없음 (ERD 반영됨) |
| D1 | 상품은 리프 카테고리에만 등록 | `ERD.md` § 7 규칙 한 줄 |
| D3 | 우편번호 직접 입력 | 없음 (현재 가정) |
| M4 | main 환원 범위 | main 문서 작성 |

## PM 참고

- STATUS 전달 사항 중 "ERD → JSP: 회원관리 역할 토글 제거 · 역할별 헤더 이식" 은 **이미 반영됨** — `admin/members.jsp` 에 역할 변경 없음(남은 `role` select 는 검색 필터), `common/header.jsp` 역할 분기 확인.
