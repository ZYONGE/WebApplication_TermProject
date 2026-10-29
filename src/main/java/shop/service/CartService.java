package shop.service;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import shop.dao.CartDAO;
import shop.dao.ProductDAO;
import shop.dto.CartItemDTO;
import shop.dto.ProductDTO;

/*
  장바구니.

  ★ 목록에는 노출 조건을 걸지 않는다 (CRUD.md § 2 주의).
    담아 둔 뒤 품절 · 숨김이 된 상품도 보여준다. 대신 isOrderable() 로
    주문 대상에서 빼고 화면에 "주문 불가" 로 표시한다.
    조용히 사라지면 사용자는 자기가 담은 것이 왜 없는지 알 수 없다.
*/
public class CartService {

	public static final int MAX_QUANTITY = 99;

	private final CartDAO cartDAO = new CartDAO();
	private final ProductDAO productDAO = new ProductDAO();

	public List<CartItemDTO> findItems(int memberId) throws SQLException {
		return cartDAO.findByMember(memberId);
	}

	/* 헤더 수량 배지 */
	public int countQuantity(int memberId) throws SQLException {
		return cartDAO.countQuantity(memberId);
	}

	/* 주문 가능한 줄만 — 주문서와 결제가 이것만 본다. */
	public List<CartItemDTO> findOrderableItems(int memberId) throws SQLException {
		List<CartItemDTO> orderable = new ArrayList<CartItemDTO>();
		for (CartItemDTO item : cartDAO.findByMember(memberId)) {
			if (item.isOrderable()) {
				orderable.add(item);
			}
		}
		return orderable;
	}

	public int sumOrderable(List<CartItemDTO> items) {
		int sum = 0;
		for (CartItemDTO item : items) {
			sum += item.getLineTotal();
		}
		return sum;
	}

	/*
	  담기.

	  담는 시점에 노출 조건을 검사한다 — 숨김 · 품절 상품을 상세 URL 로
	  열지 못하더라도, 담기 요청은 직접 보낼 수 있기 때문이다.
	*/
	public void add(int memberId, int productId, int quantity) throws SQLException {
		if (quantity < 1 || quantity > MAX_QUANTITY) {
			throw new ServiceException("수량은 1~" + MAX_QUANTITY + "개로 입력하세요.");
		}
		ProductDTO product = productDAO.findAnyById(productId);
		if (product == null) {
			throw new ServiceException("상품을 찾을 수 없습니다.");
		}
		if (!product.isOnSale()) {
			throw new ServiceException("현재 구매할 수 없는 상품입니다.");
		}
		if (product.getStock() < quantity) {
			throw new ServiceException("재고가 " + product.getStock() + "개 남았습니다.");
		}
		cartDAO.add(memberId, productId, quantity);
	}

	public void updateQuantity(int cartId, int memberId, int quantity) throws SQLException {
		if (quantity < 1 || quantity > MAX_QUANTITY) {
			throw new ServiceException("수량은 1~" + MAX_QUANTITY + "개로 입력하세요.");
		}
		if (cartDAO.updateQuantity(cartId, memberId, quantity) == 0) {
			throw new ServiceException("장바구니 항목을 찾을 수 없습니다.");
		}
	}

	public void delete(int cartId, int memberId) throws SQLException {
		cartDAO.delete(cartId, memberId);
	}
}
