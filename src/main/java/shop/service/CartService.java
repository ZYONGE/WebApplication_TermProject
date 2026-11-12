package shop.service;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import shop.dao.CartDAO;
import shop.dao.ProductDAO;
import shop.dto.CartItemDTO;
import shop.dto.ProductDTO;
import shop.util.DBUtil;

/*
  장바구니.

  ★ 목록에는 노출 조건을 걸지 않는다 (CRUD.md § 2 주의).
    담아 둔 뒤 품절 · 숨김이 된 상품도 보여준다. 대신 isOrderable() 로
    주문 대상에서 빼고 화면에 "주문 불가" 로 표시한다.
    조용히 사라지면 사용자는 자기가 담은 것이 왜 없는지 알 수 없다.

  ★ quantity 는 섭취 일수(= 포 수)다. 범위는 1 ~ MAX_INTAKE_DAYS (ERD § 7.1).
    같은 상품을 다시 담으면 일수를 덮어쓴다.
*/
public class CartService {

	private final CartDAO cartDAO = new CartDAO();
	private final ProductDAO productDAO = new ProductDAO();
	private final ConfigService configService = new ConfigService();

	public List<CartItemDTO> findItems(int memberId) throws SQLException {
		return cartDAO.findByMember(memberId);
	}

	/* 헤더 배지 — 줄 수 */
	public int countLines(int memberId) throws SQLException {
		return cartDAO.countLines(memberId);
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
	  주문 대상 줄의 섭취 일수가 서로 다른가 — 장바구니가 "일수가 다른 줄" 을 안내한다.
	  막지는 않는다. 영양제마다 일부러 다르게 살 수도 있다.
	*/
	public boolean hasMixedDays(List<CartItemDTO> items) {
		int days = -1;
		for (CartItemDTO item : items) {
			if (!item.isOrderable()) {
				continue;
			}
			if (days >= 0 && days != item.getQuantity()) {
				return true;
			}
			days = item.getQuantity();
		}
		return false;
	}

	/*
	  담기 (상품상세).

	  담는 시점에 노출 조건을 검사한다 — 숨김 · 품절 상품을 상세 URL 로
	  열지 못하더라도, 담기 요청은 직접 보낼 수 있기 때문이다.
	*/
	public void add(int memberId, int productId, int days) throws SQLException {
		checkDays(days);
		ProductDTO product = productDAO.findAnyById(productId);
		if (product == null) {
			throw new ServiceException("상품을 찾을 수 없습니다.");
		}
		if (!product.isOnSale()) {
			throw new ServiceException("현재 구매할 수 없는 상품입니다.");
		}
		if (product.getStock() < days) {
			throw new ServiceException("재고가 " + product.getStock() + "포 남았습니다. "
					+ product.getStock() + "일분까지 담을 수 있습니다.");
		}
		cartDAO.add(memberId, productId, days);
	}

	/*
	  조합 담기 (/cart/addAll) — 조합 전체를 같은 일수로, 낱개 여러 줄로 담는다 (CONCEPT K1).

	  ★ 조합 탭은 낱개만 고르게 한다. 트랙 · 숨김 · 재고 부족 줄은 서버가 다시 걸러 낸다
	    (화면이 고른 값을 그대로 믿지 않는다). 담지 못한 줄은 건너뛰고 몇 건인지 돌려준다.
	  ★ 여러 줄이 한 단위다 — 한 트랜잭션으로 넣는다.
	  ★ 경고는 막지 않는다. 장바구니 화면이 다시 계산해 보여준다.

	  돌려주는 값: 담은 줄 수와, 건너뛴 상품마다 "이름(이유)" 목록 — 화면이 어떤 상품을 뺐는지 알린다.
	*/
	public static class AddAllResult {
		public final int added;
		public final List<String> skipped;

		AddAllResult(int added, List<String> skipped) {
			this.added = added;
			this.skipped = skipped;
		}
	}

	public AddAllResult addAll(int memberId, List<Integer> productIds, int days) throws SQLException {
		checkDays(days);
		Set<Integer> ids = new LinkedHashSet<Integer>(productIds);
		if (ids.isEmpty()) {
			throw new ServiceException("담을 영양제를 하나 이상 고르세요.");
		}

		Map<Integer, ProductDTO> products = productDAO.findAnyByIds(ids);
		List<ProductDTO> targets = new ArrayList<ProductDTO>();
		List<String> skipped = new ArrayList<String>();
		for (Integer id : ids) {
			ProductDTO p = products.get(id);
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
			throw new ServiceException("지금 담을 수 있는 영양제가 없습니다."
					+ (skipped.isEmpty() ? "" : " " + String.join(", ", skipped)));
		}

		Connection conn = null;
		try {
			conn = DBUtil.beginTransaction();
			for (ProductDTO p : targets) {
				cartDAO.add(conn, memberId, p.getProductId(), days);
			}
			DBUtil.commit(conn);
		} catch (SQLException e) {
			DBUtil.rollback(conn);
			throw e;
		} finally {
			DBUtil.release(conn);
		}
		return new AddAllResult(targets.size(), skipped);
	}

	public void updateQuantity(int cartId, int memberId, int days) throws SQLException {
		checkDays(days);
		if (cartDAO.updateQuantity(cartId, memberId, days) == 0) {
			throw new ServiceException("장바구니 항목을 찾을 수 없습니다.");
		}
	}

	public void delete(int cartId, int memberId) throws SQLException {
		cartDAO.delete(cartId, memberId);
	}

	private void checkDays(int days) throws SQLException {
		int max = configService.maxIntakeDays();
		if (days < 1 || days > max) {
			throw new ServiceException("섭취 일수는 1~" + max + "일로 입력하세요.");
		}
	}
}
