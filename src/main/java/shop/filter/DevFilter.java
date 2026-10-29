package shop.filter;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import shop.dto.MemberDTO;
import shop.util.WebUtil;

/*
  /dev/* — DEV 만 (ERD § 5).

  ★ 세션의 role 이 아니라 "원래 DEV 로 로그인했는가" 를 본다.
    /dev/loginAs 로 일반 회원에 전환하면 세션의 role 은 USER 가 되는데,
    그때도 /dev 로 돌아올 수 있어야 전환을 해제할 수 있다.
    그래서 전환 시 원래 계정을 세션에 따로 보관하고(DEV_ORIGIN) 그것도 함께 본다.
*/
public class DevFilter implements Filter {

	/* /dev/loginAs 로 전환하기 전의 원래 DEV 계정 */
	public static final String SESSION_DEV_ORIGIN = "devOrigin";

	public void init(FilterConfig config) throws ServletException {
	}

	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
			throws IOException, ServletException {
		HttpServletRequest req = (HttpServletRequest) request;
		HttpServletResponse res = (HttpServletResponse) response;

		if (!isDev(req)) {
			res.sendRedirect(req.getContextPath() + "/");
			return;
		}
		chain.doFilter(request, response);
	}

	private boolean isDev(HttpServletRequest req) {
		MemberDTO member = WebUtil.loginMember(req);
		if (member != null && "DEV".equals(member.getRole())) {
			return true;
		}
		if (req.getSession(false) == null) {
			return false;
		}
		Object origin = req.getSession(false).getAttribute(SESSION_DEV_ORIGIN);
		return origin instanceof MemberDTO && "DEV".equals(((MemberDTO) origin).getRole());
	}

	public void destroy() {
	}
}
