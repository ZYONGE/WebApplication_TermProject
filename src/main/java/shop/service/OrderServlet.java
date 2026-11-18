package shop.service;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import shop.dao.AdminDAO;
import shop.dao.CartDAO;
import shop.dao.ConfigDAO;
import shop.dao.DB;
import shop.dao.MemberDAO;
import shop.dao.OrderDAO;
import shop.dao.ProductDAO;
import shop.model.Item;
import shop.model.Order;
import shop.model.Product;

/* 주문서 · 결제 · 주문 완료 · 주문 내역 · 주문 상세 · 주문 취소 */
@WebServlet({ "/order/form", "/order/pay", "/order/complete", "/order/list", "/order/view", "/order/cancel" })
public class OrderServlet extends BaseServlet {

	private static final long serialVersionUID = 1L;

	@Override
	protected void get(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException, SQLException {
		int memberId = loginId(request);
		int orderId = intParam(request, "orderId", 0);
		switch (request.getServletPath()) {
		case "/order/form":
			List<Item> items = new ArrayList<Item>();
			for (Item item : CartDAO.cart(memberId, true)) {
				if (item.isOrderable()) {
					items.add(item);
				}
			}
			if (items.isEmpty()) {
				flashError(request, "주문할 수 있는 상품이 없습니다.");
				redirect(request, response, "/cart/list");
				return;
			}
			int total = CartServlet.sumOrderable(items);
			request.setAttribute("items", items);
			request.setAttribute("totalPrice", total);
			request.setAttribute("deliveryFee", ConfigDAO.deliveryFee(total));
			request.setAttribute("payAmount", total + ConfigDAO.deliveryFee(total));
			request.setAttribute("member", loginMember(request));
			request.setAttribute("defaultAddr", MemberDAO.defaultAddress(memberId));
			request.setAttribute("addresses", MemberDAO.addresses(memberId));
			request.setAttribute("warnings", CartServlet.warnings(items));
			render(request, response, "order/form", "주문서");
			return;
		case "/order/list":
			request.setAttribute("pageData", OrderDAO.myOrders(memberId, intParam(request, "page", 1), 10));
			request.setAttribute("pageUrl", "/order/list?");
			render(request, response, "order/list", "주문 내역");
			return;
		case "/order/complete":
			/* 방금 결제한 주문일 때만 "완료" 화면 — 아니면 주문 상세로 */
			Object justPaid = request.getSession().getAttribute("justPaidOrderId");
			if (justPaid == null || (Integer) justPaid != orderId) {
				redirect(request, response, "/order/view?orderId=" + orderId);
				return;
			}
			request.getSession().removeAttribute("justPaidOrderId");
		}
		Order order = OrderDAO.detail(orderId, memberId);
		if (order == null) {
			flashError(request, "주문을 찾을 수 없습니다.");
			redirect(request, response, "/order/list");
			return;
		}
		request.setAttribute("order", order);
		if (request.getServletPath().equals("/order/complete")) {
			render(request, response, "order/complete", "주문 완료");
		} else {
			render(request, response, "order/view", "주문 상세");
		}
	}

	@Override
	protected void post(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException, SQLException {
		int orderId = intParam(request, "orderId", 0);
		if (request.getServletPath().equals("/order/cancel")) {
			try {
				cancel(orderId, loginId(request), false);
				flash(request, "주문을 취소했습니다. 재고가 복구되었습니다.");
			} catch (Fail e) {
				flashError(request, e.getMessage());
			}
			redirect(request, response, "/order/view?orderId=" + orderId);
		} else if (request.getServletPath().equals("/order/pay")) {
			try {
				orderId = pay(request, loginId(request));
				request.getSession().setAttribute("justPaidOrderId", orderId);
				redirect(request, response, "/order/complete?orderId=" + orderId);
			} catch (Fail e) {
				flashError(request, e.getMessage());
				redirect(request, response, "/order/form");
			}
		} else {
			redirect(request, response, "/order/list");
		}
	}

	/*
	  결제 — 한 트랜잭션으로
	    주문 저장 → 주문 상품 저장 · 재고 빼기 → 결제 · 배송 시작 → 장바구니 비우기
	  중간에 하나라도 실패하면 전부 되돌린다.
	*/
	private int pay(HttpServletRequest request, int memberId) throws SQLException {
		String receiver = param(request, "receiver");
		String phone = param(request, "receiverPhone");
		String method = param(request, "method");
		if (isEmpty(receiver)) {
			throw new Fail("받는 사람을 입력하세요.");
		}
		if (isEmpty(phone)) {
			throw new Fail("연락처를 입력하세요.");
		}
		if (isEmpty(param(request, "zipcode")) || isEmpty(param(request, "addr1"))) {
			throw new Fail("배송 주소를 입력하세요.");
		}
		if (!"카드".equals(method) && !"무통장".equals(method) && !"간편결제".equals(method)) {
			throw new Fail("결제 수단을 선택하세요.");
		}
		DB.begin();
		try {
			List<Item> cart = CartDAO.cart(memberId, false);
			if (cart.isEmpty()) {
				throw new Fail("장바구니가 비어 있습니다.");
			}
			if (noneOrderable(cart)) {
				throw new Fail("주문할 수 있는 상품이 없습니다.");
			}
			int total = CartServlet.sumOrderable(cart);
			int fee = ConfigDAO.deliveryFee(total);
			int orderId = OrderDAO.insertOrder(memberId, total, fee, receiver, phone, param(request, "zipcode"),
					param(request, "addr1"), param(request, "addr2"), param(request, "deliveryMemo"));
			for (Item item : cart) {
				if (!item.isOrderable()) {
					continue;
				}
				OrderDAO.insertItem(orderId, item, item.isTrack() ? snapshot(item.getProductId()) : null);
				if (ProductDAO.decreaseStock(item.getProductId(), item.getQuantity()) == 0) {
					throw new Fail("'" + item.getProductName() + "' 의 재고가 부족합니다. 장바구니를 다시 확인하세요.");
				}
			}
			OrderDAO.insertPaymentAndDelivery(orderId, method, total + fee);
			CartDAO.clear(memberId);
			DB.commit();
			return orderId;
		} finally {
			DB.end();
		}
	}

	private static boolean noneOrderable(List<Item> cart) {
		for (Item item : cart) {
			if (item.isOrderable()) {
				return false;
			}
		}
		return true;
	}

	/* 트랙 구성 기록 — "종합비타민 + 오메가3" (주문 뒤 트랙 구성이 바뀌어도 남는다) */
	private static String snapshot(int trackId) throws SQLException {
		Product track = new Product();
		track.setTrackItems(ProductDAO.trackItems(trackId));
		String names = track.getItemNames();
		return names.isEmpty() ? "(구성 없음)" : names.length() <= 500 ? names : names.substring(0, 500);
	}

	/*
	  주문 취소 (회원 · 관리자 공용) — 발송 전(결제완료 · 상품준비)만.
	  주문 취소 + 결제 환불 + 재고 되돌리기를 한 트랜잭션으로.
	*/
	static void cancel(int orderId, int actorId, boolean admin) throws SQLException {
		Order order = OrderDAO.detail(orderId, 0);
		if (order == null) {
			throw new Fail("주문을 찾을 수 없습니다.");
		}
		if (!admin && order.getMemberId() != actorId) {
			throw new Fail("본인의 주문만 취소할 수 있습니다.");
		}
		if (!"정상".equals(order.getOrderStatus())) {
			throw new Fail("이미 취소된 주문입니다.");
		}
		if (order.getDeliveryStatus() == null) {
			throw new Fail("배송 정보가 없어 취소할 수 없습니다.");
		}
		if (!order.isCancelable()) {
			throw new Fail("이미 발송된 주문은 취소할 수 없습니다. (현재 " + order.getDeliveryStatus() + ")");
		}
		DB.begin();
		try {
			if (OrderDAO.setOrderStatus(orderId, true) == 0) {
				throw new Fail("이미 취소된 주문입니다.");
			}
			OrderDAO.setPayStatus(orderId, true);
			for (Item item : order.getItems()) {
				ProductDAO.increaseStock(item.getProductId(), item.getQuantity());
			}
			if (admin) {
				AdminDAO.adminLog(actorId, "ORDER_CANCEL", "orders", orderId,
						"주문 " + order.getOrderNo() + " 취소 · 재고 " + order.getItems().size() + "건 복구");
			}
			DB.commit();
		} finally {
			DB.end();
		}
	}
}
