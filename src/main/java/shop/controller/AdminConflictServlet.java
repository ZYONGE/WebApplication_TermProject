package shop.controller;

import java.io.IOException;
import java.sql.SQLException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import shop.service.AdminService;
import shop.service.ServiceException;

/*
  39번 관리자 조합 관리 — 동시 섭취 금지 조합 (CRUD.md · ERD § 7.1 · IMPACT K7)

  등록 · 문구 수정 · 삭제. 모두 admin_log(CONFLICT_*) 와 같은 트랜잭션이다.
  /admin/* 라 AdminFilter 를 탄다.
*/
@WebServlet(name = "AdminConflictServlet", urlPatterns = { "/admin/conflicts" })
public class AdminConflictServlet extends BaseServlet {

	private static final long serialVersionUID = 1L;

	private final AdminService adminService = new AdminService();

	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		try {
			request.setAttribute("pageTitle", "금지 조합 관리");
			pageCss(request, "admin");
			request.setAttribute("conflicts", adminService.findConflicts());
			request.setAttribute("singles", adminService.findAllSingles());
			int editId = intParam(request, "editId", 0);
			if (editId > 0) {
				request.setAttribute("editConflict", adminService.findConflict(editId));
			}
			render(request, response, "/admin/conflicts.jsp");
		} catch (SQLException e) {
			throw wrap(e);
		}
	}

	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		int adminId = loginMemberId(request);
		String mode = param(request, "mode", "");
		try {
			if ("create".equals(mode)) {
				adminService.createConflict(adminId, intParam(request, "productA", 0),
						intParam(request, "productB", 0), param(request, "message"));
				flash(request, "금지 조합을 등록했습니다.");
			} else if ("update".equals(mode)) {
				adminService.updateConflict(adminId, intParam(request, "conflictId", 0),
						param(request, "message"));
				flash(request, "경고 문구를 수정했습니다.");
			} else if ("delete".equals(mode)) {
				adminService.deleteConflict(adminId, intParam(request, "conflictId", 0));
				flash(request, "금지 조합을 삭제했습니다.");
			}
		} catch (ServiceException e) {
			flashError(request, e.getMessage());
		} catch (SQLException e) {
			throw wrap(e);
		}
		redirect(request, response, "/admin/conflicts");
	}
}
