package shop.dto;

/* 장바구니 줄 */
public class CartItemDTO {

	private int cartId;
	private int memberId;
	private int productId;
	private String productType;
	private String productName;
	private String thumbnail;
	private String status;
	private int price;
	private int stock;
	private int quantity;


	/* 유도 필드 — DB 컬럼이 아니다 */
	public int getLineTotal() {
		return price * quantity;
	}

	/* 재고가 모자라거나 숨김 처리된 줄은 주문 대상에서 뺀다 */
	public boolean isOrderable() {
		return "판매중".equals(status) && stock >= quantity;
	}

	public int getCartId() {
		return cartId;
	}

	public void setCartId(int cartId) {
		this.cartId = cartId;
	}

	public int getMemberId() {
		return memberId;
	}

	public void setMemberId(int memberId) {
		this.memberId = memberId;
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

	public String getThumbnail() {
		return thumbnail;
	}

	public void setThumbnail(String thumbnail) {
		this.thumbnail = thumbnail;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public int getPrice() {
		return price;
	}

	public void setPrice(int price) {
		this.price = price;
	}

	public int getStock() {
		return stock;
	}

	public void setStock(int stock) {
		this.stock = stock;
	}

	public int getQuantity() {
		return quantity;
	}

	public void setQuantity(int quantity) {
		this.quantity = quantity;
	}

	/* 트랙 패키지인가 — 낱개(SINGLE) / 트랙(TRACK) (ERD § 7.1) */
	public boolean isTrack() {
		return "TRACK".equals(productType);
	}

	/* 화면 표기용 구분 이름 */
	public String getTypeLabel() {
		return isTrack() ? "트랙" : "낱개";
	}

	public String getProductType() {
		return productType;
	}

	public void setProductType(String productType) {
		this.productType = productType;
	}
}
