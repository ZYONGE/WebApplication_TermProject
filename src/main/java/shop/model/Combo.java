package shop.model;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/* 내 조합 (combo) — 고른 낱개 영양제 목록 + 섭취 일수 */
public class Combo {
	private int comboId, memberId, days, itemCount;
	private String name;
	private Timestamp createdAt, updatedAt;
	private List<Item> items = new ArrayList<Item>();

	public int getComboId() { return comboId; }
	public int getMemberId() { return memberId; }
	public int getDays() { return days; }
	public int getItemCount() { return itemCount; }
	public String getName() { return name; }
	public Timestamp getCreatedAt() { return createdAt; }
	public Timestamp getUpdatedAt() { return updatedAt; }
	public List<Item> getItems() { return items; }

	public void setItems(List<Item> items) { this.items = items; }

	/* 지금 살 수 있는 상품만의 1포 합계 */
	public int getPackPrice() {
		int sum = 0;
		for (Item item : items) {
			if (item.isAvailable()) {
				sum += item.getPrice();
			}
		}
		return sum;
	}

	public int getTotalPrice() { return getPackPrice() * days; }
}
