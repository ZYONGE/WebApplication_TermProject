package shop.dto;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/* 상품 */
public class ProductDTO {

	private int productId;
	private int categoryId;
	private String categoryName;
	private String name;
	private int price;
	private int stock;
	private String description;
	private String thumbnail;
	private String status;
	private Timestamp createdAt;
	private List<ProductImageDTO> images;
	private double avgRating;
	private int reviewCount;


	public ProductDTO() {
		this.images = new ArrayList<ProductImageDTO>();
	}

	/* 고객 탐색 화면의 노출 조건 — ERD 2.4 */
	public boolean isOnSale() {
		return "판매중".equals(status) && stock > 0;
	}

	public int getProductId() {
		return productId;
	}

	public void setProductId(int productId) {
		this.productId = productId;
	}

	public int getCategoryId() {
		return categoryId;
	}

	public void setCategoryId(int categoryId) {
		this.categoryId = categoryId;
	}

	public String getCategoryName() {
		return categoryName;
	}

	public void setCategoryName(String categoryName) {
		this.categoryName = categoryName;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
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

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
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

	public Timestamp getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(Timestamp createdAt) {
		this.createdAt = createdAt;
	}

	public List<ProductImageDTO> getImages() {
		return images;
	}

	public void setImages(List<ProductImageDTO> images) {
		this.images = images;
	}

	public double getAvgRating() {
		return avgRating;
	}

	public void setAvgRating(double avgRating) {
		this.avgRating = avgRating;
	}

	public int getReviewCount() {
		return reviewCount;
	}

	public void setReviewCount(int reviewCount) {
		this.reviewCount = reviewCount;
	}
}
