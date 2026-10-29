package shop.controller;

import java.io.IOException;
import java.sql.SQLException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import shop.service.AdminService;
import shop.service.ServiceException;
import shop.util.Paging;

/*
  26번 회원 관리 (CRUD.md § 3)

  ★ 권한 변경에서 DEV 를 거부하는 것이 이 화면의 핵심 규칙이다 (ERD § 5).
    필터는 경로만 막는다. 권한 상승은 AdminService.changeRole 이 막는다.
*/
@WebServlet(name = "AdminMemberServlet", urlPatterns = { "/admin/members" })
public class AdminMemberServlet extends BaseServlet {

	private static final long serialVersionUID = 1L;

	private final AdminService adminService = new AdminService();

	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		try {
			request.setAttribute("pageTitle", "회원 관리");
			pageCss(request, "admin");
			request.setAttribute("pageData", adminService.findMembers(
					param(request, "keyword"), param(request, "role"),
					intParam(request, "page", 1), Paging.DEFAULT_SIZE));
			request.setAttribute("pageUrl", "/admin/members?");
			render(request, response, "/admin/members.jsp");
		} catch (SQLException e) {
			throw wrap(e);
		}
	}

	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		try {
			adminService.changeRole(loginMemberId(request), intParam(request, "memberId", 0),
					param(request, "role"));
			flash(request, "회원 권한을 변경했습니다.");
		} catch (ServiceException e) {
			flashError(request, e.getMessage());
		} catch (SQLException e) {
			throw wrap(e);
		}
		redirect(request, response, "/admin/members");
	}
}
