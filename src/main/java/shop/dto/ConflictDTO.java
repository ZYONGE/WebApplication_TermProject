package shop.dto;

import java.sql.Timestamp;

/*
  동시 섭취 금지 조합 — supplement_conflict (ERD § 9)
  productA < productB 로 저장된다. 이름은 조인 필드다.
*/
public class ConflictDTO {

	private int conflictId;
	private int productA;
	private int productB;
	private String productAName;
	private String productBName;
	private String message;
	private Timestamp createdAt;

	/* 한쪽 상품에서 본 "상대 상품" — 상품상세의 "함께 드시지 마세요" 목록이 쓴다. */
	public String otherName(int productId) {
		return productId == productA ? productBName : productAName;
	}

	public int otherId(int productId) {
		return productId == productA ? productB : productA;
	}


	public int getConflictId() {
		return conflictId;
	}

	public void setConflictId(int conflictId) {
		this.conflictId = conflictId;
	}

	public int getProductA() {
		return productA;
	}

	public void setProductA(int productA) {
		this.productA = productA;
	}

	public int getProductB() {
		return productB;
	}

	public void setProductB(int productB) {
		this.productB = productB;
	}

	public String getProductAName() {
		return productAName;
	}

	public void setProductAName(String productAName) {
		this.productAName = productAName;
	}

	public String getProductBName() {
		return productBName;
	}

	public void setProductBName(String productBName) {
		this.productBName = productBName;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public Timestamp getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(Timestamp createdAt) {
		this.createdAt = createdAt;
	}
}
