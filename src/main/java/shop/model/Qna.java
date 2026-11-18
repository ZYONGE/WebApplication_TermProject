package shop.model;

import java.sql.Timestamp;

/* 상품 문의 (qna) */
public class Qna {
	private int qnaId, memberId, productId;
	private String memberName, productName, question, isSecret, answer;
	private Timestamp createdAt, answeredAt;

	public int getQnaId() { return qnaId; }
	public int getMemberId() { return memberId; }
	public int getProductId() { return productId; }
	public String getMemberName() { return memberName; }
	public String getProductName() { return productName; }
	public String getQuestion() { return question; }
	public String getIsSecret() { return isSecret; }
	public String getAnswer() { return answer; }
	public Timestamp getCreatedAt() { return createdAt; }
	public Timestamp getAnsweredAt() { return answeredAt; }
	public boolean isAnswered() { return answer != null && answer.length() > 0; }
	public boolean isSecretPost() { return "Y".equals(isSecret); }

	public void setMemberName(String memberName) { this.memberName = memberName; }

	/* 비밀글을 남이 볼 때 — 내용을 지운다 */
	public void hide() {
		question = null;
		answer = null;
	}
}
