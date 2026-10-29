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

import shop.util.WebUtil;

/*
  로그인이 필요한 영역 — /cart/*, /order/*, /mypage/*, /wish/*

  ERD § 5 의 필터 목록에는 없다. 거기서 정한 것은 권한(ADMIN · DEV) 경계이고,
  이쪽은 "로그인 여부" 경계라 성격이 다르다. 서블릿마다 같은 검사를 반복하는 대신
  한 군데로 모았다 — 빠뜨린 서블릿이 생기지 않는다.

  돌아갈 주소를 쿼리로 넘겨 로그인 후 원래 화면으로 복귀시킨다.
*/
public class LoginFilter implements Filter {

	public void init(FilterConfig config) throws ServletException {
	}

	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
			throws IOException, ServletException {
		HttpServletRequest req = (HttpServletRequest) request;
		HttpServletResponse res = (HttpServletResponse) response;

		if (!WebUtil.isLogin(req)) {
			String back = URLEncoder.encode(WebUtil.currentPath(req), "UTF-8");
			res.sendRedirect(req.getContextPath() + "/member/login?returnUrl=" + back);
			return;
		}
		chain.doFilter(request, response);
	}

	public void destroy() {
	}
}
