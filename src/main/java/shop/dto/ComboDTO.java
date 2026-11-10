package shop.dto;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/*
  저장한 조합 (내 조합) — combo (ERD § 9)
  가격은 저장하지 않는다. packPrice 는 담을 수 있는 구성의 현재 1포 가격 합이다.
*/
public class ComboDTO {

	private int comboId;
	private int memberId;
	private String name;
	private int days;
	private Timestamp createdAt;
	private Timestamp updatedAt;
	private List<ComboItemDTO> items;
	private int itemCount;

	public ComboDTO() {
		this.items = new ArrayList<ComboItemDTO>();
	}

	/* 유도 필드 — 담을 수 있는 구성의 1포 가격 합 */
	public int getPackPrice() {
		int sum = 0;
		for (ComboItemDTO item : items) {
			if (item.isAvailable()) {
				sum += item.getPrice();
			}
		}
		return sum;
	}

	/* 유도 필드 — 1포 합계 × 일수 */
	public int getTotalPrice() {
		return getPackPrice() * days;
	}


	public int getComboId() {
		return comboId;
	}

	public void setComboId(int comboId) {
		this.comboId = comboId;
	}

	public int getMemberId() {
		return memberId;
	}

	public void setMemberId(int memberId) {
		this.memberId = memberId;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public int getDays() {
		return days;
	}

	public void setDays(int days) {
		this.days = days;
	}

	public Timestamp getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(Timestamp createdAt) {
		this.createdAt = createdAt;
	}

	public Timestamp getUpdatedAt() {
		return updatedAt;
	}

	public void setUpdatedAt(Timestamp updatedAt) {
		this.updatedAt = updatedAt;
	}

	public List<ComboItemDTO> getItems() {
		return items;
	}

	public void setItems(List<ComboItemDTO> items) {
		this.items = items;
	}

	public int getItemCount() {
		return itemCount;
	}

	public void setItemCount(int itemCount) {
		this.itemCount = itemCount;
	}
}
