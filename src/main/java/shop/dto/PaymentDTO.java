package shop.dto;

import java.sql.Timestamp;

/* 결제 */
public class PaymentDTO {

	private int paymentId;
	private int orderId;
	private String method;
	private String payStatus;
	private int amount;
	private Timestamp paidAt;
	private Timestamp refundedAt;


	public int getPaymentId() {
		return paymentId;
	}

	public void setPaymentId(int paymentId) {
		this.paymentId = paymentId;
	}

	public int getOrderId() {
		return orderId;
	}

	public void setOrderId(int orderId) {
		this.orderId = orderId;
	}

	public String getMethod() {
		return method;
	}

	public void setMethod(String method) {
		this.method = method;
	}

	public String getPayStatus() {
		return payStatus;
	}

	public void setPayStatus(String payStatus) {
		this.payStatus = payStatus;
	}

	public int getAmount() {
		return amount;
	}

	public void setAmount(int amount) {
		this.amount = amount;
	}

	public Timestamp getPaidAt() {
		return paidAt;
	}

	public void setPaidAt(Timestamp paidAt) {
		this.paidAt = paidAt;
	}

	public Timestamp getRefundedAt() {
		return refundedAt;
	}

	public void setRefundedAt(Timestamp refundedAt) {
		this.refundedAt = refundedAt;
	}
}
