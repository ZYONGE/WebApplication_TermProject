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

/* 28번 공지 관리 (CRUD.md § 3) */
@WebServlet(name = "AdminNoticeServlet", urlPatterns = { "/admin/notices" })
public class AdminNoticeServlet extends BaseServlet {

	private static final long serialVersionUID = 1L;

	private final AdminService adminService = new AdminService();

	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		try {
			request.setAttribute("pageTitle", "공지 관리");
			pageCss(request, "admin");
			request.setAttribute("pageData", adminService.findNotices(param(request, "keyword"),
					intParam(request, "page", 1), Paging.DEFAULT_SIZE));
			request.setAttribute("pageUrl", "/admin/notices?");

			int editId = intParam(request, "editId", 0);
			if (editId > 0) {
				request.setAttribute("editNotice", adminService.findNotice(editId));
			}
			render(request, response, "/admin/notices.jsp");
		} catch (SQLException e) {
			throw wrap(e);
		}
	}

	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		int adminId = loginMemberId(request);
		String mode = param(request, "mode", "save");
		try {
			if ("delete".equals(mode)) {
				adminService.deleteNotice(adminId, intParam(request, "noticeId", 0));
				flash(request, "공지를 삭제했습니다.");
			} else {
				int noticeId = intParam(request, "noticeId", 0);
				if (noticeId > 0) {
					adminService.updateNotice(adminId, noticeId, param(request, "title"),
							param(request, "content"));
					flash(request, "공지를 수정했습니다.");
				} else {
					adminService.createNotice(adminId, param(request, "title"),
							param(request, "content"));
					flash(request, "공지를 등록했습니다.");
				}
			}
		} catch (ServiceException e) {
			flashError(request, e.getMessage());
		} catch (SQLException e) {
			throw wrap(e);
		}
		redirect(request, response, "/admin/notices");
	}
}
