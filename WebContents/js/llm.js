/*
  LLM 기능 3종의 화면 쪽. 페이지를 다 그린 뒤 JSON 엔드포인트를 비동기로 불러 자리를 채운다.
  (docs/tracks/LLM.md § 5)

    F1 상품 설명 요약  product/view.jsp 의 자리 → GET /llm/summary?type=desc&productId=
    F2 리뷰 요약       product/view.jsp 의 자리 → GET /llm/summary?type=review&productId=
    F3 연관 검색       product/list.jsp 의 자리 → GET /llm/related?keyword=

  규칙
    - 실패(네트워크 · 501 · ok:false)하면 자리를 숨긴다. 기존 화면은 건드리지 않는다.
    - 답변은 textContent 로만 넣는다 (innerHTML 금지).
    - 응답이 최대 180초라 기다리는 동안 "AI 요약을 만드는 중" 문구를 둔다.
*/
