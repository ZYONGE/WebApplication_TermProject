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

/* 27번 문의 답변 (CRUD.md § 3) — 미답변 우선으로 보여준다. */
@WebServlet(name = "AdminQnaServlet", urlPatterns = { "/admin/qna" })
public class AdminQnaServlet extends BaseServlet {

	private static final long serialVersionUID = 1L;

	private final AdminService adminService = new AdminService();

	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		try {
			/* 기본은 미답변만 — 답변 대기열이 이 화면의 목적이다. */
			boolean onlyUnanswered = !"all".equals(param(request, "filter"));
			request.setAttribute("pageTitle", "문의 답변");
			pageCss(request, "admin");
			request.setAttribute("onlyUnanswered", Boolean.valueOf(onlyUnanswered));
			request.setAttribute("pageData", adminService.findQna(param(request, "keyword"),
					onlyUnanswered, intParam(request, "page", 1), Paging.DEFAULT_SIZE));
			request.setAttribute("pageUrl",
					"/admin/qna?filter=" + (onlyUnanswered ? "wait" : "all") + "&");
			render(request, response, "/admin/qna.jsp");
		} catch (SQLException e) {
			throw wrap(e);
		}
	}

	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		try {
			adminService.answerQna(loginMemberId(request), intParam(request, "qnaId", 0),
					param(request, "answer"));
			flash(request, "답변을 등록했습니다.");
		} catch (ServiceException e) {
			flashError(request, e.getMessage());
		} catch (SQLException e) {
			throw wrap(e);
		}
		redirect(request, response, "/admin/qna");
	}
}
