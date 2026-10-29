package shop.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import shop.dto.DeliveryDTO;
import shop.dto.OrderDTO;
import shop.dto.OrderItemDTO;
import shop.dto.PageDTO;
import shop.dto.PaymentDTO;
import shop.util.DBUtil;
import shop.util.Paging;

/*
  orders · order_item — 주문.

  주문은 지우지 않는다. 취소는 order_status = '취소' 로 남긴다 (CRUD.md § 4).
  order_item 은 주문 시점 스냅샷이라 UPDATE · DELETE 가 없다.
*/
public class OrderDAO {

	private static final String SELECT_BASE =
		"SELECT order_id, order_no, member_id, total_price, delivery_fee, order_status,"
	  + "       receiver, receiver_phone, zipcode, addr1, addr2, delivery_memo,"
	  + "       ordered_at, canceled_at"
	  + "  FROM orders";

	/* ---------------- 주문 생성 (결제 트랜잭션) ---------------- */

	/*
	  주문 INSERT 후 order_no 를 채운다.

	  order_no 는 order_id 가 있어야 만들 수 있는데 NOT NULL UNIQUE 라 비워 둘 수 없다.
	  그래서 커넥션별로 겹치지 않는 임시값을 넣고 바로 덮어쓴다 (ERD § 2.8 (a) 안).
	  같은 트랜잭션 안이라 중간 상태가 밖으로 보이지 않는다.
	*/
	public int insertOrder(Connection conn, OrderDTO order) throws SQLException {
		String insert = "INSERT INTO orders"
				+ " (order_no, member_id, total_price, delivery_fee, order_status,"
				+ "  receiver, receiver_phone, zipcode, addr1, addr2, delivery_memo)"
				+ " VALUES (CONCAT('T', LPAD(CONNECTION_ID(), 10, '0')), ?, ?, ?, '정상', ?, ?, ?, ?, ?, ?)";
		String fill = "UPDATE orders"
				+ "    SET order_no = CONCAT(DATE_FORMAT(ordered_at, '%Y%m%d'), '-', LPAD(order_id, 4, '0'))"
				+ "  WHERE order_id = ?";

		PreparedStatement pstmt = null;
		ResultSet rs = null;
		int orderId = 0;
		try {
			pstmt = conn.prepareStatement(insert, Statement.RETURN_GENERATED_KEYS);
			pstmt.setInt(1, order.getMemberId());
			pstmt.setInt(2, order.getTotalPrice());
			pstmt.setInt(3, order.getDeliveryFee());
			pstmt.setString(4, order.getReceiver());
			pstmt.setString(5, order.getReceiverPhone());
			pstmt.setString(6, order.getZipcode());
			pstmt.setString(7, order.getAddr1());
			pstmt.setString(8, order.getAddr2());
			pstmt.setString(9, order.getDeliveryMemo());
			pstmt.executeUpdate();
			rs = pstmt.getGeneratedKeys();
			if (rs.next()) {
				orderId = rs.getInt(1);
			}
		} finally {
			DBUtil.close(rs);
			DBUtil.close(pstmt);
		}

		if (orderId == 0) {
			return 0;
		}
		try {
			pstmt = conn.prepareStatement(fill);
			pstmt.setInt(1, orderId);
			pstmt.executeUpdate();
		} finally {
			DBUtil.close(pstmt);
		}
		return orderId;
	}

	public int insertItem(Connection conn, int orderId, int productId, String productName,
			int price, int quantity) throws SQLException {
		String sql = "INSERT INTO order_item (order_id, product_id, product_name, price, quantity)"
				   + " VALUES (?, ?, ?, ?, ?)";
		PreparedStatement pstmt = null;
		try {
			pstmt = conn.prepareStatement(sql);
			pstmt.setInt(1, orderId);
			pstmt.setInt(2, productId);
			pstmt.setString(3, productName);
			pstmt.setInt(4, price);
			pstmt.setInt(5, quantity);
			return pstmt.executeUpdate();
		} finally {
			DBUtil.close(pstmt);
		}
	}

	/* ---------------- 조회 ---------------- */

	public OrderDTO findById(int orderId) throws SQLException {
		String sql = SELECT_BASE + " WHERE order_id = ?";
		Connection conn = null;
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			conn = DBUtil.getConnection();
			pstmt = conn.prepareStatement(sql);
			pstmt.setInt(1, orderId);
			rs = pstmt.executeQuery();
			return rs.next() ? map(rs) : null;
		} finally {
			DBUtil.close(rs, pstmt, conn);
		}
	}

	/* 남의 주문을 URL 로 열지 못하게 member_id 를 조건에 넣는다. */
	public OrderDTO findByIdForMember(int orderId, int memberId) throws SQLException {
		String sql = SELECT_BASE + " WHERE order_id = ? AND member_id = ?";
		Connection conn = null;
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			conn = DBUtil.getConnection();
			pstmt = conn.prepareStatement(sql);
			pstmt.setInt(1, orderId);
			pstmt.setInt(2, memberId);
			rs = pstmt.executeQuery();
			return rs.next() ? map(rs) : null;
		} finally {
			DBUtil.close(rs, pstmt, conn);
		}
	}

	public PageDTO<OrderDTO> findPageByMember(int memberId, int page, int size) throws SQLException {
		int total = 0;
		Connection conn = null;
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		List<OrderDTO> list = new ArrayList<OrderDTO>();
		try {
			conn = DBUtil.getConnection();

			pstmt = conn.prepareStatement("SELECT COUNT(*) FROM orders WHERE member_id = ?");
			pstmt.setInt(1, memberId);
			rs = pstmt.executeQuery();
			if (rs.next()) {
				total = rs.getInt(1);
			}
			DBUtil.close(rs);
			DBUtil.close(pstmt);

			pstmt = conn.prepareStatement(SELECT_BASE
					+ " WHERE member_id = ? ORDER BY ordered_at DESC, order_id DESC LIMIT ? OFFSET ?");
			pstmt.setInt(1, memberId);
			pstmt.setInt(2, Paging.normalizeSize(size));
			pstmt.setInt(3, Paging.offset(page, size));
			rs = pstmt.executeQuery();
			while (rs.next()) {
				list.add(map(rs));
			}
		} finally {
			DBUtil.close(rs, pstmt, conn);
		}
		return new PageDTO<OrderDTO>(list, page, size, total);
	}

	/*
	  주문 상품 목록.

	  review 를 LEFT JOIN 해서 리뷰 작성 여부를 함께 받는다 — 주문상세와 내 리뷰 화면이
	  "리뷰 쓰기 / 작성 완료" 를 이 값으로 분기한다 (CRUD.md § 6-3).
	*/
	public List<OrderItemDTO> findItems(int orderId) throws SQLException {
		String sql = "SELECT oi.item_id, oi.order_id, oi.product_id, oi.product_name,"
				   + "       oi.price, oi.quantity,"
				   + "       CASE WHEN r.review_id IS NULL THEN 0 ELSE 1 END AS review_written"
				   + "  FROM order_item oi"
				   + "  LEFT JOIN review r ON r.item_id = oi.item_id"
				   + " WHERE oi.order_id = ?"
				   + " ORDER BY oi.item_id";
		List<OrderItemDTO> list = new ArrayList<OrderItemDTO>();
		Connection conn = null;
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			conn = DBUtil.getConnection();
			pstmt = conn.prepareStatement(sql);
			pstmt.setInt(1, orderId);
			rs = pstmt.executeQuery();
			while (rs.next()) {
				OrderItemDTO dto = new OrderItemDTO();
				dto.setItemId(rs.getInt("item_id"));
				dto.setOrderId(rs.getInt("order_id"));
				dto.setProductId(rs.getInt("product_id"));
				dto.setProductName(rs.getString("product_name"));
				dto.setPrice(rs.getInt("price"));
				dto.setQuantity(rs.getInt("quantity"));
				dto.setReviewWritten(rs.getInt("review_written") == 1);
				list.add(dto);
			}
			return list;
		} finally {
			DBUtil.close(rs, pstmt, conn);
		}
	}

	/* 취소 시 재고를 되돌리려면 주문 상품을 트랜잭션 안에서 다시 읽어야 한다. */
	public List<OrderItemDTO> findItems(Connection conn, int orderId) throws SQLException {
		String sql = "SELECT item_id, order_id, product_id, product_name, price, quantity"
				   + "  FROM order_item WHERE order_id = ? ORDER BY item_id";
		List<OrderItemDTO> list = new ArrayList<OrderItemDTO>();
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			pstmt = conn.prepareStatement(sql);
			pstmt.setInt(1, orderId);
			rs = pstmt.executeQuery();
			while (rs.next()) {
				OrderItemDTO dto = new OrderItemDTO();
				dto.setItemId(rs.getInt("item_id"));
				dto.setOrderId(rs.getInt("order_id"));
				dto.setProductId(rs.getInt("product_id"));
				dto.setProductName(rs.getString("product_name"));
				dto.setPrice(rs.getInt("price"));
				dto.setQuantity(rs.getInt("quantity"));
				list.add(dto);
			}
			return list;
		} finally {
			DBUtil.close(rs);
			DBUtil.close(pstmt);
		}
	}

	/* ---------------- 취소 ---------------- */

	/*
	  주문 취소 (ERD § 6.3-1).

	  WHERE order_status = '정상' 가드가 중복 취소를 막는다. 영향 행이 0 이면
	  이미 취소된 주문이므로 호출부가 롤백한다.
	  canceled_at 을 함께 넣지 않으면 chk_orders_canceled CHECK 에 걸린다.
	*/
	public int cancel(Connection conn, int orderId) throws SQLException {
		String sql = "UPDATE orders SET order_status = '취소', canceled_at = NOW()"
				   + " WHERE order_id = ? AND order_status = '정상'";
		PreparedStatement pstmt = null;
		try {
			pstmt = conn.prepareStatement(sql);
			pstmt.setInt(1, orderId);
			return pstmt.executeUpdate();
		} finally {
			DBUtil.close(pstmt);
		}
	}

	/* /dev 강제 복구 — 취소를 되돌린다. canceled_at 도 함께 비워야 CHECK 를 통과한다. */
	public int restore(Connection conn, int orderId) throws SQLException {
		String sql = "UPDATE orders SET order_status = '정상', canceled_at = NULL"
				   + " WHERE order_id = ?";
		PreparedStatement pstmt = null;
		try {
			pstmt = conn.prepareStatement(sql);
			pstmt.setInt(1, orderId);
			return pstmt.executeUpdate();
		} finally {
			DBUtil.close(pstmt);
		}
	}

	/* ---------------- 관리자 목록 (v_admin_order) ---------------- */

	public PageDTO<OrderDTO> findAdminPage(String keyword, String deliveryStatus,
			String orderStatus, int page, int size) throws SQLException {
		StringBuilder where = new StringBuilder(" WHERE 1 = 1");
		List<Object> args = new ArrayList<Object>();
		if (keyword != null && keyword.length() > 0) {
			where.append(" AND (order_no LIKE ? OR login_id LIKE ? OR receiver LIKE ?)");
			String like = "%" + keyword + "%";
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

		int total = 0;
		List<OrderDTO> list = new ArrayList<OrderDTO>();
		Connection conn = null;
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			conn = DBUtil.getConnection();

			pstmt = conn.prepareStatement("SELECT COUNT(*) FROM v_admin_order" + where);
			bind(pstmt, args);
			rs = pstmt.executeQuery();
			if (rs.next()) {
				total = rs.getInt(1);
			}
			DBUtil.close(rs);
			DBUtil.close(pstmt);

			pstmt = conn.prepareStatement("SELECT * FROM v_admin_order" + where
					+ " ORDER BY ordered_at DESC, order_id DESC LIMIT ? OFFSET ?");
			int i = bind(pstmt, args);
			pstmt.setInt(i++, Paging.normalizeSize(size));
			pstmt.setInt(i, Paging.offset(page, size));
			rs = pstmt.executeQuery();
			while (rs.next()) {
				list.add(mapAdminRow(rs));
			}
		} finally {
			DBUtil.close(rs, pstmt, conn);
		}
		return new PageDTO<OrderDTO>(list, page, size, total);
	}

	/* ---------------- 공통 ---------------- */

	private int bind(PreparedStatement pstmt, List<Object> args) throws SQLException {
		int i = 1;
		for (Object arg : args) {
			pstmt.setObject(i++, arg);
		}
		return i;
	}

	private OrderDTO map(ResultSet rs) throws SQLException {
		OrderDTO dto = new OrderDTO();
		dto.setOrderId(rs.getInt("order_id"));
		dto.setOrderNo(rs.getString("order_no"));
		dto.setMemberId(rs.getInt("member_id"));
		dto.setTotalPrice(rs.getInt("total_price"));
		dto.setDeliveryFee(rs.getInt("delivery_fee"));
		dto.setOrderStatus(rs.getString("order_status"));
		dto.setReceiver(rs.getString("receiver"));
		dto.setReceiverPhone(rs.getString("receiver_phone"));
		dto.setZipcode(rs.getString("zipcode"));
		dto.setAddr1(rs.getString("addr1"));
		dto.setAddr2(rs.getString("addr2"));
		dto.setDeliveryMemo(rs.getString("delivery_memo"));
		dto.setOrderedAt(rs.getTimestamp("ordered_at"));
		dto.setCanceledAt(rs.getTimestamp("canceled_at"));
		return dto;
	}

	/*
	  관리자 목록 한 줄 — payment · delivery 를 중첩해 채운다.

	  뷰가 LEFT JOIN 이라 결제 · 배송이 비어 있을 수 있다. /dev 가 데이터를 비틀어
	  반쪽 주문을 만든 경우인데, 그때도 행을 감추지 않고 보여줘야 이상을 알아챈다
	  (schema.sql v_admin_order 주석).
	*/
	private OrderDTO mapAdminRow(ResultSet rs) throws SQLException {
		OrderDTO dto = map(rs);
		dto.setLoginId(rs.getString("login_id"));
		dto.setMemberName(rs.getString("member_name"));
		dto.setItemCount(rs.getInt("item_count"));
		dto.setFirstProductName(rs.getString("first_product_name"));

		int paymentId = rs.getInt("payment_id");
		if (!rs.wasNull()) {
			PaymentDTO pay = new PaymentDTO();
			pay.setPaymentId(paymentId);
			pay.setOrderId(dto.getOrderId());
			pay.setMethod(rs.getString("pay_method"));
			pay.setPayStatus(rs.getString("pay_status"));
			pay.setAmount(dto.getPayAmount());
			pay.setPaidAt(rs.getTimestamp("paid_at"));
			pay.setRefundedAt(rs.getTimestamp("refunded_at"));
			dto.setPayment(pay);
		}

		int deliveryId = rs.getInt("delivery_id");
		if (!rs.wasNull()) {
			DeliveryDTO dlv = new DeliveryDTO();
			dlv.setDeliveryId(deliveryId);
			dlv.setOrderId(dto.getOrderId());
			dlv.setStatus(rs.getString("delivery_status"));
			dlv.setCourier(rs.getString("courier"));
			dlv.setTrackingNo(rs.getString("tracking_no"));
			dlv.setReadyAt(rs.getTimestamp("ready_at"));
			dlv.setShippedAt(rs.getTimestamp("shipped_at"));
			dlv.setDeliveredAt(rs.getTimestamp("delivered_at"));
			dto.setDelivery(dlv);
		}
		return dto;
	}
}
