package shop.util;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import shop.dto.MemberDTO;

/* 요청 파라미터 · 세션에서 값을 꺼낼 때의 공통 처리. */
public class WebUtil {

	/* 세션에 로그인 회원을 담는 키. 이 이름을 쓰는 곳이 여러 군데라 상수로 고정한다. */
	public static final String SESSION_MEMBER = "loginMember";

	private WebUtil() {
	}

	public static int intParam(HttpServletRequest request, String name, int defaultValue) {
		return toInt(request.getParameter(name), defaultValue);
	}

	public static int toInt(String value, int defaultValue) {
		if (StringUtil.isEmpty(value)) {
			return defaultValue;
		}
		try {
			return Integer.parseInt(value.trim());
		} catch (NumberFormatException e) {
			return defaultValue;
		}
	}

	public static String param(HttpServletRequest request, String name) {
		return StringUtil.trim(request.getParameter(name));
	}

	public static String param(HttpServletRequest request, String name, String defaultValue) {
		return StringUtil.nvl(request.getParameter(name), defaultValue);
	}

	/* 로그인한 회원. 비로그인이면 null. */
	public static MemberDTO loginMember(HttpServletRequest request) {
		HttpSession session = request.getSession(false);
		if (session == null) {
			return null;
		}
		return (MemberDTO) session.getAttribute(SESSION_MEMBER);
	}

	public static int loginMemberId(HttpServletRequest request) {
		MemberDTO member = loginMember(request);
		return member == null ? 0 : member.getMemberId();
	}

	public static boolean isLogin(HttpServletRequest request) {
		return loginMember(request) != null;
	}

	/* 로그인 후 돌아올 경로 — 쿼리스트링까지 보존한다. */
	public static String currentPath(HttpServletRequest request) {
		String uri = request.getRequestURI();
		String query = request.getQueryString();
		return query == null ? uri : uri + "?" + query;
	}
}
