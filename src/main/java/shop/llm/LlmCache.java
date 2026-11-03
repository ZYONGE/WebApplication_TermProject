package shop.llm;

/*
  [공통] 생성 결과 메모리 캐시 + 동시 호출 제한. (docs/tracks/LLM.md § 5 공통 설계)

  테이블을 만들 수 없으므로 결과는 서버 메모리에만 둔다. Tomcat 재시작 시 사라진다.
  키는 원본이 바뀌면 함께 바뀌게 짓는다 — 무효화 로직 없이 새로 만들어진다.
    F1  desc:{productId}:{설명 해시}
    F2  review:{productId}:{리뷰 수}:{최신 리뷰 시각}
    F3  related:{정규화한 검색어}

  구현 예정
    - ConcurrentHashMap 저장소 (상한을 두고 넘으면 오래된 것부터 버린다)
    - Semaphore(2) — 공용 GPU 서버에 동시에 2건까지만 보낸다 (STATUS L3 제안)
    - 같은 키를 이미 생성 중이면 새로 보내지 않고 그 결과를 기다린다
*/
public class LlmCache {

	public static final int MAX_CONCURRENT = 2;

	private LlmCache() {
	}
}
