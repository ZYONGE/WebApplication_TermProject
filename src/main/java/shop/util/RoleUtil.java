package shop.util;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import shop.dto.MemberDTO;

/*
  역할 경계 — USER · ADMIN · DEV 3개 입장을 완전히 격리한다 (ERD § 5).

  필터 세 개가 같은 판정을 하므로 한 군데로 모았다.
  거부할 때는 메인이 아니라 "그 역할의 홈" 으로 보낸다.
*/
public class RoleUtil {

	/* /dev/loginAs 로 전환하기 전의 원래 DEV member_id (ERD § 5.4) */
	public static final String SESSION_IMPERSONATOR = "impersonatorId";

	private RoleUtil() {
	}

	public static String role(HttpServletRequest request) {
		MemberDTO member = WebUtil.loginMember(request);
		return member == null ? null : member.getRole();
	}

	public static boolean is(HttpServletRequest request, String role) {
		return role.equals(role(request));
	}

	/* 역할별 홈. 비로그인은 null — 호출부가 로그인 화면으로 보낸다. */
	public static String homeOf(String role) {
		if ("ADMIN".equals(role)) {
			return "/admin/dashboard";
		}
		if ("DEV".equals(role)) {
			return "/dev/";
		}
		if ("USER".equals(role)) {
			return "/";
		}
		return null;
	}

	/* 계정 전환 중이면 원래 DEV 의 id, 아니면 0 */
	public static int impersonatorId(HttpServletRequest request) {
		HttpSession session = request.getSession(false);
		if (session == null) {
			return 0;
		}
		Object value = session.getAttribute(SESSION_IMPERSONATOR);
		return value instanceof Integer ? ((Integer) value).intValue() : 0;
	}
}
