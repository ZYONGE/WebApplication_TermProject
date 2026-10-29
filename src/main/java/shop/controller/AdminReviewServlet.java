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
  35번 관리자 리뷰 관리 (CRUD.md § 3)

  ★ 이 화면에는 수정이 없다. ADMIN 은 R · D 만 갖는다 (CRUD.md § 6-3).
    남의 글 내용을 고치는 경로를 아예 만들지 않는 것이 규칙의 핵심이다.
*/
@WebServlet(name = "AdminReviewServlet", urlPatterns = { "/admin/reviews" })
public class AdminReviewServlet extends BaseServlet {

	private static final long serialVersionUID = 1L;

	private final AdminService adminService = new AdminService();

	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		try {
			request.setAttribute("pageTitle", "리뷰 관리");
			pageCss(request, "admin");
			request.setAttribute("pageData", adminService.findReviews(param(request, "keyword"),
					intParam(request, "page", 1), Paging.DEFAULT_SIZE));
			request.setAttribute("pageUrl", "/admin/reviews?");
			render(request, response, "/admin/reviews.jsp");
		} catch (SQLException e) {
			throw wrap(e);
		}
	}

	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		try {
			adminService.deleteReview(loginMemberId(request), intParam(request, "reviewId", 0));
			flash(request, "리뷰를 삭제했습니다.");
		} catch (ServiceException e) {
			flashError(request, e.getMessage());
		} catch (SQLException e) {
			throw wrap(e);
		}
		redirect(request, response, "/admin/reviews");
	}
}
