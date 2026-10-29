package shop.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import shop.dto.PageDTO;
import shop.dto.ReviewDTO;
import shop.util.DBUtil;
import shop.util.Paging;
import shop.util.StringUtil;

/*
  review — 리뷰.

  권한 비대칭이 이 테이블의 규칙이다 (CRUD.md § 6-3).
      작성 : 본인만, 구매 건당 1회 (item_id UNIQUE 가 보장)
      조회 : 전체 공개
      수정 : 본인만
      삭제 : 본인 + ADMIN
  ADMIN 에게 수정 권한이 없다 — 남의 글을 고치는 경로를 만들지 않는다.

  작성자명은 조회 SQL 이 아니라 여기서 마스킹한다. 관리자 화면은 같은 쿼리로
  원본 이름을 써야 하기 때문이다 (ERD § 7).
*/
public class ReviewDAO {

	private static final String SELECT_BASE =
		"SELECT r.review_id, r.member_id, r.product_id, r.item_id, r.rating, r.content,"
	  + "       r.created_at, m.name AS member_name, p.name AS product_name"
	  + "  FROM review r"
	  + "  JOIN member  m ON m.member_id  = r.member_id"
	  + "  JOIN product p ON p.product_id = r.product_id";

	/* 상품 상세 — 공개 목록이므로 이름을 가린다. */
	public List<ReviewDTO> findByProduct(int productId) throws SQLException {
		String sql = SELECT_BASE + " WHERE r.product_id = ? ORDER BY r.created_at DESC";
		return query(sql, true, Integer.valueOf(productId));
	}

	/* 마이페이지 내 리뷰 — 본인 글이므로 가리지 않는다. */
	public List<ReviewDTO> findByMember(int memberId) throws SQLException {
		String sql = SELECT_BASE + " WHERE r.member_id = ? ORDER BY r.created_at DESC";
		return query(sql, false, Integer.valueOf(memberId));
	}

	public ReviewDTO findByItem(int itemId) throws SQLException {
		String sql = SELECT_BASE + " WHERE r.item_id = ?";
		List<ReviewDTO> list = query(sql, false, Integer.valueOf(itemId));
		return list.isEmpty() ? null : list.get(0);
	}

	public ReviewDTO findById(int reviewId) throws SQLException {
		String sql = SELECT_BASE + " WHERE r.review_id = ?";
		List<ReviewDTO> list = query(sql, false, Integer.valueOf(reviewId));
		return list.isEmpty() ? null : list.get(0);
	}

	/*
	  작성.

	  product_id 는 item_id 로 유도할 수 있는 중복 컬럼이지만(ERD § 8 쟁점 4),
	  상품 상세에서 조인을 줄이려는 의도적 비정규화라 함께 넣는다.
	*/
	public int insert(int memberId, int productId, int itemId, int rating, String content)
			throws SQLException {
		String sql = "INSERT INTO review (member_id, product_id, item_id, rating, content)"
				   + " VALUES (?, ?, ?, ?, ?)";
		Connection conn = null;
		PreparedStatement pstmt = null;
		try {
			conn = DBUtil.getConnection();
			pstmt = conn.prepareStatement(sql);
			pstmt.setInt(1, memberId);
			pstmt.setInt(2, productId);
			pstmt.setInt(3, itemId);
			pstmt.setInt(4, rating);
			pstmt.setString(5, content);
			return pstmt.executeUpdate();
		} finally {
			DBUtil.close(pstmt, conn);
		}
	}

	/* 수정은 본인만 — member_id 조건을 뺄 수 없다. */
	public int update(int reviewId, int memberId, int rating, String content)
			throws SQLException {
		String sql = "UPDATE review SET rating = ?, content = ?"
				   + " WHERE review_id = ? AND member_id = ?";
		Connection conn = null;
		PreparedStatement pstmt = null;
		try {
			conn = DBUtil.getConnection();
			pstmt = conn.prepareStatement(sql);
			pstmt.setInt(1, rating);
			pstmt.setString(2, content);
			pstmt.setInt(3, reviewId);
			pstmt.setInt(4, memberId);
			return pstmt.executeUpdate();
		} finally {
			DBUtil.close(pstmt, conn);
		}
	}

	public int deleteByOwner(int reviewId, int memberId) throws SQLException {
		String sql = "DELETE FROM review WHERE review_id = ? AND member_id = ?";
		Connection conn = null;
		PreparedStatement pstmt = null;
		try {
			conn = DBUtil.getConnection();
			pstmt = conn.prepareStatement(sql);
			pstmt.setInt(1, reviewId);
			pstmt.setInt(2, memberId);
			return pstmt.executeUpdate();
		} finally {
			DBUtil.close(pstmt, conn);
		}
	}

	/* 관리자 삭제 — 내리는 것만 되고 고치는 것은 안 된다. */
	public int deleteByAdmin(int reviewId) throws SQLException {
		String sql = "DELETE FROM review WHERE review_id = ?";
		Connection conn = null;
		PreparedStatement pstmt = null;
		try {
			conn = DBUtil.getConnection();
			pstmt = conn.prepareStatement(sql);
			pstmt.setInt(1, reviewId);
			return pstmt.executeUpdate();
		} finally {
			DBUtil.close(pstmt, conn);
		}
	}

	/* 관리자 리뷰관리 목록 — 원본 이름을 보여준다. */
	public PageDTO<ReviewDTO> findAdminPage(String keyword, int page, int size)
			throws SQLException {
		StringBuilder where = new StringBuilder(" WHERE 1 = 1");
		List<Object> args = new ArrayList<Object>();
		if (keyword != null && keyword.length() > 0) {
			where.append(" AND (p.name LIKE ? OR m.login_id LIKE ? OR r.content LIKE ?)");
			String like = "%" + keyword + "%";
			args.add(like);
			args.add(like);
			args.add(like);
		}

		String countSql = "SELECT COUNT(*) FROM review r"
						+ "  JOIN member m ON m.member_id = r.member_id"
						+ "  JOIN product p ON p.product_id = r.product_id" + where;

		int total = 0;
		List<ReviewDTO> list = new ArrayList<ReviewDTO>();
		Connection conn = null;
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			conn = DBUtil.getConnection();
			pstmt = conn.prepareStatement(countSql);
			bind(pstmt, args);
			rs = pstmt.executeQuery();
			if (rs.next()) {
				total = rs.getInt(1);
			}
			DBUtil.close(rs);
			DBUtil.close(pstmt);

			pstmt = conn.prepareStatement(SELECT_BASE + where
					+ " ORDER BY r.created_at DESC LIMIT ? OFFSET ?");
			int i = bind(pstmt, args);
			pstmt.setInt(i++, Paging.normalizeSize(size));
			pstmt.setInt(i, Paging.offset(page, size));
			rs = pstmt.executeQuery();
			while (rs.next()) {
				list.add(map(rs, false));
			}
		} finally {
			DBUtil.close(rs, pstmt, conn);
		}
		return new PageDTO<ReviewDTO>(list, page, size, total);
	}

	private List<ReviewDTO> query(String sql, boolean mask, Object... args) throws SQLException {
		List<ReviewDTO> list = new ArrayList<ReviewDTO>();
		Connection conn = null;
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			conn = DBUtil.getConnection();
			pstmt = conn.prepareStatement(sql);
			for (int i = 0; i < args.length; i++) {
				pstmt.setObject(i + 1, args[i]);
			}
			rs = pstmt.executeQuery();
			while (rs.next()) {
				list.add(map(rs, mask));
			}
			return list;
		} finally {
			DBUtil.close(rs, pstmt, conn);
		}
	}

	private int bind(PreparedStatement pstmt, List<Object> args) throws SQLException {
		int i = 1;
		for (Object arg : args) {
			pstmt.setObject(i++, arg);
		}
		return i;
	}

	private ReviewDTO map(ResultSet rs, boolean mask) throws SQLException {
		ReviewDTO dto = new ReviewDTO();
		dto.setReviewId(rs.getInt("review_id"));
		dto.setMemberId(rs.getInt("member_id"));
		dto.setProductId(rs.getInt("product_id"));
		dto.setItemId(rs.getInt("item_id"));
		String name = rs.getString("member_name");
		dto.setMemberName(mask ? StringUtil.maskName(name) : name);
		dto.setProductName(rs.getString("product_name"));
		dto.setRating(rs.getInt("rating"));
		dto.setContent(rs.getString("content"));
		dto.setCreatedAt(rs.getTimestamp("created_at"));
		return dto;
	}
}
