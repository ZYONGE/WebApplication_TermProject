package shop.service;

import java.io.IOException;
import java.net.URLEncoder;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import shop.dao.AdminDAO;
import shop.dao.CartDAO;
import shop.dao.CategoryDAO;
import shop.dao.ConfigDAO;
import shop.dao.ProductDAO;
import shop.model.Category;
import shop.model.Member;

/*
  모든 서블릿의 부모 클래스.

  요청이 들어오면 먼저 입장 검사를 한다.
    1. 점검 모드면 일반 사용자는 점검 안내로 (관리자 · 개발자 화면과 로그인은 열어 둔다)
    2. /cart /order /mypage /wish  → 일반 회원(USER)만
       /admin                      → 관리자(ADMIN)만
       /dev                        → 개발자(DEV)만
  검사를 통과하면 하위 서블릿의 get() / post() 가 실행된다.
  업무 규칙에 어긋나면 Fail 을 던지고, 받은 쪽에서 빨간 알림(flashError)으로 보여준다.
*/
public abstract class BaseServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;

	/* 업무 규칙 위반 — 사용자에게 보여줄 메시지 */
	public static class Fail extends RuntimeException {
		private static final long serialVersionUID = 1L;

		public Fail(String message) {
			super(message);
		}
	}

	/* 하위 서블릿은 doGet · doPost 대신 이 두 메서드를 만든다 (SQLException 을 그대로 던져도 된다) */
	protected void get(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException, SQLException {
		response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
	}

	protected void post(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException, SQLException {
		response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
	}

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		run(request, response, true);
	}

	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		run(request, response, false);
	}

	private void run(HttpServletRequest request, HttpServletResponse response, boolean isGet)
			throws ServletException, IOException {
		AdminDAO.IMPERSONATOR.set((Integer) request.getSession().getAttribute("impersonatorId"));
		try {
			if (!allowed(request, response)) {
				return;
			}
			if (isGet) {
				get(request, response);
			} else {
				post(request, response);
			}
		} catch (SQLException e) {
			throw new ServletException("DB 처리 중 오류가 발생했습니다.", e);
		} finally {
			AdminDAO.IMPERSONATOR.remove();
		}
	}

	/* 입장 검사 — 막았으면 다른 곳으로 보내고 false */
	private boolean allowed(HttpServletRequest request, HttpServletResponse response)
			throws IOException, SQLException {
		String path = request.getServletPath();
		String role = loginMember(request) == null ? null : loginMember(request).getRole();
		boolean staff = "ADMIN".equals(role) || "DEV".equals(role);
		boolean open = path.startsWith("/admin/") || path.startsWith("/dev/") || path.equals("/maintenance")
				|| path.equals("/member/login") || path.equals("/member/logout");
		if (!open && !staff && ConfigDAO.isMaintenance()) {
			redirect(request, response, "/maintenance");
			return false;
		}
		String need = path.startsWith("/admin") ? "ADMIN"
				: path.startsWith("/dev") ? "DEV"
				: path.matches("/(cart|order|mypage|wish)(/.*)?") ? "USER" : null;
		if (need == null || need.equals(role)) {
			return true;
		}
		/* 계정 전환 중에는 원래 계정으로 돌아가는 주소만 연다 */
		if (path.equals("/dev/loginAs/return") && request.getSession().getAttribute("impersonatorId") != null) {
			return true;
		}
		if (role == null) {
			String back = backAfterLogin(request, "USER".equals(need));
			redirect(request, response, "/member/login" + (back == null ? "" : "?returnUrl=" + URLEncoder.encode(back, "UTF-8")));
		} else {
			flashError(request, "USER".equals(need) ? "구매 · 마이페이지는 일반 회원 계정으로만 이용할 수 있습니다."
					: "ADMIN".equals(need) ? "관리자 계정으로만 접근할 수 있습니다." : "개발자 계정으로만 접근할 수 있습니다.");
			redirect(request, response, homeOf(role));
		}
		return false;
	}

	/*
	  로그인 후 돌아올 주소 — 지금 주소.
	  단, 구매 경로의 POST(담기 · 찜 같은 동작)는 돌아갈 화면이 아니므로
	  폼이 보낸 back → 같은 사이트의 Referer 순으로 "원래 보던 화면" 을 쓴다.
	*/
	private String backAfterLogin(HttpServletRequest request, boolean shop) {
		String ctx = request.getContextPath();
		String query = request.getQueryString();
		if (!shop || "GET".equals(request.getMethod())) {
			return request.getRequestURI() + (query == null ? "" : "?" + query);
		}
		String back = request.getParameter("back");
		if (back != null && back.startsWith("/") && !back.startsWith("//")) {
			return ctx + back;
		}
		String referer = request.getHeader("Referer");
		if (referer != null && referer.contains("//" + request.getServerName())) {
			int at = referer.indexOf(ctx + "/", referer.indexOf("//") + 2);
			if (at > 0) {
				return referer.substring(at);
			}
		}
		return null;
	}

	/* 역할별 첫 화면 */
	public static String homeOf(String role) {
		return "ADMIN".equals(role) ? "/admin/dashboard" : "DEV".equals(role) ? "/dev/" : "/";
	}

	/* ---------- 화면 · 이동 ---------- */

	/* /WEB-INF/views/{view}.jsp 를 보여준다 (title 은 브라우저 제목). 머리글이 쓰는 값도 함께 넣는다 */
	protected void render(HttpServletRequest request, HttpServletResponse response, String view, String title)
			throws ServletException, IOException, SQLException {
		request.setAttribute("pageTitle", title);
		request.setAttribute("navCategories", navCategories());
		request.setAttribute("maintenance", ConfigDAO.isMaintenance());
		Member member = loginMember(request);
		if (member != null && "USER".equals(member.getRole())) {
			request.setAttribute("cartCount", CartDAO.countCart(member.getMemberId()));
		}
		request.getRequestDispatcher("/WEB-INF/views/" + view + ".jsp").forward(request, response);
	}

	/* 메뉴용 카테고리 — 판매 중인 상품이 있는 카테고리만 */
	private List<Category> navCategories() throws SQLException {
		Set<Integer> live = ProductDAO.onSaleCategoryIds();
		List<Category> roots = new ArrayList<Category>();
		for (Category root : CategoryDAO.tree()) {
			List<Category> kids = new ArrayList<Category>();
			for (Category sub : root.getChildren()) {
				if (live.contains(sub.getCategoryId())) {
					kids.add(sub);
				}
			}
			root.setChildren(kids);
			if (!kids.isEmpty() || live.contains(root.getCategoryId())) {
				roots.add(root);
			}
		}
		return roots;
	}

	protected void redirect(HttpServletRequest request, HttpServletResponse response, String path) throws IOException {
		response.sendRedirect(request.getContextPath() + path);
	}

	/* 폼이 보낸 back(같은 사이트 경로)이 있으면 그리로, 없으면 기본 경로로 */
	protected void back(HttpServletRequest request, HttpServletResponse response, String defaultPath)
			throws IOException {
		String back = param(request, "back");
		redirect(request, response, back != null && back.startsWith("/") ? back : defaultPath);
	}

	/* 다음 화면에 한 번만 보여줄 알림 */
	protected void flash(HttpServletRequest request, String message) {
		request.getSession().setAttribute("flashMessage", message);
	}

	protected void flashError(HttpServletRequest request, String message) {
		request.getSession().setAttribute("flashError", message);
	}

	/* ---------- 로그인 · 파라미터 ---------- */

	protected Member loginMember(HttpServletRequest request) {
		return (Member) request.getSession().getAttribute("loginMember");
	}

	protected int loginId(HttpServletRequest request) {
		Member member = loginMember(request);
		return member == null ? 0 : member.getMemberId();
	}

	/* 앞뒤 공백을 뗀 파라미터 (없으면 null) */
	protected String param(HttpServletRequest request, String name) {
		String value = request.getParameter(name);
		return value == null ? null : value.trim();
	}

	/* 비어 있으면 기본값 */
	protected String param(HttpServletRequest request, String name, String defaultValue) {
		String value = param(request, name);
		return value == null || value.isEmpty() ? defaultValue : value;
	}

	protected int intParam(HttpServletRequest request, String name, int defaultValue) {
		return toInt(request.getParameter(name), defaultValue);
	}

	/* 같은 이름으로 여러 개 온 숫자 (0 이하 · 숫자 아님은 버린다) */
	protected List<Integer> intParams(HttpServletRequest request, String name) {
		List<Integer> list = new ArrayList<Integer>();
		String[] values = request.getParameterValues(name);
		if (values != null) {
			for (String v : values) {
				if (toInt(v, 0) > 0) {
					list.add(toInt(v, 0));
				}
			}
		}
		return list;
	}

	public static int toInt(String value, int defaultValue) {
		try {
			return Integer.parseInt(value.trim());
		} catch (RuntimeException e) {
			return defaultValue;
		}
	}

	/* [1, 2, 3] → "1,2,3" */
	public static String joinIds(Collection<Integer> ids) {
		StringBuilder sb = new StringBuilder();
		for (Integer id : ids) {
			sb.append(sb.length() == 0 ? "" : ",").append(id);
		}
		return sb.toString();
	}

	public static boolean isEmpty(String s) {
		return s == null || s.trim().isEmpty();
	}

	/* 섭취 일수 검사 (1 ~ 상한) */
	protected void checkDays(int days) throws SQLException {
		int max = ConfigDAO.maxDays();
		if (days < 1 || days > max) {
			throw new Fail("섭취 일수는 1~" + max + "일로 입력하세요.");
		}
	}
}
