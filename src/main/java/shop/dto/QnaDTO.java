package shop.dto;

import java.sql.Timestamp;

/* 상품 문의 */
public class QnaDTO {

	private int qnaId;
	private int memberId;
	private int productId;
	private String memberName;
	private String productName;
	private String question;
	private String isSecret;
	private String answer;
	private Timestamp createdAt;
	private Timestamp answeredAt;


	public boolean isAnswered() {
		return answer != null && answer.length() > 0;
	}

	public boolean isSecretPost() {
		return "Y".equals(isSecret);
	}

	public int getQnaId() {
		return qnaId;
	}

	public void setQnaId(int qnaId) {
		this.qnaId = qnaId;
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

	public String getMemberName() {
		return memberName;
	}

	public void setMemberName(String memberName) {
		this.memberName = memberName;
	}

	public String getProductName() {
		return productName;
	}

	public void setProductName(String productName) {
		this.productName = productName;
	}

	public String getQuestion() {
		return question;
	}

	public void setQuestion(String question) {
		this.question = question;
	}

	public String getIsSecret() {
		return isSecret;
	}

	public void setIsSecret(String isSecret) {
		this.isSecret = isSecret;
	}

	public String getAnswer() {
		return answer;
	}

	public void setAnswer(String answer) {
		this.answer = answer;
	}

	public Timestamp getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(Timestamp createdAt) {
		this.createdAt = createdAt;
	}

	public Timestamp getAnsweredAt() {
		return answeredAt;
	}

	public void setAnsweredAt(Timestamp answeredAt) {
		this.answeredAt = answeredAt;
	}
}
