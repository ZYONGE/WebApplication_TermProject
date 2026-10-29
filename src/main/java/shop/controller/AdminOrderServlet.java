package shop.controller;

import java.io.IOException;
import java.sql.SQLException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import shop.dao.DeliveryDAO;
import shop.dto.OrderDTO;
import shop.service.AdminService;
import shop.service.OrderService;
import shop.service.ServiceException;
import shop.util.Paging;

/*
  25번 주문 · 배송 관리 (CRUD.md § 3)

  관리자가 배송 생애주기를 한 단계씩 밀어 올리는 화면이다.
  전이 규칙과 가드는 AdminService.moveDeliveryNext 에 있다 (ERD § 6.2).
*/
@WebServlet(name = "AdminOrderServlet", urlPatterns = { "/admin/orders" })
public class AdminOrderServlet extends BaseServlet {

	private static final long serialVersionUID = 1L;

	private final AdminService adminService = new AdminService();
	private final OrderService orderService = new OrderService();

	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		try {
			request.setAttribute("pageTitle", "주문 · 배송 관리");
			pageCss(request, "admin");
			request.setAttribute("pageData", adminService.findOrders(
					param(request, "keyword"), param(request, "deliveryStatus"),
					param(request, "orderStatus"),
					intParam(request, "page", 1), Paging.DEFAULT_SIZE));
			request.setAttribute("flow", DeliveryDAO.FLOW);
			request.setAttribute("pageUrl", "/admin/orders?");

			int detailId = intParam(request, "orderId", 0);
			if (detailId > 0) {
				OrderDTO detail = orderService.findDetail(detailId, 0, true);
				request.setAttribute("detail", detail);
			}
			render(request, response, "/admin/orders.jsp");
		} catch (SQLException e) {
			throw wrap(e);
		}
	}

	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		int adminId = loginMemberId(request);
		String mode = param(request, "mode", "next");
		try {
			if ("cancel".equals(mode)) {
				orderService.cancel(intParam(request, "orderId", 0), adminId, true);
				flash(request, "주문을 취소했습니다. 재고가 복구되었습니다.");
			} else {
				adminService.moveDeliveryNext(adminId, intParam(request, "deliveryId", 0),
						param(request, "courier"), param(request, "trackingNo"));
				flash(request, "배송 상태를 다음 단계로 넘겼습니다.");
			}
		} catch (ServiceException e) {
			flashError(request, e.getMessage());
		} catch (SQLException e) {
			throw wrap(e);
		}
		redirect(request, response, "/admin/orders");
	}
}
