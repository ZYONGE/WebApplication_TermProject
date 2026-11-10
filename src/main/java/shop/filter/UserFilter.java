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
			String back = returnPath(req);
			res.sendRedirect(req.getContextPath() + "/member/login"
					+ (back == null ? "" : "?returnUrl=" + URLEncoder.encode(back, "UTF-8")));
			return;
		}
		if (!"USER".equals(role)) {
			req.getSession().setAttribute("flashError", "구매 · 마이페이지는 일반 회원 계정으로만 이용할 수 있습니다.");
			res.sendRedirect(req.getContextPath() + RoleUtil.homeOf(role));
			return;
		}
		chain.doFilter(request, response);
	}

	/*
	  로그인 후 돌아올 주소.

	  ★ GET 이면 지금 주소 그대로. POST(찜 · 담기 · 저장 같은 동작 주소)는 돌아갈 화면이 아니다 —
	    그 주소를 returnUrl 로 쓰면 로그인 뒤 브라우저가 GET 으로 열어 405 가 난다.
	    그래서 폼이 보낸 back(컨텍스트 기준 경로) → 같은 앱의 Referer 순으로 "원래 보던 화면" 을 쓴다.
	    둘 다 없으면 null — 로그인 뒤 메인으로 간다.
	*/
	private String returnPath(HttpServletRequest req) {
		if ("GET".equalsIgnoreCase(req.getMethod())) {
			return WebUtil.currentPath(req);
		}
		String ctx = req.getContextPath();
		String back = req.getParameter("back");
		if (back != null && back.startsWith("/") && !back.startsWith("//")) {
			return ctx + back;
		}
		String referer = req.getHeader("Referer");
		if (referer != null) {
			int at = referer.indexOf(ctx + "/", referer.indexOf("//") + 2);
			String host = req.getServerName();
			if (at > 0 && referer.contains("//" + host)) {
				return referer.substring(at);
			}
		}
		return null;
	}

	public void destroy() {
	}
}
