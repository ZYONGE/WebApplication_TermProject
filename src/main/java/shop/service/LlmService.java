package shop.service;

/*
  LLM 기능 3종의 업무 계층. 서블릿은 이 클래스만 부른다. (docs/tracks/LLM.md § 5)

    F1 summarizeDescription(productId)  상품 설명 요약     → shop.llm.prompt.DescSummaryPrompt
    F2 summarizeReviews(productId)      리뷰 요약          → shop.llm.prompt.ReviewSummaryPrompt
    F3 findRelated(keyword)             연관 검색 상품 목록 → shop.llm.prompt.RelatedSearchPrompt

  흐름: 기존 DAO 로 읽기 → 호출 생략 조건 확인 → LlmCache 조회 → 프롬프트 → OllamaClient → 결과 검증
  DB 는 읽기만 한다.
*/
public class LlmService {
}
