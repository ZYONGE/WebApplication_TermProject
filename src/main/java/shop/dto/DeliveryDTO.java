package shop.dto;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/* 배송 */
public class DeliveryDTO {

	private int deliveryId;
	private int orderId;
	private String status;
	private String courier;
	private String trackingNo;
	private Timestamp paidAt;
	private Timestamp readyAt;
	private Timestamp shippedAt;
	private Timestamp deliveredAt;
	private List<DeliveryHistoryDTO> history;


	public DeliveryDTO() {
		this.history = new ArrayList<DeliveryHistoryDTO>();
	}

	public int getDeliveryId() {
		return deliveryId;
	}

	public void setDeliveryId(int deliveryId) {
		this.deliveryId = deliveryId;
	}

	public int getOrderId() {
		return orderId;
	}

	public void setOrderId(int orderId) {
		this.orderId = orderId;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public String getCourier() {
		return courier;
	}

	public void setCourier(String courier) {
		this.courier = courier;
	}

	public String getTrackingNo() {
		return trackingNo;
	}

	public void setTrackingNo(String trackingNo) {
		this.trackingNo = trackingNo;
	}

	public Timestamp getPaidAt() {
		return paidAt;
	}

	public void setPaidAt(Timestamp paidAt) {
		this.paidAt = paidAt;
	}

	public Timestamp getReadyAt() {
		return readyAt;
	}

	public void setReadyAt(Timestamp readyAt) {
		this.readyAt = readyAt;
	}

	public Timestamp getShippedAt() {
		return shippedAt;
	}

	public void setShippedAt(Timestamp shippedAt) {
		this.shippedAt = shippedAt;
	}

	public Timestamp getDeliveredAt() {
		return deliveredAt;
	}

	public void setDeliveredAt(Timestamp deliveredAt) {
		this.deliveredAt = deliveredAt;
	}

	public List<DeliveryHistoryDTO> getHistory() {
		return history;
	}

	public void setHistory(List<DeliveryHistoryDTO> history) {
		this.history = history;
	}
}
