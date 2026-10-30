package shop.util;

/*
  요청 단위 감사 문맥 — 지금 처리 중인 요청이 계정 전환 중인지.

  admin_log 를 쓰는 DAO 는 요청 객체를 모른다. 그래서 요청이 들어올 때
  BaseServlet 이 원래 DEV id 를 여기 싣고, AdminLogDAO 가 꺼내 detail 에
  impersonated_by=<devId> 를 덧붙인다 (ERD § 5.4-3).
  호출부마다 덧붙이게 두면 한 곳만 빠뜨려도 실제 행위자가 사라진다.
*/
public class AuditContext {

	private static final ThreadLocal<Integer> IMPERSONATOR = new ThreadLocal<Integer>();

	private AuditContext() {
	}

	public static void setImpersonator(int devId) {
		if (devId > 0) {
			IMPERSONATOR.set(Integer.valueOf(devId));
		} else {
			IMPERSONATOR.remove();
		}
	}

	public static int impersonator() {
		Integer value = IMPERSONATOR.get();
		return value == null ? 0 : value.intValue();
	}

	/* 톰캣은 스레드를 재사용한다. 요청이 끝나면 반드시 비운다. */
	public static void clear() {
		IMPERSONATOR.remove();
	}
}
