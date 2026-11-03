package shop.llm.prompt;

/*
  [F3] 연관 검색 프롬프트. (docs/tracks/LLM.md § 5)

  입력: 검색어 + 판매중 상품 후보(id · 이름 · 카테고리) JSON
  출력: 키워드에 해당하거나 인접한 상품 id 의 JSON 배열

  규칙
    - 검색어가 비었으면 LLM 을 부르지 않는다.
    - Ollama 요청에 format: "json" 을 준다 — OllamaClient 에 형식 옵션 추가 필요.
    - 후보 목록에 없는 id 는 버린다. 기존 LIKE 검색 결과에 있는 상품은 뺀다.
    - 최대 MAX_RESULTS 개.
*/
public final class RelatedSearchPrompt {

	public static final int MAX_RESULTS = 8;

	private RelatedSearchPrompt() {
	}
}
