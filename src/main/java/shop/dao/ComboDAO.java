package shop.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import shop.dto.ComboDTO;
import shop.dto.ComboItemDTO;
import shop.util.DBUtil;

/*
  combo · combo_item — 저장한 조합 (내 조합) (ERD § 3 · § 7.1).

  ★ 조회 · 삭제는 언제나 member_id 를 함께 건다. 남의 조합 번호를 넣어도 열리지 않는다.
  ★ 가격은 저장하지 않는다. 구성 조회가 product 를 조인해 현재 가격 · 재고 · 상태를 준다.
  ★ 구성은 저장할 때마다 전부 지우고 다시 넣는다 — 한 트랜잭션 (ComboService.save).
*/
public class ComboDAO {

	private static final String SELECT_BASE =
		"SELECT cb.combo_id, cb.member_id, cb.name, cb.days, cb.created_at, cb.updated_at,"
	  + "       (SELECT COUNT(*) FROM combo_item ci WHERE ci.combo_id = cb.combo_id) AS item_count"
	  + "  FROM combo cb";

	public List<ComboDTO> findByMember(int memberId) throws SQLException {
		String sql = SELECT_BASE + " WHERE cb.member_id = ? ORDER BY cb.updated_at DESC, cb.combo_id DESC";
		List<ComboDTO> list = new ArrayList<ComboDTO>();
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

	/* 불러오기 — combo_id 와 member_id 를 함께 조건으로 건다 (ERD § 7.1 조합 불러오기). */
	public ComboDTO findByIdForMember(int comboId, int memberId) throws SQLException {
		String sql = SELECT_BASE + " WHERE cb.combo_id = ? AND cb.member_id = ?";
		Connection conn = null;
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			conn = DBUtil.getConnection();
			pstmt = conn.prepareStatement(sql);
			pstmt.setInt(1, comboId);
			pstmt.setInt(2, memberId);
			rs = pstmt.executeQuery();
			return rs.next() ? map(rs) : null;
		} finally {
			DBUtil.close(rs, pstmt, conn);
		}
	}

	/* 같은 회원 안의 이름 — UNIQUE (member_id, name). 없으면 0 */
	public int findIdByName(Connection conn, int memberId, String name) throws SQLException {
		String sql = "SELECT combo_id FROM combo WHERE member_id = ? AND name = ?";
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			pstmt = conn.prepareStatement(sql);
			pstmt.setInt(1, memberId);
			pstmt.setString(2, name);
			rs = pstmt.executeQuery();
			return rs.next() ? rs.getInt(1) : 0;
		} finally {
			DBUtil.close(rs);
			DBUtil.close(pstmt);
		}
	}

	/* 구성 — 현재 가격 · 재고 · 상태를 조인한다. 숨김 · 재고 0 이어도 행은 남는다. */
	public List<ComboItemDTO> findItems(int comboId) throws SQLException {
		String sql = "SELECT ci.combo_id, ci.product_id, ci.sort_order,"
				   + "       p.name AS product_name, p.thumbnail, p.status, p.price, p.stock"
				   + "  FROM combo_item ci"
				   + "  JOIN product p ON p.product_id = ci.product_id"
				   + " WHERE ci.combo_id = ?"
				   + " ORDER BY ci.sort_order, ci.product_id";
		List<ComboItemDTO> list = new ArrayList<ComboItemDTO>();
		Connection conn = null;
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			conn = DBUtil.getConnection();
			pstmt = conn.prepareStatement(sql);
			pstmt.setInt(1, comboId);
			rs = pstmt.executeQuery();
			while (rs.next()) {
				ComboItemDTO dto = new ComboItemDTO();
				dto.setComboId(rs.getInt("combo_id"));
				dto.setProductId(rs.getInt("product_id"));
				dto.setSortOrder(rs.getInt("sort_order"));
				dto.setProductName(rs.getString("product_name"));
				dto.setThumbnail(rs.getString("thumbnail"));
				dto.setStatus(rs.getString("status"));
				dto.setPrice(rs.getInt("price"));
				dto.setStock(rs.getInt("stock"));
				list.add(dto);
			}
			return list;
		} finally {
			DBUtil.close(rs, pstmt, conn);
		}
	}

	public int insert(Connection conn, int memberId, String name, int days) throws SQLException {
		String sql = "INSERT INTO combo (member_id, name, days) VALUES (?, ?, ?)";
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
			pstmt.setInt(1, memberId);
			pstmt.setString(2, name);
			pstmt.setInt(3, days);
			pstmt.executeUpdate();
			rs = pstmt.getGeneratedKeys();
			return rs.next() ? rs.getInt(1) : 0;
		} finally {
			DBUtil.close(rs);
			DBUtil.close(pstmt);
		}
	}

	/*
	  덮어쓰기 — 일수만 바꾸고 구성은 다시 넣는다.
	  구성만 바뀌고 days 가 같으면 ON UPDATE 가 동작하지 않으므로 updated_at 을 직접 채운다.
	*/
	public int update(Connection conn, int comboId, int memberId, int days) throws SQLException {
		String sql = "UPDATE combo SET days = ?, updated_at = CURRENT_TIMESTAMP"
				   + " WHERE combo_id = ? AND member_id = ?";
		PreparedStatement pstmt = null;
		try {
			pstmt = conn.prepareStatement(sql);
			pstmt.setInt(1, days);
			pstmt.setInt(2, comboId);
			pstmt.setInt(3, memberId);
			return pstmt.executeUpdate();
		} finally {
			DBUtil.close(pstmt);
		}
	}

	public int deleteItems(Connection conn, int comboId) throws SQLException {
		String sql = "DELETE FROM combo_item WHERE combo_id = ?";
		PreparedStatement pstmt = null;
		try {
			pstmt = conn.prepareStatement(sql);
			pstmt.setInt(1, comboId);
			return pstmt.executeUpdate();
		} finally {
			DBUtil.close(pstmt);
		}
	}

	public int insertItem(Connection conn, int comboId, int productId, int sortOrder) throws SQLException {
		String sql = "INSERT INTO combo_item (combo_id, product_id, sort_order) VALUES (?, ?, ?)";
		PreparedStatement pstmt = null;
		try {
			pstmt = conn.prepareStatement(sql);
			pstmt.setInt(1, comboId);
			pstmt.setInt(2, productId);
			pstmt.setInt(3, sortOrder);
			return pstmt.executeUpdate();
		} finally {
			DBUtil.close(pstmt);
		}
	}

	/* combo_item 은 CASCADE 로 함께 지워진다. */
	public int delete(int comboId, int memberId) throws SQLException {
		String sql = "DELETE FROM combo WHERE combo_id = ? AND member_id = ?";
		Connection conn = null;
		PreparedStatement pstmt = null;
		try {
			conn = DBUtil.getConnection();
			pstmt = conn.prepareStatement(sql);
			pstmt.setInt(1, comboId);
			pstmt.setInt(2, memberId);
			return pstmt.executeUpdate();
		} finally {
			DBUtil.close(pstmt, conn);
		}
	}

	private ComboDTO map(ResultSet rs) throws SQLException {
		ComboDTO dto = new ComboDTO();
		dto.setComboId(rs.getInt("combo_id"));
		dto.setMemberId(rs.getInt("member_id"));
		dto.setName(rs.getString("name"));
		dto.setDays(rs.getInt("days"));
		dto.setCreatedAt(rs.getTimestamp("created_at"));
		dto.setUpdatedAt(rs.getTimestamp("updated_at"));
		dto.setItemCount(rs.getInt("item_count"));
		return dto;
	}
}
