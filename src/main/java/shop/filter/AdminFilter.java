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
  /admin/* — role = 'ADMIN' 만 (ERD § 5.5).

  DEV 도 거부한다. 예전 규정(ADMIN · DEV 모두 허용)은 폐지됐다 —
  관리자는 상품 · 주문 관리, 개발자는 데이터 · 상태 조작으로 입장이 갈린다.
*/
public class AdminFilter implements Filter {

	public void init(FilterConfig config) throws ServletException {
	}

	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
			throws IOException, ServletException {
		HttpServletRequest req = (HttpServletRequest) request;
		HttpServletResponse res = (HttpServletResponse) response;

		String role = RoleUtil.role(req);
		if (role == null) {
			/* 구매 경로와 같은 규칙 — 로그인 후 원래 화면으로 돌아온다. */
			String back = URLEncoder.encode(WebUtil.currentPath(req), "UTF-8");
			res.sendRedirect(req.getContextPath() + "/member/login?returnUrl=" + back);
			return;
		}
		if (!"ADMIN".equals(role)) {
			req.getSession().setAttribute("flashError", "관리자 계정으로만 접근할 수 있습니다.");
			res.sendRedirect(req.getContextPath() + RoleUtil.homeOf(role));
			return;
		}
		chain.doFilter(request, response);
	}

	public void destroy() {
	}
}
