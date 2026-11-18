package shop.service;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import shop.dao.CartDAO;
import shop.dao.ConfigDAO;
import shop.dao.ConflictDAO;
import shop.dao.DB;
import shop.dao.ProductDAO;
import shop.model.Conflict;
import shop.model.Item;
import shop.model.Product;

/* 장바구니 · 찜 (수량 = 섭취 일수. 1포가 하루치라 N일분은 N포) */
@WebServlet({ "/cart/list", "/cart/add", "/cart/addAll", "/cart/update", "/cart/delete", "/wish/toggle", "/wish/remove" })

public class CartServlet extends BaseServlet {

	private static final long serialVersionUID = 1L;

	@Override
	protected void get(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException, SQLException {
		if (request.getServletPath().startsWith("/wish/")) {
			redirect(request, response, "/mypage/wishlist");
			return;
		}
		List<Item> items = CartDAO.cart(loginId(request), true);
		int total = sumOrderable(items);
		int blocked = 0;
		Set<Integer> days = new LinkedHashSet<Integer>();
		for (Item item : items) {
			if (item.isOrderable()) {
				days.add(item.getQuantity());
			} else {
				blocked++;
			}
		}
		request.setAttribute("items", items);
		request.setAttribute("totalPrice", total);
		request.setAttribute("deliveryFee", ConfigDAO.deliveryFee(total));
		request.setAttribute("payAmount", total + ConfigDAO.deliveryFee(total));
		request.setAttribute("blockedCount", blocked);
		request.setAttribute("freeShippingMin", ConfigDAO.freeShippingMin());
		request.setAttribute("maxDays", ConfigDAO.maxDays());
		request.setAttribute("warnings", warnings(items));
		request.setAttribute("mixedDays", days.size() > 1);
		render(request, response, "cart/list", "장바구니");
	}

	/* 주문할 수 있는 줄의 합계 (주문서도 쓴다) */
	static int sumOrderable(List<Item> items) {
		int sum = 0;
		for (Item item : items) {
			if (item.isOrderable()) {
				sum += item.getLineTotal();
			}
		}
		return sum;
	}

	/* 주문할 수 있는 줄끼리의 동시 섭취 경고 */
	static List<Conflict> warnings(List<Item> items) throws SQLException {
		List<Integer> ids = new ArrayList<Integer>();
		for (Item item : items) {
			if (item.isOrderable()) {
				ids.add(item.getProductId());
			}
		}
		return ConflictDAO.warnings(ids);
	}

	@Override
	protected void post(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException, SQLException {
		int memberId = loginId(request);
		int productId = intParam(request, "productId", 0);
		int days = intParam(request, "quantity", 0);
		try {
			switch (request.getServletPath()) {
			case "/cart/add":
				checkDays(days);
				Product p = ProductDAO.byId(productId, false);
				if (p == null) {
					throw new Fail("상품을 찾을 수 없습니다.");
				}
				if (!p.isOnSale()) {
					throw new Fail("현재 구매할 수 없는 상품입니다.");
				}
				if (p.getStock() < days) {
					throw new Fail("재고가 " + p.getStock() + "포 남았습니다. " + p.getStock() + "일분까지 담을 수 있습니다.");
				}
				CartDAO.add(memberId, productId, days);
				flash(request, days + "일분을 장바구니에 담았습니다.");
				back(request, response, "/cart/list");
				return;
			case "/cart/addAll":
				addAll(request, memberId);
				break;
			case "/cart/update":
				checkDays(days);
				if (CartDAO.updateDays(intParam(request, "cartId", 0), memberId, days) == 0) {
					throw new Fail("장바구니 항목을 찾을 수 없습니다.");
				}
				flash(request, "섭취 일수를 변경했습니다.");
				break;
			case "/cart/delete":
				CartDAO.delete(intParam(request, "cartId", 0), memberId);
				flash(request, "장바구니에서 뺐습니다.");
				break;
			case "/wish/toggle":
				boolean wished = CartDAO.isWished(memberId, productId);
				if (wished) {
					CartDAO.unwish(memberId, productId);
				} else {
					CartDAO.wish(memberId, productId);
				}
				flash(request, wished ? "찜을 해제했습니다." : "찜했습니다.");
				back(request, response, "/mypage/wishlist");
				return;
			case "/wish/remove":
				CartDAO.unwish(memberId, productId);
				flash(request, "찜을 해제했습니다.");
				back(request, response, "/mypage/wishlist");
				return;
			}
		} catch (Fail e) {
			flashError(request, e.getMessage());
		}
		redirect(request, response, "/cart/list");
	}

	/* 조합 맞추기에서 고른 낱개를 같은 일수로 한꺼번에 담기 — 담을 수 없는 상품은 빼고 알려 준다 */
	private void addAll(HttpServletRequest request, int memberId) throws SQLException {
		int days = intParam(request, "days", 0);
		checkDays(days);
		Set<Integer> ids = new LinkedHashSet<Integer>(intParams(request, "productId"));
		if (ids.isEmpty()) {
			throw new Fail("담을 영양제를 하나 이상 고르세요.");
		}
		Map<Integer, Product> products = ProductDAO.byIds(ids);
		List<Product> targets = new ArrayList<Product>();
		List<String> skipped = new ArrayList<String>();
		for (Integer id : ids) {
			Product p = products.get(id);
			if (p == null || p.isTrack()) {
				continue;
			}
			if (!p.isOnSale()) {
				skipped.add(p.getName() + "(판매 중지)");
			} else if (p.getStock() < days) {
				skipped.add(p.getName() + "(재고 " + p.getStock() + "포 — " + p.getStock() + "일분까지)");
			} else {
				targets.add(p);
			}
		}
		if (targets.isEmpty()) {
			throw new Fail("지금 담을 수 있는 영양제가 없습니다." + (skipped.isEmpty() ? "" : " " + String.join(", ", skipped)));
		}
		DB.begin();
		try {
			for (Product p : targets) {
				CartDAO.add(memberId, p.getProductId(), days);
			}
			DB.commit();
		} finally {
			DB.end();
		}
		flash(request, "조합 " + targets.size() + "종을 " + days + "일분씩 담았습니다."
				+ (skipped.isEmpty() ? "" : " 담지 못한 상품: " + String.join(", ", skipped)));
	}
}
