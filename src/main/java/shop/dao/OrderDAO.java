package shop.dao;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import shop.model.Item;
import shop.model.Order;
import shop.model.Page;

/* 주문 · 주문 상품 · 결제 · 배송 (orders, order_item, payment, delivery, delivery_history) */

public class OrderDAO {

	/* 배송 단계 — 이 순서로만 넘어간다 */
	public static final String[] FLOW = { "결제완료", "상품준비", "배송중", "배송완료" };
	/* 단계마다 시각을 남기는 컬럼 */
	private static final String[] TIME_COLUMN = { "paid_at", "ready_at", "shipped_at", "delivered_at" };

	public static int step(String status) {
		return Arrays.asList(FLOW).indexOf(status);
	}

	/* 주문 + 결제 + 배송을 한 행으로 */
	private static final String SELECT = "SELECT o.*, pm.method AS pay_method, pm.pay_status, pm.refunded_at,"
		+ " d.delivery_id, d.status AS delivery_status, d.courier, d.tracking_no";
	private static final String FROM = "FROM orders o LEFT JOIN payment pm ON pm.order_id = o.order_id"
		+ " LEFT JOIN delivery d ON d.order_id = o.order_id";

	/* 주문 하나 — 상품 · 배송 기록까지. memberId 가 0 이 아니면 그 회원의 주문만 */
	public static Order detail(int orderId, int memberId) throws SQLException {
		Order order = memberId > 0
				? DB.one(Order.class, SELECT + " " + FROM + " WHERE o.order_id = ? AND o.member_id = ?", orderId, memberId)
				: DB.one(Order.class, SELECT + " " + FROM + " WHERE o.order_id = ?", orderId);
		if (order != null) {
			order.setItems(items(orderId));
			if (order.getDeliveryId() != null) {
				order.setHistory(DB.maps("SELECT h.*, m.name AS changed_by_name FROM delivery_history h"
						+ " LEFT JOIN member m ON m.member_id = h.changed_by WHERE h.delivery_id = ?"
						+ " ORDER BY h.changed_at, h.history_id", order.getDeliveryId()));
			}
		}
		return order;
	}

	public static List<Item> items(int orderId) throws SQLException {
		return DB.list(Item.class, "SELECT oi.*, r.review_id IS NOT NULL AS review_written FROM order_item oi"
				+ " LEFT JOIN review r ON r.item_id = oi.item_id WHERE oi.order_id = ? ORDER BY oi.item_id", orderId);
	}

	/* 내 주문 목록 — 주문마다 상품 목록까지 */
	public static Page<Order> myOrders(int memberId, int page, int size) throws SQLException {
		Page<Order> result = DB.page(Order.class, SELECT, FROM + " WHERE o.member_id = ?",
				"ORDER BY o.ordered_at DESC, o.order_id DESC", page, size, memberId);
		for (Order o : result.getList()) {
			o.setItems(items(o.getOrderId()));
		}
		return result;
	}

	/* 주문 목록 (관리자) — v_admin_order 뷰에 회원 · 결제 · 배송이 모여 있다 */
	public static Page<Order> adminPage(String keyword, String deliveryStatus, String orderStatus, int page)
			throws SQLException {
		StringBuilder where = new StringBuilder("FROM v_admin_order WHERE 1 = 1");
		List<Object> args = new ArrayList<Object>();
		if (keyword != null && keyword.length() > 0) {
			where.append(" AND (order_no LIKE ? OR login_id LIKE ? OR receiver LIKE ?)");
			String like = ProductDAO.like(keyword);
			args.add(like);
			args.add(like);
			args.add(like);
		}
		if (deliveryStatus != null && deliveryStatus.length() > 0) {
			where.append(" AND delivery_status = ?");
			args.add(deliveryStatus);
		}
		if ("정상".equals(orderStatus) || "취소".equals(orderStatus)) {
			where.append(" AND order_status = ?");
			args.add(orderStatus);
		}
		return DB.page(Order.class, "SELECT *", where.toString(), "ORDER BY ordered_at DESC, order_id DESC", page, 10, args);
	}

	/* ---------- 주문 만들기 (트랜잭션 안에서 부른다) ---------- */

	/* 주문번호는 저장한 뒤 "주문일-번호(4자리)" 로 채운다 */
	public static int insertOrder(int memberId, int totalPrice, int deliveryFee, String receiver, String receiverPhone,
			String zipcode, String addr1, String addr2, String memo) throws SQLException {
		int orderId = DB.insert("INSERT INTO orders (order_no, member_id, total_price, delivery_fee, order_status,"
				+ " receiver, receiver_phone, zipcode, addr1, addr2, delivery_memo)"
				+ " VALUES (CONCAT('T', LPAD(CONNECTION_ID(), 10, '0')), ?, ?, ?, '정상', ?, ?, ?, ?, ?, ?)",
				memberId, totalPrice, deliveryFee, receiver, receiverPhone, zipcode, addr1, addr2, memo);
		DB.update("UPDATE orders SET order_no = CONCAT(DATE_FORMAT(ordered_at, '%Y%m%d'), '-', LPAD(order_id, 4, '0'))"
				+ " WHERE order_id = ?", orderId);
		return orderId;
	}

	public static void insertItem(int orderId, Item item, String trackSnapshot) throws SQLException {
		DB.update("INSERT INTO order_item (order_id, product_id, product_name, price, quantity, track_snapshot)"
				+ " VALUES (?, ?, ?, ?, ?, ?)", orderId, item.getProductId(), item.getProductName(), item.getPrice(),
				item.getQuantity(), trackSnapshot);
	}

	/* 결제 기록 + 배송 시작(결제완료) */
	public static void insertPaymentAndDelivery(int orderId, String method, int amount) throws SQLException {
		DB.update("INSERT INTO payment (order_id, method, amount, pay_status) VALUES (?, ?, ?, '완료')", orderId, method, amount);
		int deliveryId = DB.insert("INSERT INTO delivery (order_id, status, paid_at) VALUES (?, '결제완료', NOW())", orderId);
		addHistory(deliveryId, null, "결제완료", null, false);
	}

	/* ---------- 상태 바꾸기 ---------- */

	/* 주문 취소 / 되살리기 — 바뀐 행 수 */
	public static int setOrderStatus(int orderId, boolean cancel) throws SQLException {
		return cancel
				? DB.update("UPDATE orders SET order_status = '취소', canceled_at = NOW() WHERE order_id = ? AND order_status = '정상'", orderId)
				: DB.update("UPDATE orders SET order_status = '정상', canceled_at = NULL WHERE order_id = ?", orderId);
	}

	/* 결제 환불 / 되살리기 */
	public static int setPayStatus(int orderId, boolean refund) throws SQLException {
		return refund
				? DB.update("UPDATE payment SET pay_status = '환불', refunded_at = NOW() WHERE order_id = ? AND pay_status = '완료'", orderId)
				: DB.update("UPDATE payment SET pay_status = '완료', refunded_at = NULL WHERE order_id = ?", orderId);
	}

	/* 배송의 지금 단계 (배송 정보가 없으면 null) */
	public static String deliveryStatus(int deliveryId) throws SQLException {
		return DB.text("SELECT status FROM delivery WHERE delivery_id = ?", deliveryId);
	}

	/* 다음 단계로 — from 상태일 때만 바뀐다(동시에 누른 경우 대비). 배송중이 되면 운송장도 저장 */
	public static int moveNext(int deliveryId, String from, String to, String courier, String trackingNo)
			throws SQLException {
		String column = TIME_COLUMN[step(to)];
		if ("배송중".equals(to)) {
			return DB.update("UPDATE delivery SET status = ?, " + column + " = NOW(), courier = ?, tracking_no = ?"
					+ " WHERE delivery_id = ? AND status = ?", to, courier, trackingNo, deliveryId, from);
		}
		return DB.update("UPDATE delivery SET status = ?, " + column + " = NOW() WHERE delivery_id = ? AND status = ?",
				to, deliveryId, from);
	}

	/* 강제 변경 (dev) — 그 단계까지의 시각은 채우고, 뒤 단계 시각은 비운다 */
	public static int force(int deliveryId, String to, String courier, String trackingNo) throws SQLException {
		int target = step(to);
		StringBuilder sql = new StringBuilder("UPDATE delivery SET status = ?");
		for (int i = 0; i < FLOW.length; i++) {
			String c = TIME_COLUMN[i];
			sql.append(", ").append(c).append(i <= target ? " = IFNULL(" + c + ", NOW())" : " = NULL");
		}
		if (target >= step("배송중")) {
			sql.append(", courier = ?, tracking_no = ? WHERE delivery_id = ?");
			return DB.update(sql.toString(), to, courier, trackingNo, deliveryId);
		}
		sql.append(", courier = NULL, tracking_no = NULL WHERE delivery_id = ?");
		return DB.update(sql.toString(), to, deliveryId);
	}

	public static void addHistory(int deliveryId, String prev, String next, Integer changedBy, boolean forced)
			throws SQLException {
		DB.update("INSERT INTO delivery_history (delivery_id, prev_status, new_status, changed_by, is_forced)"
				+ " VALUES (?, ?, ?, ?, ?)", deliveryId, prev, next, changedBy, forced ? "Y" : "N");
	}
}
