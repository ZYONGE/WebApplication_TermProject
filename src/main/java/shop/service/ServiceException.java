package shop.service;

/*
  업무 규칙 위반 — 화면에 그대로 보여줄 메시지를 담는다.

  SQLException 과 구분하는 이유: SQLException 은 "고장" 이고 이것은 "거절" 이다.
  고장은 에러 페이지로 보내고, 거절은 사용자가 고칠 수 있도록 원래 화면에 돌려준다.
*/
public class ServiceException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public ServiceException(String message) {
		super(message);
	}
}
