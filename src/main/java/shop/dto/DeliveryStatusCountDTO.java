package shop.dto;

/* 배송 상태별 건수 (v_delivery_status_count) */
public class DeliveryStatusCountDTO {

	private String deliveryStatus;
	private int orderCount;


	public String getDeliveryStatus() {
		return deliveryStatus;
	}

	public void setDeliveryStatus(String deliveryStatus) {
		this.deliveryStatus = deliveryStatus;
	}

	public int getOrderCount() {
		return orderCount;
	}

	public void setOrderCount(int orderCount) {
		this.orderCount = orderCount;
	}
}
