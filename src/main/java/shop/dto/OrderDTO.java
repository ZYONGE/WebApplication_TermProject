package shop.dto;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/* 주문 */
public class OrderDTO {

	private int orderId;
	private String orderNo;
	private int memberId;
	private int totalPrice;
	private int deliveryFee;
	private String orderStatus;
	private String receiver;
	private String receiverPhone;
	private String zipcode;
	private String addr1;
	private String addr2;
	private String deliveryMemo;
	private Timestamp orderedAt;
	private Timestamp canceledAt;
	/* 관리자 목록용 조인 필드 — v_admin_order 가 함께 준다 (ERD § 7 에 더한 표시용 값) */
	private String loginId;
	private String memberName;
	private int itemCount;
	private String firstProductName;

	private List<OrderItemDTO> items;
	private PaymentDTO payment;
	private DeliveryDTO delivery;


	public OrderDTO() {
		this.items = new ArrayList<OrderItemDTO>();
	}

	/* 유도 필드 — 상품합계 + 배송비 */
	public int getPayAmount() {
		return totalPrice + deliveryFee;
	}

	/* 취소 가능 여부 — 배송이 시작되기 전까지만 (ERD 6.3) */
	public boolean isCancelable() {
		if (!"정상".equals(orderStatus) || delivery == null) {
			return false;
		}
		return "결제완료".equals(delivery.getStatus())
			|| "상품준비".equals(delivery.getStatus());
	}

	/* "티셔츠 외 2건" — 관리자 주문 목록의 제목 */
	public String getItemSummary() {
		if (firstProductName == null) {
			return "";
		}
		return itemCount > 1 ? firstProductName + " 외 " + (itemCount - 1) + "건" : firstProductName;
	}

	public String getLoginId() {
		return loginId;
	}

	public void setLoginId(String loginId) {
		this.loginId = loginId;
	}

	public String getMemberName() {
		return memberName;
	}

	public void setMemberName(String memberName) {
		this.memberName = memberName;
	}

	public int getItemCount() {
		return itemCount;
	}

	public void setItemCount(int itemCount) {
		this.itemCount = itemCount;
	}

	public String getFirstProductName() {
		return firstProductName;
	}

	public void setFirstProductName(String firstProductName) {
		this.firstProductName = firstProductName;
	}

	public int getOrderId() {
		return orderId;
	}

	public void setOrderId(int orderId) {
		this.orderId = orderId;
	}

	public String getOrderNo() {
		return orderNo;
	}

	public void setOrderNo(String orderNo) {
		this.orderNo = orderNo;
	}

	public int getMemberId() {
		return memberId;
	}

	public void setMemberId(int memberId) {
		this.memberId = memberId;
	}

	public int getTotalPrice() {
		return totalPrice;
	}

	public void setTotalPrice(int totalPrice) {
		this.totalPrice = totalPrice;
	}

	public int getDeliveryFee() {
		return deliveryFee;
	}

	public void setDeliveryFee(int deliveryFee) {
		this.deliveryFee = deliveryFee;
	}

	public String getOrderStatus() {
		return orderStatus;
	}

	public void setOrderStatus(String orderStatus) {
		this.orderStatus = orderStatus;
	}

	public String getReceiver() {
		return receiver;
	}

	public void setReceiver(String receiver) {
		this.receiver = receiver;
	}

	public String getReceiverPhone() {
		return receiverPhone;
	}

	public void setReceiverPhone(String receiverPhone) {
		this.receiverPhone = receiverPhone;
	}

	public String getZipcode() {
		return zipcode;
	}

	public void setZipcode(String zipcode) {
		this.zipcode = zipcode;
	}

	public String getAddr1() {
		return addr1;
	}

	public void setAddr1(String addr1) {
		this.addr1 = addr1;
	}

	public String getAddr2() {
		return addr2;
	}

	public void setAddr2(String addr2) {
		this.addr2 = addr2;
	}

	public String getDeliveryMemo() {
		return deliveryMemo;
	}

	public void setDeliveryMemo(String deliveryMemo) {
		this.deliveryMemo = deliveryMemo;
	}

	public Timestamp getOrderedAt() {
		return orderedAt;
	}

	public void setOrderedAt(Timestamp orderedAt) {
		this.orderedAt = orderedAt;
	}

	public Timestamp getCanceledAt() {
		return canceledAt;
	}

	public void setCanceledAt(Timestamp canceledAt) {
		this.canceledAt = canceledAt;
	}

	public List<OrderItemDTO> getItems() {
		return items;
	}

	public void setItems(List<OrderItemDTO> items) {
		this.items = items;
	}

	public PaymentDTO getPayment() {
		return payment;
	}

	public void setPayment(PaymentDTO payment) {
		this.payment = payment;
	}

	public DeliveryDTO getDelivery() {
		return delivery;
	}

	public void setDelivery(DeliveryDTO delivery) {
		this.delivery = delivery;
	}
}
