package shop.dto;

import java.sql.Timestamp;

/* 배송 상태 이력 */
public class DeliveryHistoryDTO {

	private int historyId;
	private int deliveryId;
	private String prevStatus;
	private String newStatus;
	private String isForced;
	private Integer changedBy;
	private String changedByName;
	private Timestamp changedAt;


	public int getHistoryId() {
		return historyId;
	}

	public void setHistoryId(int historyId) {
		this.historyId = historyId;
	}

	public int getDeliveryId() {
		return deliveryId;
	}

	public void setDeliveryId(int deliveryId) {
		this.deliveryId = deliveryId;
	}

	public String getPrevStatus() {
		return prevStatus;
	}

	public void setPrevStatus(String prevStatus) {
		this.prevStatus = prevStatus;
	}

	public String getNewStatus() {
		return newStatus;
	}

	public void setNewStatus(String newStatus) {
		this.newStatus = newStatus;
	}

	public String getIsForced() {
		return isForced;
	}

	public void setIsForced(String isForced) {
		this.isForced = isForced;
	}

	public Integer getChangedBy() {
		return changedBy;
	}

	public void setChangedBy(Integer changedBy) {
		this.changedBy = changedBy;
	}

	public String getChangedByName() {
		return changedByName;
	}

	public void setChangedByName(String changedByName) {
		this.changedByName = changedByName;
	}

	public Timestamp getChangedAt() {
		return changedAt;
	}

	public void setChangedAt(Timestamp changedAt) {
		this.changedAt = changedAt;
	}
}
