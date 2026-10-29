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
  /admin/* — ADMIN 또는 DEV 만 (ERD § 5).

  ★ 필터는 "경로 접근" 만 막는다. "권한 상승"(ADMIN 이 남에게 DEV 를 주는 것)은
    AdminService.changeRole 에서 막는다. 둘은 다른 문제다.
*/
public class AdminFilter implements Filter {

	public void init(FilterConfig config) throws ServletException {
	}

	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
			throws IOException, ServletException {
		HttpServletRequest req = (HttpServletRequest) request;
		HttpServletResponse res = (HttpServletResponse) response;

		MemberDTO member = WebUtil.loginMember(req);
		if (member == null) {
			res.sendRedirect(req.getContextPath() + "/member/login");
			return;
		}
		if (!"ADMIN".equals(member.getRole()) && !"DEV".equals(member.getRole())) {
			res.sendRedirect(req.getContextPath() + "/");
			return;
		}
		chain.doFilter(request, response);
	}

	public void destroy() {
	}
}
