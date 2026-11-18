package shop.model;

import java.sql.Timestamp;

/* 상품 한 줄 — 장바구니(cart) · 찜(wishlist) · 조합(combo_item) · 주문 상품(order_item) 이 함께 쓴다 */
public class Item {
	private int cartId, itemId, orderId, comboId, memberId, productId, price, stock, quantity, sortOrder;
	private String productType, productName, thumbnail, status, trackSnapshot;
	private boolean reviewWritten;
	private Timestamp createdAt;

	public int getCartId() { return cartId; }
	public int getItemId() { return itemId; }
	public int getOrderId() { return orderId; }
	public int getComboId() { return comboId; }
	public int getMemberId() { return memberId; }
	public int getProductId() { return productId; }
	public int getPrice() { return price; }
	public int getStock() { return stock; }
	public int getQuantity() { return quantity; }
	public int getSortOrder() { return sortOrder; }
	public String getProductType() { return productType; }
	public String getProductName() { return productName; }
	public String getThumbnail() { return thumbnail; }
	public String getStatus() { return status; }
	public String getTrackSnapshot() { return trackSnapshot; }
	public boolean isReviewWritten() { return reviewWritten; }
	public Timestamp getCreatedAt() { return createdAt; }

	/* 합계 = 1포 가격 × 일수 */
	public int getLineTotal() { return price * quantity; }
	public boolean isTrack() { return "TRACK".equals(productType); }
	public String getTypeLabel() { return isTrack() ? "트랙" : "낱개"; }

	/* 주문 가능 = 판매중이고 재고가 담은 일수 이상 */
	public boolean isOrderable() { return "판매중".equals(status) && stock >= quantity; }

	/* 지금 살 수 있는 상품인가 (조합 · 찜) */
	public boolean isAvailable() { return "판매중".equals(status) && stock > 0; }

	/* 장바구니의 상태 칸 */
	public String getCartState() {
		if (!"판매중".equals(status)) {
			return "판매 중지";
		}
		if (stock <= 0) {
			return "재고 없음";
		}
		return stock < quantity ? "재고 " + stock + "포 (일수를 줄이세요)" : "주문 가능";
	}

	public String getStateLabel() {
		return !"판매중".equals(status) ? "판매 중지" : stock > 0 ? "판매중" : "재고 없음";
	}
}
