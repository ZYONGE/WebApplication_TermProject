package shop.model;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/* 상품 (product) — 낱개(SINGLE) 또는 트랙 패키지(TRACK). 가격은 1포 기준 */
public class Product {
	private int productId, categoryId, price, stock, reviewCount;
	private String categoryName, productType, name, description, thumbnail, status;
	private double avgRating;
	private Timestamp createdAt;
	private List<Map<String, Object>> images = new ArrayList<Map<String, Object>>();
	private List<Product> trackItems = new ArrayList<Product>();

	public int getProductId() { return productId; }
	public int getCategoryId() { return categoryId; }
	public int getPrice() { return price; }
	public int getStock() { return stock; }
	public int getReviewCount() { return reviewCount; }
	public String getCategoryName() { return categoryName; }
	public String getProductType() { return productType; }
	public String getName() { return name; }
	public String getDescription() { return description; }
	public String getThumbnail() { return thumbnail; }
	public String getStatus() { return status; }
	public double getAvgRating() { return avgRating; }
	public Timestamp getCreatedAt() { return createdAt; }
	public List<Map<String, Object>> getImages() { return images; }
	public List<Product> getTrackItems() { return trackItems; }

	public void setImages(List<Map<String, Object>> images) { this.images = images; }
	public void setTrackItems(List<Product> trackItems) { this.trackItems = trackItems; }

	public boolean isOnSale() { return "판매중".equals(status) && stock > 0; }
	public boolean isTrack() { return "TRACK".equals(productType); }
	public String getTypeLabel() { return isTrack() ? "트랙" : "낱개"; }

	/* "종합비타민 + 오메가3 + …" — 트랙 구성 이름 */
	public String getItemNames() {
		StringBuilder sb = new StringBuilder();
		for (Product item : trackItems) {
			sb.append(sb.length() == 0 ? "" : " + ").append(item.getName());
		}
		return sb.toString();
	}

	/* 구성품을 낱개로 따로 살 때의 1포 가격 합 */
	public int getItemPriceSum() {
		int sum = 0;
		for (Product item : trackItems) {
			sum += item.getPrice();
		}
		return sum;
	}
}
