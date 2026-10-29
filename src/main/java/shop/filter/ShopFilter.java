package shop.filter;

import java.io.IOException;
import java.sql.SQLException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import shop.dto.MemberDTO;
import shop.service.ConfigService;
import shop.util.WebUtil;

/*
  점검 모드 — SHOP_STATUS = MAINTENANCE 면 일반 사용자를 점검 안내로 보낸다.

  ★ AdminFilter 보다 반드시 먼저 와야 한다 (ERD § 5).
    반대 순서면 점검 모드에서 관리자까지 막혀 점검을 해제할 수 없게 된다.

  ★ 통과시키는 경로에 /member/login 을 포함했다.
    ERD § 5 의 목록은 /admin/*, /dev/*, 정적 자원뿐인데, 그대로 두면
    "점검 모드를 켠 뒤 로그아웃한 관리자" 가 영영 들어올 수 없다.
    관리자 경로만 열어 두는 것은 이미 로그인한 상태를 전제한 설계다.
    → 트랙 문서 § 8 에 ERD 트랙 확인 요청으로 올려 둔다.
*/
public class ShopFilter implements Filter {

	private final ConfigService configService = new ConfigService();

	public void init(FilterConfig config) throws ServletException {
	}

	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
			throws IOException, ServletException {
		HttpServletRequest req = (HttpServletRequest) request;
		HttpServletResponse res = (HttpServletResponse) response;

		String path = req.getRequestURI().substring(req.getContextPath().length());

		if (isAlwaysOpen(path) || hasStaffRole(req)) {
			chain.doFilter(request, response);
			return;
		}

		try {
			if (configService.isMaintenance()) {
				res.sendRedirect(req.getContextPath() + "/maintenance");
				return;
			}
		} catch (SQLException e) {
			/* 설정을 못 읽는 상황에서 사이트 전체를 막지는 않는다. 통과시키고 화면에서 드러나게 둔다. */
			req.setAttribute("configError", e.getMessage());
		}
		chain.doFilter(request, response);
	}

	private boolean isAlwaysOpen(String path) {
		return path.startsWith("/admin/")
			|| path.startsWith("/dev/")
			|| path.startsWith("/css/")
			|| path.startsWith("/js/")
			|| path.startsWith("/images/")
			|| path.startsWith("/upload/")
			|| path.equals("/maintenance")
			|| path.equals("/member/login")
			|| path.equals("/member/logout");
	}

	/* 이미 로그인한 운영 계정은 점검 중에도 사이트를 그대로 본다. */
	private boolean hasStaffRole(HttpServletRequest req) {
		MemberDTO member = WebUtil.loginMember(req);
		return member != null
			&& ("ADMIN".equals(member.getRole()) || "DEV".equals(member.getRole()));
	}

	public void destroy() {
	}
}
