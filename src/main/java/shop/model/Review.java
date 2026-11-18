package shop.model;

import java.sql.Timestamp;

/* 리뷰 (review) — 작성자 이름 · 상품 이름을 함께 읽는다 */
public class Review {
	private int reviewId, memberId, productId, itemId, rating;
	private String memberName, productName, content;
	private Timestamp createdAt;

	public int getReviewId() { return reviewId; }
	public int getMemberId() { return memberId; }
	public int getProductId() { return productId; }
	public int getItemId() { return itemId; }
	public int getRating() { return rating; }
	public String getMemberName() { return memberName; }
	public String getProductName() { return productName; }
	public String getContent() { return content; }
	public Timestamp getCreatedAt() { return createdAt; }

	public void setMemberName(String memberName) { this.memberName = memberName; }
}
