package shop.controller;

import java.io.IOException;
import java.sql.SQLException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import shop.service.AdminService;
import shop.service.ConfigService;

/* 22번 관리자 대시보드 — 뷰 3개를 한 화면에 모은다 (CRUD.md § 3) */
@WebServlet(name = "AdminDashboardServlet", urlPatterns = { "/admin/dashboard" })
public class AdminDashboardServlet extends BaseServlet {

	private static final long serialVersionUID = 1L;

	private final AdminService adminService = new AdminService();
	private final ConfigService configService = new ConfigService();

	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		try {
			request.setAttribute("pageTitle", "관리자 대시보드");
			pageCss(request, "admin");
			request.setAttribute("summary", adminService.findSummary());
			request.setAttribute("salesDaily", adminService.findSalesDaily(7));
			request.setAttribute("deliveryCounts", adminService.findDeliveryStatusCount());
			request.setAttribute("lowStock", adminService.findLowStock());
			request.setAttribute("lowStockThreshold",
					Integer.valueOf(configService.lowStockThreshold()));
			render(request, response, "/admin/dashboard.jsp");
		} catch (SQLException e) {
			throw wrap(e);
		}
	}
}
