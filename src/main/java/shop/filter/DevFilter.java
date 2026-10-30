package shop.filter;

import java.io.IOException;
import java.net.URLEncoder;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import shop.util.RoleUtil;
import shop.util.WebUtil;

/*
  /dev/* — role = 'DEV' 만 (ERD § 5.5).

  ★ 판정은 "지금 세션의 역할" 이다. 계정 전환 중이면 세션 역할이 대상 계정 것이므로
    /dev 전체가 막힌다. 그래야 USER 시점 재현이 성립하고, 전환된 계정 이름으로
    dev 조작이 기록되는 일이 생기지 않는다.
  ★ 예외는 복귀 경로 /dev/loginAs/return 하나 — 세션 역할 대신 impersonatorId 로 판정한다.
*/
public class DevFilter implements Filter {

	private static final String RETURN_PATH = "/dev/loginAs/return";

	public void init(FilterConfig config) throws ServletException {
	}

	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
			throws IOException, ServletException {
		HttpServletRequest req = (HttpServletRequest) request;
		HttpServletResponse res = (HttpServletResponse) response;

		String path = req.getRequestURI().substring(req.getContextPath().length());
		if (RETURN_PATH.equals(path) && RoleUtil.impersonatorId(req) > 0) {
			chain.doFilter(request, response);
			return;
		}

		String role = RoleUtil.role(req);
		if (role == null) {
			String back = URLEncoder.encode(WebUtil.currentPath(req), "UTF-8");
			res.sendRedirect(req.getContextPath() + "/member/login?returnUrl=" + back);
			return;
		}
		if (!"DEV".equals(role)) {
			req.getSession().setAttribute("flashError", "개발자 계정으로만 접근할 수 있습니다.");
			res.sendRedirect(req.getContextPath() + RoleUtil.homeOf(role));
			return;
		}
		chain.doFilter(request, response);
	}

	public void destroy() {
	}
}
