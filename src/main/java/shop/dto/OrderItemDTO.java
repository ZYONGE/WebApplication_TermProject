package shop.dto;

/* 주문 상품 (주문 시점 스냅샷) */
public class OrderItemDTO {

	private int itemId;
	private int orderId;
	private int productId;
	private String productName;
	private int price;
	private int quantity;
	private boolean reviewWritten;


	public int getLineTotal() {
		return price * quantity;
	}

	public int getItemId() {
		return itemId;
	}

	public void setItemId(int itemId) {
		this.itemId = itemId;
	}

	public int getOrderId() {
		return orderId;
	}

	public void setOrderId(int orderId) {
		this.orderId = orderId;
	}

	public int getProductId() {
		return productId;
	}

	public void setProductId(int productId) {
		this.productId = productId;
	}

	public String getProductName() {
		return productName;
	}

	public void setProductName(String productName) {
		this.productName = productName;
	}

	public int getPrice() {
		return price;
	}

	public void setPrice(int price) {
		this.price = price;
	}

	public int getQuantity() {
		return quantity;
	}

	public void setQuantity(int quantity) {
		this.quantity = quantity;
	}

	public boolean isReviewWritten() {
		return reviewWritten;
	}

	public void setReviewWritten(boolean reviewWritten) {
		this.reviewWritten = reviewWritten;
	}
}
