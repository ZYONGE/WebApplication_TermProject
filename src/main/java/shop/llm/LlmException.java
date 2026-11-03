package shop.llm;

/*
  LLM 호출 실패. 메시지는 화면에 그대로 보여도 되는 안내 문구로만 쓴다.
  원인(오류 응답 본문 · 연결 예외)은 cause 에 실어 서버 로그로 남긴다.
*/
public class LlmException extends Exception {

	private static final long serialVersionUID = 1L;

	public LlmException(String message) {
		super(message);
	}

	public LlmException(String message, Throwable cause) {
		super(message, cause);
	}
}
