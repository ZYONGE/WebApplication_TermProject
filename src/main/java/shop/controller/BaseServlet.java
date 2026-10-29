package shop.controller;

import java.io.IOException;
import java.sql.SQLException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import shop.dao.CartDAO;
import shop.dao.CategoryDAO;
import shop.dto.MemberDTO;
import shop.service.ConfigService;
import shop.util.WebUtil;

/*
  모든 서블릿의 공통 바탕.

  하는 일은 네 가지다.
    1. 경로 조각(action) 을 뽑아 준다 — /cart/add 의 'add'
    2. 뷰로 넘기기 전에 헤더가 쓰는 공통 모델을 채운다
       (CRUD.md § 2 — "헤더가 매 요청마다 cfg · cat · crt 를 R")
    3. 한 번만 보여주고 사라지는 메시지(flash)를 세션에 싣고 내린다
    4. ServiceException(업무 규칙 거절) 과 SQLException(고장) 을 갈라 처리한다
*/
public abstract class BaseServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;

	protected static final String FLASH = "flashMessage";
	protected static final String FLASH_ERROR = "flashError";

	private final CategoryDAO categoryDAO = new CategoryDAO();
	private final CartDAO cartDAO = new CartDAO();
	private final ConfigService configService = new ConfigService();

	/*
	  /cart/add -> "add", /mypage/ -> "".

	  서블릿을 /cart/* 같은 와일드카드로 매핑하지 않는다. 뷰가 /cart/list.jsp 처럼
	  같은 디렉터리에 있어서, forward 한 JSP 를 같은 서블릿이 다시 받아 무한 순환한다.
	  그래서 URL 을 하나씩 정확히 나열하고, 동작은 servletPath 의 마지막 조각으로 가른다.
	*/
	protected String action(HttpServletRequest request) {
		String path = request.getServletPath();
		if (path == null) {
			return "";
		}
		return path.substring(path.lastIndexOf('/') + 1);
	}

	/*
	  뷰로 넘긴다.

	  view 는 웹 루트 기준 경로다 (예: "/product/list.jsp").
	  forward 는 FORWARD 디스패치라 JspDirectAccessFilter 를 타지 않는다.
	*/
	protected void render(HttpServletRequest request, HttpServletResponse response, String view)
			throws ServletException, IOException {
		fillHeaderModel(request);
		request.getRequestDispatcher(view).forward(request, response);
	}

	/* 페이지 전용 CSS 이름 — header.jsp 가 css/pages/<name>.css 를 끼워 넣는다. */
	protected void pageCss(HttpServletRequest request, String name) {
		request.setAttribute("pageCss", name);
	}

	protected void redirect(HttpServletRequest request, HttpServletResponse response, String path)
			throws IOException {
		response.sendRedirect(request.getContextPath() + path);
	}

	/* 한 번만 보여줄 메시지 — 리다이렉트 뒤에도 살아남아야 해서 세션에 싣는다. */
	protected void flash(HttpServletRequest request, String message) {
		request.getSession().setAttribute(FLASH, message);
	}

	protected void flashError(HttpServletRequest request, String message) {
		request.getSession().setAttribute(FLASH_ERROR, message);
	}

	protected MemberDTO loginMember(HttpServletRequest request) {
		return WebUtil.loginMember(request);
	}

	protected int loginMemberId(HttpServletRequest request) {
		return WebUtil.loginMemberId(request);
	}

	protected boolean isAdmin(HttpServletRequest request) {
		MemberDTO member = loginMember(request);
		return member != null
			&& ("ADMIN".equals(member.getRole()) || "DEV".equals(member.getRole()));
	}

	protected int intParam(HttpServletRequest request, String name, int defaultValue) {
		return WebUtil.intParam(request, name, defaultValue);
	}

	protected String param(HttpServletRequest request, String name) {
		return WebUtil.param(request, name);
	}

	protected String param(HttpServletRequest request, String name, String defaultValue) {
		return WebUtil.param(request, name, defaultValue);
	}

	/*
	  헤더 공통 모델.

	  장바구니 배지는 로그인했을 때만 조회한다 — 비로그인 요청까지 DB 를 때릴 이유가 없다.
	*/
	private void fillHeaderModel(HttpServletRequest request) {
		try {
			request.setAttribute("navCategories", categoryDAO.findTree());
			request.setAttribute("maintenance", Boolean.valueOf(configService.isMaintenance()));

			int memberId = loginMemberId(request);
			if (memberId > 0) {
				request.setAttribute("cartCount", Integer.valueOf(cartDAO.countQuantity(memberId)));
			}
		} catch (SQLException e) {
			/* 헤더 때문에 본문을 못 보여주는 것은 과하다. 비워 두고 넘어간다. */
			request.setAttribute("headerError", e.getMessage());
		}
	}

	/* 서블릿이 잡지 못한 SQLException 은 500 으로 올린다 — 고장은 감추지 않는다. */
	protected ServletException wrap(SQLException e) {
		return new ServletException("DB 처리 중 오류가 발생했습니다.", e);
	}

	/* 로그인 세션 교체 — 로그인 · 계정 전환이 함께 쓴다. */
	protected void setLoginMember(HttpServletRequest request, MemberDTO member) {
		HttpSession session = request.getSession();
		session.setAttribute(WebUtil.SESSION_MEMBER, member);
	}
}
