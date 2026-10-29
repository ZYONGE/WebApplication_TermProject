package shop.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import shop.dto.CategoryDTO;
import shop.util.DBUtil;

/*
  category — 카테고리.

  parent_id 자기 참조 1단 트리다. 네비게이션이 매 요청마다 전체를 읽으므로
  행 전체를 한 번에 받아 메모리에서 트리를 만든다. 재귀 조회를 하지 않는다.
*/
public class CategoryDAO {

	public List<CategoryDTO> findAll() throws SQLException {
		String sql = "SELECT category_id, parent_id, name, sort_order"
				   + "  FROM category ORDER BY sort_order, category_id";
		List<CategoryDTO> list = new ArrayList<CategoryDTO>();
		Connection conn = null;
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			conn = DBUtil.getConnection();
			pstmt = conn.prepareStatement(sql);
			rs = pstmt.executeQuery();
			while (rs.next()) {
				list.add(map(rs));
			}
			return list;
		} finally {
			DBUtil.close(rs, pstmt, conn);
		}
	}

	/* 대분류 목록에 children 을 채워 돌려준다 — 헤더 네비게이션이 그대로 쓴다. */
	public List<CategoryDTO> findTree() throws SQLException {
		List<CategoryDTO> all = findAll();
		Map<Integer, CategoryDTO> byId = new LinkedHashMap<Integer, CategoryDTO>();
		for (CategoryDTO c : all) {
			byId.put(Integer.valueOf(c.getCategoryId()), c);
		}
		List<CategoryDTO> roots = new ArrayList<CategoryDTO>();
		for (CategoryDTO c : all) {
			if (c.getParentId() == null) {
				roots.add(c);
			} else {
				CategoryDTO parent = byId.get(c.getParentId());
				if (parent != null) {
					parent.getChildren().add(c);
				}
			}
		}
		return roots;
	}

	public CategoryDTO findById(int categoryId) throws SQLException {
		String sql = "SELECT category_id, parent_id, name, sort_order"
				   + "  FROM category WHERE category_id = ?";
		Connection conn = null;
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			conn = DBUtil.getConnection();
			pstmt = conn.prepareStatement(sql);
			pstmt.setInt(1, categoryId);
			rs = pstmt.executeQuery();
			return rs.next() ? map(rs) : null;
		} finally {
			DBUtil.close(rs, pstmt, conn);
		}
	}

	/*
	  대분류 이름 중복 검사.

	  UNIQUE (parent_id, name) 은 parent_id 가 NULL 이면 걸리지 않는다 —
	  MySQL UNIQUE 는 NULL 을 중복으로 보지 않기 때문이다. 그 구멍을 여기서 막는다.
	  (schema.sql category 주석)
	*/
	public boolean existsRootName(String name, int exceptId) throws SQLException {
		String sql = "SELECT 1 FROM category"
				   + " WHERE parent_id IS NULL AND name = ? AND category_id <> ?";
		Connection conn = null;
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			conn = DBUtil.getConnection();
			pstmt = conn.prepareStatement(sql);
			pstmt.setString(1, name);
			pstmt.setInt(2, exceptId);
			rs = pstmt.executeQuery();
			return rs.next();
		} finally {
			DBUtil.close(rs, pstmt, conn);
		}
	}

	public int insert(CategoryDTO category) throws SQLException {
		String sql = "INSERT INTO category (parent_id, name, sort_order) VALUES (?, ?, ?)";
		Connection conn = null;
		PreparedStatement pstmt = null;
		try {
			conn = DBUtil.getConnection();
			pstmt = conn.prepareStatement(sql);
			setParentId(pstmt, 1, category.getParentId());
			pstmt.setString(2, category.getName());
			pstmt.setInt(3, category.getSortOrder());
			return pstmt.executeUpdate();
		} finally {
			DBUtil.close(pstmt, conn);
		}
	}

	public int update(CategoryDTO category) throws SQLException {
		String sql = "UPDATE category SET parent_id = ?, name = ?, sort_order = ?"
				   + " WHERE category_id = ?";
		Connection conn = null;
		PreparedStatement pstmt = null;
		try {
			conn = DBUtil.getConnection();
			pstmt = conn.prepareStatement(sql);
			setParentId(pstmt, 1, category.getParentId());
			pstmt.setString(2, category.getName());
			pstmt.setInt(3, category.getSortOrder());
			pstmt.setInt(4, category.getCategoryId());
			return pstmt.executeUpdate();
		} finally {
			DBUtil.close(pstmt, conn);
		}
	}

	/* FK 가 RESTRICT 라 상품이나 하위 카테고리가 남아 있으면 SQLException 이 난다. */
	public int delete(int categoryId) throws SQLException {
		String sql = "DELETE FROM category WHERE category_id = ?";
		Connection conn = null;
		PreparedStatement pstmt = null;
		try {
			conn = DBUtil.getConnection();
			pstmt = conn.prepareStatement(sql);
			pstmt.setInt(1, categoryId);
			return pstmt.executeUpdate();
		} finally {
			DBUtil.close(pstmt, conn);
		}
	}

	/* 삭제 가능 여부를 미리 보여주기 위한 카운트 */
	public int countChildren(int categoryId) throws SQLException {
		return countBy("SELECT COUNT(*) FROM category WHERE parent_id = ?", categoryId);
	}

	public int countProducts(int categoryId) throws SQLException {
		return countBy("SELECT COUNT(*) FROM product WHERE category_id = ?", categoryId);
	}

	private int countBy(String sql, int id) throws SQLException {
		Connection conn = null;
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			conn = DBUtil.getConnection();
			pstmt = conn.prepareStatement(sql);
			pstmt.setInt(1, id);
			rs = pstmt.executeQuery();
			return rs.next() ? rs.getInt(1) : 0;
		} finally {
			DBUtil.close(rs, pstmt, conn);
		}
	}

	private void setParentId(PreparedStatement pstmt, int index, Integer parentId)
			throws SQLException {
		if (parentId == null || parentId.intValue() == 0) {
			pstmt.setNull(index, java.sql.Types.INTEGER);
		} else {
			pstmt.setInt(index, parentId.intValue());
		}
	}

	private CategoryDTO map(ResultSet rs) throws SQLException {
		CategoryDTO dto = new CategoryDTO();
		dto.setCategoryId(rs.getInt("category_id"));
		int parentId = rs.getInt("parent_id");
		dto.setParentId(rs.wasNull() ? null : Integer.valueOf(parentId));
		dto.setName(rs.getString("name"));
		dto.setSortOrder(rs.getInt("sort_order"));
		return dto;
	}
}
