package shop.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import shop.dto.CartItemDTO;
import shop.util.DBUtil;

/*
  cart — 장바구니.

  ★ 노출 조건을 붙이지 않는다 (CRUD.md § 2 주의).
    사용자가 담아 둔 자기 기록이므로 숨김 · 품절이어도 보여주고,
    CartItemDTO.isOrderable() 로 주문 대상에서만 뺀다.
*/
public class CartDAO {

	private static final String SELECT_BASE =
		"SELECT ct.cart_id, ct.member_id, ct.product_id, ct.quantity,"
	  + "       p.name AS product_name, p.thumbnail, p.status, p.price, p.stock"
	  + "  FROM cart ct"
	  + "  JOIN product p ON p.product_id = ct.product_id";

	public List<CartItemDTO> findByMember(int memberId) throws SQLException {
		String sql = SELECT_BASE + " WHERE ct.member_id = ? ORDER BY ct.cart_id DESC";
		List<CartItemDTO> list = new ArrayList<CartItemDTO>();
		Connection conn = null;
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			conn = DBUtil.getConnection();
			pstmt = conn.prepareStatement(sql);
			pstmt.setInt(1, memberId);
			rs = pstmt.executeQuery();
			while (rs.next()) {
				list.add(map(rs));
			}
			return list;
		} finally {
			DBUtil.close(rs, pstmt, conn);
		}
	}

	/* 결제 트랜잭션 안에서 다시 읽는다 — 커넥션을 공유해야 같은 트랜잭션에서 본다. */
	public List<CartItemDTO> findByMember(Connection conn, int memberId) throws SQLException {
		String sql = SELECT_BASE + " WHERE ct.member_id = ? ORDER BY ct.cart_id";
		List<CartItemDTO> list = new ArrayList<CartItemDTO>();
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			pstmt = conn.prepareStatement(sql);
			pstmt.setInt(1, memberId);
			rs = pstmt.executeQuery();
			while (rs.next()) {
				list.add(map(rs));
			}
			return list;
		} finally {
			DBUtil.close(rs);
			DBUtil.close(pstmt);
		}
	}

	/* 헤더 수량 배지 — 줄 수가 아니라 담긴 개수의 합이다. */
	public int countQuantity(int memberId) throws SQLException {
		String sql = "SELECT IFNULL(SUM(quantity), 0) FROM cart WHERE member_id = ?";
		Connection conn = null;
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			conn = DBUtil.getConnection();
			pstmt = conn.prepareStatement(sql);
			pstmt.setInt(1, memberId);
			rs = pstmt.executeQuery();
			return rs.next() ? rs.getInt(1) : 0;
		} finally {
			DBUtil.close(rs, pstmt, conn);
		}
	}

	/*
	  담기 — UNIQUE (member_id, product_id) 덕분에 한 문장으로 끝난다.
	  이미 있으면 수량을 더한다. (schema.sql cart 주석)
	*/
	public int add(int memberId, int productId, int quantity) throws SQLException {
		String sql = "INSERT INTO cart (member_id, product_id, quantity) VALUES (?, ?, ?)"
				   + " ON DUPLICATE KEY UPDATE quantity = quantity + VALUES(quantity)";
		Connection conn = null;
		PreparedStatement pstmt = null;
		try {
			conn = DBUtil.getConnection();
			pstmt = conn.prepareStatement(sql);
			pstmt.setInt(1, memberId);
			pstmt.setInt(2, productId);
			pstmt.setInt(3, quantity);
			return pstmt.executeUpdate();
		} finally {
			DBUtil.close(pstmt, conn);
		}
	}

	public int updateQuantity(int cartId, int memberId, int quantity) throws SQLException {
		String sql = "UPDATE cart SET quantity = ? WHERE cart_id = ? AND member_id = ?";
		Connection conn = null;
		PreparedStatement pstmt = null;
		try {
			conn = DBUtil.getConnection();
			pstmt = conn.prepareStatement(sql);
			pstmt.setInt(1, quantity);
			pstmt.setInt(2, cartId);
			pstmt.setInt(3, memberId);
			return pstmt.executeUpdate();
		} finally {
			DBUtil.close(pstmt, conn);
		}
	}

	public int delete(int cartId, int memberId) throws SQLException {
		String sql = "DELETE FROM cart WHERE cart_id = ? AND member_id = ?";
		Connection conn = null;
		PreparedStatement pstmt = null;
		try {
			conn = DBUtil.getConnection();
			pstmt = conn.prepareStatement(sql);
			pstmt.setInt(1, cartId);
			pstmt.setInt(2, memberId);
			return pstmt.executeUpdate();
		} finally {
			DBUtil.close(pstmt, conn);
		}
	}

	/* 주문 후 비우기 — 결제 트랜잭션의 마지막 단계 (ERD § 6.1-7) */
	public int clear(Connection conn, int memberId) throws SQLException {
		String sql = "DELETE FROM cart WHERE member_id = ?";
		PreparedStatement pstmt = null;
		try {
			pstmt = conn.prepareStatement(sql);
			pstmt.setInt(1, memberId);
			return pstmt.executeUpdate();
		} finally {
			DBUtil.close(pstmt);
		}
	}

	private CartItemDTO map(ResultSet rs) throws SQLException {
		CartItemDTO dto = new CartItemDTO();
		dto.setCartId(rs.getInt("cart_id"));
		dto.setMemberId(rs.getInt("member_id"));
		dto.setProductId(rs.getInt("product_id"));
		dto.setProductName(rs.getString("product_name"));
		dto.setThumbnail(rs.getString("thumbnail"));
		dto.setStatus(rs.getString("status"));
		dto.setPrice(rs.getInt("price"));
		dto.setStock(rs.getInt("stock"));
		dto.setQuantity(rs.getInt("quantity"));
		return dto;
	}
}
