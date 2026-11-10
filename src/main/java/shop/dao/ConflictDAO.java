package shop.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import shop.dto.ConflictDTO;
import shop.util.DBUtil;

/*
  supplement_conflict — 동시 섭취 금지 조합 (ERD § 3 · § 7.1).

  ★ 쌍은 product_a < product_b 로 저장한다 (CHECK chk_conflict_order).
    정렬은 이 DAO 의 insert 가 한 번 더 지킨다 — 호출부가 순서를 잊어도 CHECK 에 걸리지 않게.
  ★ 두 상품은 SINGLE 이어야 한다. DB 가 못 막으므로 AdminService 가 지킨다 (ERD § 8-10).
*/
public class ConflictDAO {

	private static final String SELECT_BASE =
		"SELECT sc.conflict_id, sc.product_a, sc.product_b, sc.message, sc.created_at,"
	  + "       pa.name AS product_a_name, pb.name AS product_b_name"
	  + "  FROM supplement_conflict sc"
	  + "  JOIN product pa ON pa.product_id = sc.product_a"
	  + "  JOIN product pb ON pb.product_id = sc.product_b";

	/* 전체 — 관리자 39번 화면 · 조합 탭이 화면에 심어 두는 경고 데이터 */
	public List<ConflictDTO> findAll() throws SQLException {
		return query(SELECT_BASE + " ORDER BY sc.product_a, sc.product_b", null, false);
	}

	/* 묶음 안의 쌍만 — 두 상품이 모두 ids 안에 있는 행 (경고 계산) */
	public List<ConflictDTO> findAmong(Collection<Integer> ids) throws SQLException {
		if (ids == null || ids.size() < 2) {
			return new ArrayList<ConflictDTO>();
		}
		String in = ProductDAO.placeholders(ids.size());
		String sql = SELECT_BASE + " WHERE sc.product_a IN (" + in + ") AND sc.product_b IN (" + in + ")"
				   + " ORDER BY sc.product_a, sc.product_b";
		return query(sql, ids, true);
	}

	/* 한쪽이라도 ids 에 걸린 행 — 상품상세의 "함께 드시지 마세요" (트랙이면 구성품 전체) */
	public List<ConflictDTO> findInvolving(Collection<Integer> ids) throws SQLException {
		if (ids == null || ids.isEmpty()) {
			return new ArrayList<ConflictDTO>();
		}
		String in = ProductDAO.placeholders(ids.size());
		String sql = SELECT_BASE + " WHERE sc.product_a IN (" + in + ") OR sc.product_b IN (" + in + ")"
				   + " ORDER BY sc.product_a, sc.product_b";
		return query(sql, ids, true);
	}

	public ConflictDTO findById(int conflictId) throws SQLException {
		String sql = SELECT_BASE + " WHERE sc.conflict_id = ?";
		Connection conn = null;
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			conn = DBUtil.getConnection();
			pstmt = conn.prepareStatement(sql);
			pstmt.setInt(1, conflictId);
			rs = pstmt.executeQuery();
			return rs.next() ? map(rs) : null;
		} finally {
			DBUtil.close(rs, pstmt, conn);
		}
	}

	/* 같은 쌍이 이미 있는가 — UNIQUE 위반 전에 화면 문구로 돌려준다. */
	public boolean existsPair(int x, int y) throws SQLException {
		String sql = "SELECT 1 FROM supplement_conflict WHERE product_a = ? AND product_b = ?";
		Connection conn = null;
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			conn = DBUtil.getConnection();
			pstmt = conn.prepareStatement(sql);
			pstmt.setInt(1, Math.min(x, y));
			pstmt.setInt(2, Math.max(x, y));
			rs = pstmt.executeQuery();
			return rs.next();
		} finally {
			DBUtil.close(rs, pstmt, conn);
		}
	}

	public int insert(Connection conn, int x, int y, String message) throws SQLException {
		String sql = "INSERT INTO supplement_conflict (product_a, product_b, message) VALUES (?, ?, ?)";
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
			pstmt.setInt(1, Math.min(x, y));
			pstmt.setInt(2, Math.max(x, y));
			pstmt.setString(3, message);
			pstmt.executeUpdate();
			rs = pstmt.getGeneratedKeys();
			return rs.next() ? rs.getInt(1) : 0;
		} finally {
			DBUtil.close(rs);
			DBUtil.close(pstmt);
		}
	}

	public int updateMessage(Connection conn, int conflictId, String message) throws SQLException {
		String sql = "UPDATE supplement_conflict SET message = ? WHERE conflict_id = ?";
		PreparedStatement pstmt = null;
		try {
			pstmt = conn.prepareStatement(sql);
			pstmt.setString(1, message);
			pstmt.setInt(2, conflictId);
			return pstmt.executeUpdate();
		} finally {
			DBUtil.close(pstmt);
		}
	}

	public int delete(Connection conn, int conflictId) throws SQLException {
		String sql = "DELETE FROM supplement_conflict WHERE conflict_id = ?";
		PreparedStatement pstmt = null;
		try {
			pstmt = conn.prepareStatement(sql);
			pstmt.setInt(1, conflictId);
			return pstmt.executeUpdate();
		} finally {
			DBUtil.close(pstmt);
		}
	}

	/* ids 를 두 번 바인딩해야 하는 IN 절(twice=true)과 바인딩 없는 조회를 함께 처리한다. */
	private List<ConflictDTO> query(String sql, Collection<Integer> ids, boolean twice)
			throws SQLException {
		List<ConflictDTO> list = new ArrayList<ConflictDTO>();
		Connection conn = null;
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			conn = DBUtil.getConnection();
			pstmt = conn.prepareStatement(sql);
			if (ids != null) {
				int i = 1;
				for (int round = 0; round < (twice ? 2 : 1); round++) {
					for (Integer id : ids) {
						pstmt.setInt(i++, id.intValue());
					}
				}
			}
			rs = pstmt.executeQuery();
			while (rs.next()) {
				list.add(map(rs));
			}
			return list;
		} finally {
			DBUtil.close(rs, pstmt, conn);
		}
	}

	private ConflictDTO map(ResultSet rs) throws SQLException {
		ConflictDTO dto = new ConflictDTO();
		dto.setConflictId(rs.getInt("conflict_id"));
		dto.setProductA(rs.getInt("product_a"));
		dto.setProductB(rs.getInt("product_b"));
		dto.setProductAName(rs.getString("product_a_name"));
		dto.setProductBName(rs.getString("product_b_name"));
		dto.setMessage(rs.getString("message"));
		dto.setCreatedAt(rs.getTimestamp("created_at"));
		return dto;
	}
}
