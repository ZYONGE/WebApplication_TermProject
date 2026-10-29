package shop.controller;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import shop.dto.CartItemDTO;
import shop.service.CartService;
import shop.service.ConfigService;
import shop.service.ServiceException;

/* 6번 장바구니 (CRUD.md § 2) — LoginFilter 가 비로그인을 먼저 걸러 낸다. */
@WebServlet(name = "CartServlet", urlPatterns = { "/cart/list", "/cart/add", "/cart/update", "/cart/delete" })
public class CartServlet extends BaseServlet {

	private static final long serialVersionUID = 1L;

	private final CartService cartService = new CartService();
	private final ConfigService configService = new ConfigService();

	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		try {
			list(request, response);
		} catch (SQLException e) {
			throw wrap(e);
		}
	}

	/* 상태를 바꾸는 요청은 전부 POST 다. 새로고침으로 중복 실행되지 않게 처리 후 리다이렉트한다. */
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		String action = action(request);
		int memberId = loginMemberId(request);
		try {
			if ("add".equals(action)) {
				cartService.add(memberId, intParam(request, "productId", 0),
						intParam(request, "quantity", 1));
				flash(request, "장바구니에 담았습니다.");
				/* 담은 뒤 어디로 갈지는 보낸 쪽이 정한다 — 상품상세에 남을 수도, 장바구니로 갈 수도 있다. */
				String back = param(request, "back");
				redirect(request, response, back != null && back.startsWith("/") ? back : "/cart/list");
				return;
			}
			if ("update".equals(action)) {
				cartService.updateQuantity(intParam(request, "cartId", 0), memberId,
						intParam(request, "quantity", 1));
				flash(request, "수량을 변경했습니다.");
			} else if ("delete".equals(action)) {
				cartService.delete(intParam(request, "cartId", 0), memberId);
				flash(request, "장바구니에서 뺐습니다.");
			}
		} catch (ServiceException e) {
			flashError(request, e.getMessage());
		} catch (SQLException e) {
			throw wrap(e);
		}
		redirect(request, response, "/cart/list");
	}

	/*
	  장바구니 목록.

	  담아 둔 줄은 품절 · 숨김이어도 보여준다. 합계는 주문 가능한 줄만 더한다
	  (CRUD.md § 2 주의).
	*/
	private void list(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException, SQLException {
		int memberId = loginMemberId(request);
		List<CartItemDTO> items = cartService.findItems(memberId);

		int totalPrice = 0;
		int blocked = 0;
		for (CartItemDTO item : items) {
			if (item.isOrderable()) {
				totalPrice += item.getLineTotal();
			} else {
				blocked++;
			}
		}
		int deliveryFee = configService.calculateDeliveryFee(totalPrice);

		request.setAttribute("pageTitle", "장바구니");
		pageCss(request, "cart");
		request.setAttribute("items", items);
		request.setAttribute("totalPrice", Integer.valueOf(totalPrice));
		request.setAttribute("deliveryFee", Integer.valueOf(deliveryFee));
		request.setAttribute("payAmount", Integer.valueOf(totalPrice + deliveryFee));
		request.setAttribute("blockedCount", Integer.valueOf(blocked));
		request.setAttribute("freeShippingMin", Integer.valueOf(configService.freeShippingMin()));

		render(request, response, "/cart/list.jsp");
	}
}
