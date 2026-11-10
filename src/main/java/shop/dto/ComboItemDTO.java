package shop.dto;

/*
  저장한 조합의 구성 한 줄 — combo_item + product 조인 (ERD § 9)
  가격 · 재고 · 상태는 저장 시점이 아니라 현재값이다.
*/
public class ComboItemDTO {

	private int comboId;
	private int productId;
	private int sortOrder;
	private String productName;
	private String thumbnail;
	private String status;
	private int price;
	private int stock;

	/* 지금 담을 수 있는가 — 탐색 노출 조건과 같다 (판매중 · 재고 > 0) */
	public boolean isAvailable() {
		return "판매중".equals(status) && stock > 0;
	}


	public int getComboId() {
		return comboId;
	}

	public void setComboId(int comboId) {
		this.comboId = comboId;
	}

	public int getProductId() {
		return productId;
	}

	public void setProductId(int productId) {
		this.productId = productId;
	}

	public int getSortOrder() {
		return sortOrder;
	}

	public void setSortOrder(int sortOrder) {
		this.sortOrder = sortOrder;
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
}
