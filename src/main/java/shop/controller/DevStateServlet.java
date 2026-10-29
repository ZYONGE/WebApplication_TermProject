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
import shop.service.ConfigService;
import shop.service.DevService;
import shop.service.OrderService;
import shop.service.ServiceException;
import shop.util.Paging;

/*
  30번 dev 상태 컨트롤러 (CRUD.md § 3)

  정상 규칙을 넘어서는 조작을 모아 둔 화면이다. 강제 변경의 결과(is_forced='Y')를
  같은 화면에서 바로 확인할 수 있어야 하므로, 배송 이력을 함께 보여준다
  (CRUD.md § 6-1 — is_forced 를 읽는 유일한 화면).
*/
@WebServlet(name = "DevStateServlet", urlPatterns = { "/dev/state" })
public class DevStateServlet extends BaseServlet {

	private static final long serialVersionUID = 1L;

	private final DevService devService = new DevService();
	private final AdminService adminService = new AdminService();
	private final OrderService orderService = new OrderService();
	private final ConfigService configService = new ConfigService();

	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		try {
			request.setAttribute("pageTitle", "dev · 상태 컨트롤러");
			pageCss(request, "admin");
			request.setAttribute("pageData", adminService.findOrders(param(request, "keyword"),
					null, null, intParam(request, "page", 1), Paging.DEFAULT_SIZE));
			request.setAttribute("pageUrl", "/dev/state?");
			request.setAttribute("flow", DeliveryDAO.FLOW);
			request.setAttribute("configs", configService.findAll());
			request.setAttribute("products",
					adminService.findProducts(null, null, null, 1, 100).getList());

			int orderId = intParam(request, "orderId", 0);
			if (orderId > 0) {
				OrderDTO detail = orderService.findDetail(orderId, 0, true);
				request.setAttribute("detail", detail);
			}
			render(request, response, "/dev/state.jsp");
		} catch (SQLException e) {
			throw wrap(e);
		}
	}

	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		int devId = loginMemberId(request);
		String mode = param(request, "mode", "");
		try {
			if ("delivery".equals(mode)) {
				devService.forceDeliveryStatus(devId, intParam(request, "deliveryId", 0),
						param(request, "status"), param(request, "courier"),
						param(request, "trackingNo"));
				flash(request, "배송 상태를 강제로 변경했습니다. (is_forced = 'Y')");
			} else if ("order".equals(mode)) {
				devService.forceOrderStatus(devId, intParam(request, "orderId", 0),
						param(request, "status"), "Y".equals(param(request, "restoreStock")));
				flash(request, "주문 상태를 강제로 변경했습니다.");
			} else if ("pay".equals(mode)) {
				devService.forcePayStatus(devId, intParam(request, "orderId", 0),
						param(request, "status"));
				flash(request, "결제 상태를 강제로 변경했습니다.");
			} else if ("stock".equals(mode)) {
				devService.forceStock(devId, intParam(request, "productId", 0),
						intParam(request, "stock", 0));
				flash(request, "재고를 변경했습니다.");
			} else if ("shop".equals(mode)) {
				String next = devService.toggleShopStatus(devId);
				flash(request, "점검 모드: " + next);
			} else if ("config".equals(mode)) {
				devService.updateConfig(devId, param(request, "configKey"),
						param(request, "configValue"));
				flash(request, "설정을 변경했습니다.");
			}
		} catch (ServiceException e) {
			flashError(request, e.getMessage());
		} catch (SQLException e) {
			throw wrap(e);
		}
		redirect(request, response, "/dev/state");
	}
}
