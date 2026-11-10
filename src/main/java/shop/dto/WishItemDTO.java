package shop.dto;

import java.sql.Timestamp;

/* 찜 항목 */
public class WishItemDTO {

	private int memberId;
	private int productId;
	private String productType;
	private String productName;
	private String thumbnail;
	private String status;
	private int price;
	private Timestamp createdAt;


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

	public Timestamp getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(Timestamp createdAt) {
		this.createdAt = createdAt;
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
