package shop.model;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/* 주문 (orders) — 결제(payment) · 배송(delivery) 값도 한 행으로 함께 읽는다 */
public class Order {
	private int orderId, memberId, totalPrice, deliveryFee, itemCount;
	private String orderNo, orderStatus, receiver, receiverPhone, zipcode, addr1, addr2, deliveryMemo;
	private String loginId, memberName, firstProductName;
	private Timestamp orderedAt, canceledAt;
	private String payMethod, payStatus;
	private Timestamp refundedAt;
	private Integer deliveryId;
	private String deliveryStatus, courier, trackingNo;
	private List<Item> items = new ArrayList<Item>();
	private List<Map<String, Object>> history = new ArrayList<Map<String, Object>>();

	public int getOrderId() { return orderId; }
	public int getMemberId() { return memberId; }
	public int getTotalPrice() { return totalPrice; }
	public int getDeliveryFee() { return deliveryFee; }
	public int getItemCount() { return itemCount; }
	public String getOrderNo() { return orderNo; }
	public String getOrderStatus() { return orderStatus; }
	public String getReceiver() { return receiver; }
	public String getReceiverPhone() { return receiverPhone; }
	public String getZipcode() { return zipcode; }
	public String getAddr1() { return addr1; }
	public String getAddr2() { return addr2; }
	public String getDeliveryMemo() { return deliveryMemo; }
	public String getLoginId() { return loginId; }
	public String getMemberName() { return memberName; }
	public Timestamp getOrderedAt() { return orderedAt; }
	public Timestamp getCanceledAt() { return canceledAt; }
	public String getPayMethod() { return payMethod; }
	public String getPayStatus() { return payStatus; }
	public Timestamp getRefundedAt() { return refundedAt; }
	public Integer getDeliveryId() { return deliveryId; }
	public String getDeliveryStatus() { return deliveryStatus; }
	public String getCourier() { return courier; }
	public String getTrackingNo() { return trackingNo; }
	public List<Item> getItems() { return items; }
	public List<Map<String, Object>> getHistory() { return history; }

	public void setItems(List<Item> items) { this.items = items; }
	public void setHistory(List<Map<String, Object>> history) { this.history = history; }

	public int getPayAmount() { return totalPrice + deliveryFee; }

	/* 발송 전(결제완료 · 상품준비)에만 취소할 수 있다 */
	public boolean isCancelable() {
		return "정상".equals(orderStatus)
			&& ("결제완료".equals(deliveryStatus) || "상품준비".equals(deliveryStatus));
	}

	/* "원포올 외 2건" — 관리자 목록은 뷰의 값, 내 주문 목록은 상품 목록으로 */
	public String getItemSummary() {
		String first = firstProductName != null ? firstProductName : items.isEmpty() ? null : items.get(0).getProductName();
		int count = firstProductName != null ? itemCount : items.size();
		if (first == null) {
			return "";
		}
		return count > 1 ? first + " 외 " + (count - 1) + "건" : first;
	}

	/* 화면에 보일 상태 — 취소 주문은 배송 단계와 상관없이 "취소" */
	public String getDisplayStatus() {
		if ("취소".equals(orderStatus)) {
			return "취소";
		}
		return deliveryStatus == null ? "-" : deliveryStatus;
	}
}
