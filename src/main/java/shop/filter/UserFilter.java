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
  구매 경로 — role = 'USER' 만 (ERD § 5.5).
      /cart/*  /order/*  /mypage/*  /wish/*
  (리뷰 작성 · 수정 · 삭제와 문의 작성은 /mypage/review · /mypage/qna 로 들어온다)

  비로그인 → 로그인 화면 (돌아올 주소를 붙인다)
  ADMIN · DEV → 각자의 홈. 관리자 · 개발자는 고객 활동을 하지 않는다.

  상품상세에서 구매 버튼을 숨기는 것은 화면 처리일 뿐이고, 막는 것은 이 필터다.
*/
public class UserFilter implements Filter {

	public void init(FilterConfig config) throws ServletException {
	}

	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
			throws IOException, ServletException {
		HttpServletRequest req = (HttpServletRequest) request;
		HttpServletResponse res = (HttpServletResponse) response;

		String role = RoleUtil.role(req);
		if (role == null) {
			String back = URLEncoder.encode(WebUtil.currentPath(req), "UTF-8");
			res.sendRedirect(req.getContextPath() + "/member/login?returnUrl=" + back);
			return;
		}
		if (!"USER".equals(role)) {
			req.getSession().setAttribute("flashError", "구매 · 마이페이지는 일반 회원 계정으로만 이용할 수 있습니다.");
			res.sendRedirect(req.getContextPath() + RoleUtil.homeOf(role));
			return;
		}
		chain.doFilter(request, response);
	}

	public void destroy() {
	}
}
