package shop.dto;

import java.sql.Date;

/* 일별 매출 (v_sales_daily) */
public class SalesDailyDTO {

	private Date salesDate;
	private int orderCount;
	private int goodsAmount;
	private int deliveryAmount;
	private int totalAmount;


	public Date getSalesDate() {
		return salesDate;
	}

	public void setSalesDate(Date salesDate) {
		this.salesDate = salesDate;
	}

	public int getOrderCount() {
		return orderCount;
	}

	public void setOrderCount(int orderCount) {
		this.orderCount = orderCount;
	}

	public int getGoodsAmount() {
		return goodsAmount;
	}

	public void setGoodsAmount(int goodsAmount) {
		this.goodsAmount = goodsAmount;
	}

	public int getDeliveryAmount() {
		return deliveryAmount;
	}

	public void setDeliveryAmount(int deliveryAmount) {
		this.deliveryAmount = deliveryAmount;
	}

	public int getTotalAmount() {
		return totalAmount;
	}

	public void setTotalAmount(int totalAmount) {
		this.totalAmount = totalAmount;
	}
}
