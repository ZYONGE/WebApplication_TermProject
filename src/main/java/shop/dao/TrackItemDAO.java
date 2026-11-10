package shop.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import shop.dto.ProductDTO;
import shop.util.DBUtil;

/*
  track_item — 트랙 구성 (ERD § 3 · § 7.1).

  트랙 1포 = 구성 낱개 1회분씩. 구성마다 수량 컬럼이 없다.
  ★ track_id 는 TRACK, item_id 는 SINGLE 이어야 한다. 행을 넘나드는 조건이라
    DB 가 못 막는다 — AdminService.saveTrackItems 가 지킨다 (ERD § 8-9).
  ★ 구성은 고칠 때 전부 지우고 다시 넣는다. 한 트랜잭션 + TRACK_ITEM_SET 로그.
*/
public class TrackItemDAO {

	private static final String SELECT_ITEMS =
		"SELECT ti.track_id, p.product_id, p.category_id, p.product_type, p.name, p.price, p.stock,"
	  + "       p.description, p.thumbnail, p.status, p.created_at"
	  + "  FROM track_item ti"
	  + "  JOIN product p ON p.product_id = ti.item_id";

	/* 한 트랙의 구성 낱개 — 상품상세 · 관리자 수정 팝업 */
	public List<ProductDTO> findItems(int trackId) throws SQLException {
		Connection conn = null;
		try {
			conn = DBUtil.getConnection();
			return findItems(conn, trackId);
		} finally {
			DBUtil.close(conn);
		}
	}

	/* 결제 트랜잭션 안에서 구성 스냅샷을 만들 때 — 커넥션을 공유한다. */
	public List<ProductDTO> findItems(Connection conn, int trackId) throws SQLException {
		String sql = SELECT_ITEMS + " WHERE ti.track_id = ? ORDER BY ti.sort_order, ti.item_id";
		List<ProductDTO> list = new ArrayList<ProductDTO>();
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			pstmt = conn.prepareStatement(sql);
			pstmt.setInt(1, trackId);
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

	/*
	  여러 트랙의 구성을 한 번에 — 경고 계산 · 조합 탭 트랙 목록이 쓴다.
	  트랙마다 조회하면 N+1 이 된다. 키는 track_id, 값은 구성 낱개 목록.
	*/
	public Map<Integer, List<ProductDTO>> findItemsByTracks(Collection<Integer> trackIds)
			throws SQLException {
		Map<Integer, List<ProductDTO>> map = new LinkedHashMap<Integer, List<ProductDTO>>();
		if (trackIds == null || trackIds.isEmpty()) {
			return map;
		}
		for (Integer id : trackIds) {
			map.put(id, new ArrayList<ProductDTO>());
		}
		String sql = SELECT_ITEMS + " WHERE ti.track_id IN (" + ProductDAO.placeholders(trackIds.size())
				   + ") ORDER BY ti.track_id, ti.sort_order, ti.item_id";
		Connection conn = null;
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			conn = DBUtil.getConnection();
			pstmt = conn.prepareStatement(sql);
			int i = 1;
			for (Integer id : trackIds) {
				pstmt.setInt(i++, id.intValue());
			}
			rs = pstmt.executeQuery();
			while (rs.next()) {
				map.get(Integer.valueOf(rs.getInt("track_id"))).add(map(rs));
			}
			return map;
		} finally {
			DBUtil.close(rs, pstmt, conn);
		}
	}

	/*
	  두 낱개를 함께 담은 트랙 수 — 금지 조합을 새로 등록할 때 검사한다.
	  이미 어떤 트랙 안에 같이 들어 있으면 그 트랙이 금지 조합을 품게 된다 (ERD § 8-14).
	*/
	public int countTracksContainingBoth(int productA, int productB) throws SQLException {
		String sql = "SELECT COUNT(*) FROM track_item a"
				   + "  JOIN track_item b ON b.track_id = a.track_id"
				   + " WHERE a.item_id = ? AND b.item_id = ?";
		Connection conn = null;
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			conn = DBUtil.getConnection();
			pstmt = conn.prepareStatement(sql);
			pstmt.setInt(1, productA);
			pstmt.setInt(2, productB);
			rs = pstmt.executeQuery();
			return rs.next() ? rs.getInt(1) : 0;
		} finally {
			DBUtil.close(rs, pstmt, conn);
		}
	}

	/* 이 낱개가 구성품으로 들어간 트랙 수 — 낱개를 트랙으로 바꾸려 할 때 막는다. */
	public int countTracksUsing(int itemId) throws SQLException {
		String sql = "SELECT COUNT(*) FROM track_item WHERE item_id = ?";
		Connection conn = null;
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			conn = DBUtil.getConnection();
			pstmt = conn.prepareStatement(sql);
			pstmt.setInt(1, itemId);
			rs = pstmt.executeQuery();
			return rs.next() ? rs.getInt(1) : 0;
		} finally {
			DBUtil.close(rs, pstmt, conn);
		}
	}

	public int deleteByTrack(Connection conn, int trackId) throws SQLException {
		String sql = "DELETE FROM track_item WHERE track_id = ?";
		PreparedStatement pstmt = null;
		try {
			pstmt = conn.prepareStatement(sql);
			pstmt.setInt(1, trackId);
			return pstmt.executeUpdate();
		} finally {
			DBUtil.close(pstmt);
		}
	}

	public int insert(Connection conn, int trackId, int itemId, int sortOrder) throws SQLException {
		String sql = "INSERT INTO track_item (track_id, item_id, sort_order) VALUES (?, ?, ?)";
		PreparedStatement pstmt = null;
		try {
			pstmt = conn.prepareStatement(sql);
			pstmt.setInt(1, trackId);
			pstmt.setInt(2, itemId);
			pstmt.setInt(3, sortOrder);
			return pstmt.executeUpdate();
		} finally {
			DBUtil.close(pstmt);
		}
	}

	private ProductDTO map(ResultSet rs) throws SQLException {
		ProductDTO dto = new ProductDTO();
		dto.setProductId(rs.getInt("product_id"));
		dto.setCategoryId(rs.getInt("category_id"));
		dto.setProductType(rs.getString("product_type"));
		dto.setName(rs.getString("name"));
		dto.setPrice(rs.getInt("price"));
		dto.setStock(rs.getInt("stock"));
		dto.setDescription(rs.getString("description"));
		dto.setThumbnail(rs.getString("thumbnail"));
		dto.setStatus(rs.getString("status"));
		dto.setCreatedAt(rs.getTimestamp("created_at"));
		return dto;
	}
}
