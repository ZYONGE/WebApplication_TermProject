package shop.dto;

/*
  섭취 경고 1건 — 테이블 없음, IntakeService 의 계산 결과 (ERD § 9)

  type     CONFLICT 금지 조합 / DUPLICATE 중복 섭취
  viaA·B   그 낱개가 들어온 트랙 이름. 낱개로 직접 골랐으면 null
  경고는 안내만 하고 구매를 막지 않는다 (ERD § 7.1).
*/
public class IntakeWarningDTO {

	private String type;
	private int productA;
	private int productB;
	private String nameA;
	private String nameB;
	private String viaA;
	private String viaB;
	private String message;

	public static final String CONFLICT = "CONFLICT";
	public static final String DUPLICATE = "DUPLICATE";

	public boolean isConflict() {
		return CONFLICT.equals(type);
	}


	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
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

	public String getNameA() {
		return nameA;
	}

	public void setNameA(String nameA) {
		this.nameA = nameA;
	}

	public String getNameB() {
		return nameB;
	}

	public void setNameB(String nameB) {
		this.nameB = nameB;
	}

	public String getViaA() {
		return viaA;
	}

	public void setViaA(String viaA) {
		this.viaA = viaA;
	}

	public String getViaB() {
		return viaB;
	}

	public void setViaB(String viaB) {
		this.viaB = viaB;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}
}
