package shop.model;

import java.sql.Timestamp;

/*
  함께 먹으면 안 되는 두 영양제 (supplement_conflict).
  장바구니 · 조합 경고에도 쓴다 — 그때 type 은 CONFLICT(금지 조합) 또는 DUPLICATE(같은 영양제 중복),
  viaA · viaB 는 그 영양제가 들어 있는 트랙 이름(직접 고른 낱개면 null)이다.
*/
public class Conflict {
	private int conflictId, productA, productB;
	private String productAName, productBName, message, type, viaA, viaB;
	private Timestamp createdAt;

	public Conflict() {
	}

	public Conflict(String type, int a, String nameA, String viaA, int b, String nameB, String viaB, String message) {
		this.type = type;
		this.productA = a;
		this.productAName = nameA;
		this.viaA = viaA;
		this.productB = b;
		this.productBName = nameB;
		this.viaB = viaB;
		this.message = message;
	}

	public int getConflictId() { return conflictId; }
	public int getProductA() { return productA; }
	public int getProductB() { return productB; }
	public String getProductAName() { return productAName; }
	public String getProductBName() { return productBName; }
	public String getMessage() { return message; }
	public String getType() { return type; }
	public String getViaA() { return viaA; }
	public String getViaB() { return viaB; }
	public Timestamp getCreatedAt() { return createdAt; }
	public boolean isConflict() { return "CONFLICT".equals(type); }
}
