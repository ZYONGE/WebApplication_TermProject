package shop.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import shop.dto.WishItemDTO;
import shop.util.DBUtil;

/*
  wishlist — 찜.

  토글 구조라 UPDATE 가 없다 (CRUD.md § 4). 행의 존재 자체가 값이다.
  장바구니와 같은 이유로 노출 조건을 붙이지 않는다.
*/
public class WishlistDAO {

	public List<WishItemDTO> findByMember(int memberId) throws SQLException {
		String sql = "SELECT w.member_id, w.product_id, w.created_at,"
				   + "       p.name AS product_name, p.thumbnail, p.status, p.price"
				   + "  FROM wishlist w"
				   + "  JOIN product p ON p.product_id = w.product_id"
				   + " WHERE w.member_id = ?"
				   + " ORDER BY w.created_at DESC";
		List<WishItemDTO> list = new ArrayList<WishItemDTO>();
		Connection conn = null;
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			conn = DBUtil.getConnection();
			pstmt = conn.prepareStatement(sql);
			pstmt.setInt(1, memberId);
			rs = pstmt.executeQuery();
			while (rs.next()) {
				WishItemDTO dto = new WishItemDTO();
				dto.setMemberId(rs.getInt("member_id"));
				dto.setProductId(rs.getInt("product_id"));
				dto.setProductName(rs.getString("product_name"));
				dto.setThumbnail(rs.getString("thumbnail"));
				dto.setStatus(rs.getString("status"));
				dto.setPrice(rs.getInt("price"));
				dto.setCreatedAt(rs.getTimestamp("created_at"));
				list.add(dto);
			}
			return list;
		} finally {
			DBUtil.close(rs, pstmt, conn);
		}
	}

	public boolean exists(int memberId, int productId) throws SQLException {
		String sql = "SELECT 1 FROM wishlist WHERE member_id = ? AND product_id = ?";
		Connection conn = null;
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			conn = DBUtil.getConnection();
			pstmt = conn.prepareStatement(sql);
			pstmt.setInt(1, memberId);
			pstmt.setInt(2, productId);
			rs = pstmt.executeQuery();
			return rs.next();
		} finally {
			DBUtil.close(rs, pstmt, conn);
		}
	}

	public int count(int memberId) throws SQLException {
		String sql = "SELECT COUNT(*) FROM wishlist WHERE member_id = ?";
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

	public int insert(int memberId, int productId) throws SQLException {
		/* 동시 클릭으로 같은 행이 두 번 들어오는 것을 PK 가 막는다. */
		String sql = "INSERT IGNORE INTO wishlist (member_id, product_id) VALUES (?, ?)";
		return execute(sql, memberId, productId);
	}

	public int delete(int memberId, int productId) throws SQLException {
		String sql = "DELETE FROM wishlist WHERE member_id = ? AND product_id = ?";
		return execute(sql, memberId, productId);
	}

	private int execute(String sql, int memberId, int productId) throws SQLException {
		Connection conn = null;
		PreparedStatement pstmt = null;
		try {
			conn = DBUtil.getConnection();
			pstmt = conn.prepareStatement(sql);
			pstmt.setInt(1, memberId);
			pstmt.setInt(2, productId);
			return pstmt.executeUpdate();
		} finally {
			DBUtil.close(pstmt, conn);
		}
	}
}
