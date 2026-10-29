package shop.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import shop.dto.PaymentDTO;
import shop.util.DBUtil;

/*
  payment — 모의 결제.

  외부 PG 연동은 범위 외다 (CLAUDE.md § 5). 선택한 수단을 저장하고 상태만 전이한다.
  결제 이력은 지우지 않는다 — DELETE 가 없다 (CRUD.md § 4).
*/
public class PaymentDAO {

	private static final String COLUMNS =
		"payment_id, order_id, method, amount, pay_status, paid_at, refunded_at";

	public int insert(Connection conn, int orderId, String method, int amount)
			throws SQLException {
		String sql = "INSERT INTO payment (order_id, method, amount, pay_status)"
				   + " VALUES (?, ?, ?, '완료')";
		PreparedStatement pstmt = null;
		try {
			pstmt = conn.prepareStatement(sql);
			pstmt.setInt(1, orderId);
			pstmt.setString(2, method);
			pstmt.setInt(3, amount);
			return pstmt.executeUpdate();
		} finally {
			DBUtil.close(pstmt);
		}
	}

	public PaymentDTO findByOrder(int orderId) throws SQLException {
		String sql = "SELECT " + COLUMNS + " FROM payment WHERE order_id = ?";
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

	/*
	  환불 처리 (ERD § 6.3-2).

	  refunded_at 을 함께 채워야 chk_payment_refunded CHECK 를 통과한다.
	  WHERE pay_status = '완료' 가드로 중복 환불을 막는다.
	*/
	public int refund(Connection conn, int orderId) throws SQLException {
		String sql = "UPDATE payment SET pay_status = '환불', refunded_at = NOW()"
				   + " WHERE order_id = ? AND pay_status = '완료'";
		PreparedStatement pstmt = null;
		try {
			pstmt = conn.prepareStatement(sql);
			pstmt.setInt(1, orderId);
			return pstmt.executeUpdate();
		} finally {
			DBUtil.close(pstmt);
		}
	}

	/* /dev 강제 복구 — 환불을 되돌린다. refunded_at 도 함께 비워야 CHECK 를 통과한다. */
	public int restore(Connection conn, int orderId) throws SQLException {
		String sql = "UPDATE payment SET pay_status = '완료', refunded_at = NULL"
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

	private PaymentDTO map(ResultSet rs) throws SQLException {
		PaymentDTO dto = new PaymentDTO();
		dto.setPaymentId(rs.getInt("payment_id"));
		dto.setOrderId(rs.getInt("order_id"));
		dto.setMethod(rs.getString("method"));
		dto.setAmount(rs.getInt("amount"));
		dto.setPayStatus(rs.getString("pay_status"));
		dto.setPaidAt(rs.getTimestamp("paid_at"));
		dto.setRefundedAt(rs.getTimestamp("refunded_at"));
		return dto;
	}
}
