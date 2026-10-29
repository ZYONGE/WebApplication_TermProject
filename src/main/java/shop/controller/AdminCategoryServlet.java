package shop.controller;

import java.io.IOException;
import java.sql.SQLException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import shop.dto.CategoryDTO;
import shop.service.AdminService;
import shop.service.ServiceException;

/* 24번 카테고리 관리 (CRUD.md § 3) */
@WebServlet(name = "AdminCategoryServlet", urlPatterns = { "/admin/categories" })
public class AdminCategoryServlet extends BaseServlet {

	private static final long serialVersionUID = 1L;

	private final AdminService adminService = new AdminService();

	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		try {
			request.setAttribute("pageTitle", "카테고리 관리");
			pageCss(request, "admin");
			request.setAttribute("categories", adminService.findCategories());
			request.setAttribute("tree", adminService.findCategoryTree());
			render(request, response, "/admin/categories.jsp");
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
				adminService.deleteCategory(adminId, intParam(request, "categoryId", 0));
				flash(request, "카테고리를 삭제했습니다.");
			} else {
				CategoryDTO category = new CategoryDTO();
				category.setCategoryId(intParam(request, "categoryId", 0));
				int parentId = intParam(request, "parentId", 0);
				category.setParentId(parentId > 0 ? Integer.valueOf(parentId) : null);
				category.setName(param(request, "name"));
				category.setSortOrder(intParam(request, "sortOrder", 0));

				if (category.getCategoryId() > 0) {
					adminService.updateCategory(adminId, category);
					flash(request, "카테고리를 수정했습니다.");
				} else {
					adminService.createCategory(adminId, category);
					flash(request, "카테고리를 등록했습니다.");
				}
			}
		} catch (ServiceException e) {
			flashError(request, e.getMessage());
		} catch (SQLException e) {
			throw wrap(e);
		}
		redirect(request, response, "/admin/categories");
	}
}
