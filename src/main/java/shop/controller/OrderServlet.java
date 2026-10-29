package shop.controller;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import shop.dto.AddressDTO;
import shop.dto.CartItemDTO;
import shop.dto.MemberDTO;
import shop.dto.OrderDTO;
import shop.service.CartService;
import shop.service.ConfigService;
import shop.service.MypageService;
import shop.service.OrderService;
import shop.service.ServiceException;
import shop.util.Paging;

/*
  7~12번 주문서 · 결제 · 주문완료 · 주문내역 · 주문상세 · 주문취소 (CRUD.md § 2)

  ★ 8번 결제처리가 이 프로젝트의 중심이다. 한 요청에서 7개 테이블을 건드리고
    전부가 한 트랜잭션이어야 한다. 실제 처리는 OrderService.pay() 에 있고
    여기서는 입력을 모아 넘기고 결과만 분기한다.
*/
@WebServlet(name = "OrderServlet", urlPatterns = { "/order/form", "/order/pay", "/order/complete", "/order/list", "/order/view", "/order/cancel" })
public class OrderServlet extends BaseServlet {

	private static final long serialVersionUID = 1L;

	private final OrderService orderService = new OrderService();
	private final CartService cartService = new CartService();
	private final MypageService mypageService = new MypageService();
	private final ConfigService configService = new ConfigService();

	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		String action = action(request);
		pageCss(request, "order");
		try {
			if ("form".equals(action)) {
				form(request, response);
			} else if ("complete".equals(action)) {
				complete(request, response);
			} else if ("view".equals(action)) {
				view(request, response);
			} else {
				list(request, response);
			}
		} catch (SQLException e) {
			throw wrap(e);
		}
	}

	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		String action = action(request);
		try {
			if ("pay".equals(action)) {
				pay(request, response);
			} else if ("cancel".equals(action)) {
				cancel(request, response);
			} else {
				redirect(request, response, "/order/list");
			}
		} catch (SQLException e) {
			throw wrap(e);
		}
	}

	/* 7번 주문서 — 기본 배송지를 자동으로 채워 둔다 (CRUD.md § 5). */
	private void form(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException, SQLException {
		int memberId = loginMemberId(request);
		List<CartItemDTO> items = cartService.findOrderableItems(memberId);
		if (items.isEmpty()) {
			flashError(request, "주문할 수 있는 상품이 없습니다.");
			redirect(request, response, "/cart/list");
			return;
		}

		int totalPrice = cartService.sumOrderable(items);
		int deliveryFee = configService.calculateDeliveryFee(totalPrice);

		MemberDTO member = loginMember(request);
		AddressDTO defaultAddr = mypageService.findDefaultAddress(memberId);

		request.setAttribute("pageTitle", "주문서");
		request.setAttribute("items", items);
		request.setAttribute("totalPrice", Integer.valueOf(totalPrice));
		request.setAttribute("deliveryFee", Integer.valueOf(deliveryFee));
		request.setAttribute("payAmount", Integer.valueOf(totalPrice + deliveryFee));
		request.setAttribute("member", member);
		request.setAttribute("defaultAddr", defaultAddr);
		request.setAttribute("addresses", mypageService.findAddresses(memberId));

		render(request, response, "/order/form.jsp");
	}

	/*
	  8번 결제처리.

	  ★ 금액을 폼에서 받지 않는다. 서비스가 장바구니를 트랜잭션 안에서 다시 읽어
	    서버에서 계산한다. 폼 값을 믿으면 가격을 조작할 수 있다.
	*/
	private void pay(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException, SQLException {
		int memberId = loginMemberId(request);

		OrderDTO form = new OrderDTO();
		form.setReceiver(param(request, "receiver"));
		form.setReceiverPhone(param(request, "receiverPhone"));
		form.setZipcode(param(request, "zipcode"));
		form.setAddr1(param(request, "addr1"));
		form.setAddr2(param(request, "addr2"));
		form.setDeliveryMemo(param(request, "deliveryMemo"));

		try {
			int orderId = orderService.pay(memberId, form, param(request, "method"));
			redirect(request, response, "/order/complete?orderId=" + orderId);
		} catch (ServiceException e) {
			/* 재고 부족 · 빈 장바구니 등 — 주문서로 돌려보내 다시 시도하게 한다. */
			flashError(request, e.getMessage());
			redirect(request, response, "/order/form");
		}
	}

	/* 9번 주문완료 */
	private void complete(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException, SQLException {
		int orderId = intParam(request, "orderId", 0);
		OrderDTO order = orderService.findDetail(orderId, loginMemberId(request), false);
		if (order == null) {
			flashError(request, "주문을 찾을 수 없습니다.");
			redirect(request, response, "/order/list");
			return;
		}
		request.setAttribute("pageTitle", "주문 완료");
		request.setAttribute("order", order);
		render(request, response, "/order/complete.jsp");
	}

	/* 10번 주문내역 */
	private void list(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException, SQLException {
		request.setAttribute("pageTitle", "주문 내역");
		request.setAttribute("pageData", orderService.findMyOrders(loginMemberId(request),
				intParam(request, "page", 1), Paging.DEFAULT_SIZE));
		request.setAttribute("pageUrl", "/order/list?");
		render(request, response, "/order/list.jsp");
	}

	/* 11번 주문상세 · 배송조회 */
	private void view(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException, SQLException {
		int orderId = intParam(request, "orderId", 0);
		OrderDTO order = orderService.findDetail(orderId, loginMemberId(request), false);
		if (order == null) {
			flashError(request, "주문을 찾을 수 없습니다.");
			redirect(request, response, "/order/list");
			return;
		}
		request.setAttribute("pageTitle", "주문 상세");
		request.setAttribute("order", order);
		render(request, response, "/order/view.jsp");
	}

	/* 12번 주문취소 */
	private void cancel(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException, SQLException {
		int orderId = intParam(request, "orderId", 0);
		try {
			orderService.cancel(orderId, loginMemberId(request), false);
			flash(request, "주문을 취소했습니다. 재고가 복구되었습니다.");
		} catch (ServiceException e) {
			flashError(request, e.getMessage());
		}
		redirect(request, response, "/order/view?orderId=" + orderId);
	}
}
