package shop.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import shop.dto.PageDTO;
import shop.dto.QnaDTO;
import shop.util.DBUtil;
import shop.util.Paging;
import shop.util.StringUtil;

/*
  qna — 상품 문의.

  ★ 불변식: answer 와 answered_at 은 항상 같은 UPDATE 로 함께 쓴다 (ERD § 2.14).
    하나만 채우면 미답변 판정이 깨진다. chk_qna_answered CHECK 가 DB 에서도 막는다.
  ★ 미답변 조회 조건은 answered_at IS NULL 이다 — answer IS NULL 이 아니다.
    answer 는 VARCHAR(1000) x utf8mb4 라 인덱스를 걸 수 없어 판정 컬럼을 옮겼다.
*/
public class QnaDAO {

	private static final String SELECT_BASE =
		"SELECT q.qna_id, q.member_id, q.product_id, q.question, q.is_secret, q.answer,"
	  + "       q.created_at, q.answered_at, m.name AS member_name, p.name AS product_name"
	  + "  FROM qna q"
	  + "  JOIN member  m ON m.member_id  = q.member_id"
	  + "  JOIN product p ON p.product_id = q.product_id";

	/* 상품 상세 — 공개 목록이므로 이름을 가린다. 비밀글 본문은 서비스 계층이 가린다. */
	public List<QnaDTO> findByProduct(int productId) throws SQLException {
		String sql = SELECT_BASE + " WHERE q.product_id = ? ORDER BY q.created_at DESC";
		return query(sql, true, Integer.valueOf(productId));
	}

	public List<QnaDTO> findByMember(int memberId) throws SQLException {
		String sql = SELECT_BASE + " WHERE q.member_id = ? ORDER BY q.created_at DESC";
		return query(sql, false, Integer.valueOf(memberId));
	}

	public QnaDTO findById(int qnaId) throws SQLException {
		String sql = SELECT_BASE + " WHERE q.qna_id = ?";
		List<QnaDTO> list = query(sql, false, Integer.valueOf(qnaId));
		return list.isEmpty() ? null : list.get(0);
	}

	public int insert(int memberId, int productId, String question, boolean secret)
			throws SQLException {
		String sql = "INSERT INTO qna (member_id, product_id, question, is_secret)"
				   + " VALUES (?, ?, ?, ?)";
		Connection conn = null;
		PreparedStatement pstmt = null;
		try {
			conn = DBUtil.getConnection();
			pstmt = conn.prepareStatement(sql);
			pstmt.setInt(1, memberId);
			pstmt.setInt(2, productId);
			pstmt.setString(3, question);
			pstmt.setString(4, secret ? "Y" : "N");
			return pstmt.executeUpdate();
		} finally {
			DBUtil.close(pstmt, conn);
		}
	}

	public int updateQuestion(int qnaId, int memberId, String question, boolean secret)
			throws SQLException {
		String sql = "UPDATE qna SET question = ?, is_secret = ?"
				   + " WHERE qna_id = ? AND member_id = ?";
		Connection conn = null;
		PreparedStatement pstmt = null;
		try {
			conn = DBUtil.getConnection();
			pstmt = conn.prepareStatement(sql);
			pstmt.setString(1, question);
			pstmt.setString(2, secret ? "Y" : "N");
			pstmt.setInt(3, qnaId);
			pstmt.setInt(4, memberId);
			return pstmt.executeUpdate();
		} finally {
			DBUtil.close(pstmt, conn);
		}
	}

	public int delete(int qnaId, int memberId) throws SQLException {
		String sql = "DELETE FROM qna WHERE qna_id = ? AND member_id = ?";
		Connection conn = null;
		PreparedStatement pstmt = null;
		try {
			conn = DBUtil.getConnection();
			pstmt = conn.prepareStatement(sql);
			pstmt.setInt(1, qnaId);
			pstmt.setInt(2, memberId);
			return pstmt.executeUpdate();
		} finally {
			DBUtil.close(pstmt, conn);
		}
	}

	/* 답변 — 두 컬럼을 한 문장에서 함께 쓴다. 답변 수정도 answered_at 을 갱신한다. */
	public int answer(Connection conn, int qnaId, String answer) throws SQLException {
		String sql = "UPDATE qna SET answer = ?, answered_at = NOW() WHERE qna_id = ?";
		PreparedStatement pstmt = null;
		try {
			pstmt = conn.prepareStatement(sql);
			pstmt.setString(1, answer);
			pstmt.setInt(2, qnaId);
			return pstmt.executeUpdate();
		} finally {
			DBUtil.close(pstmt);
		}
	}

	/* 답변 회수 — 둘 다 비워야 CHECK 를 통과한다. */
	public int clearAnswer(Connection conn, int qnaId) throws SQLException {
		String sql = "UPDATE qna SET answer = NULL, answered_at = NULL WHERE qna_id = ?";
		PreparedStatement pstmt = null;
		try {
			pstmt = conn.prepareStatement(sql);
			pstmt.setInt(1, qnaId);
			return pstmt.executeUpdate();
		} finally {
			DBUtil.close(pstmt);
		}
	}

	public int countUnanswered() throws SQLException {
		String sql = "SELECT COUNT(*) FROM qna WHERE answered_at IS NULL";
		Connection conn = null;
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			conn = DBUtil.getConnection();
			pstmt = conn.prepareStatement(sql);
			rs = pstmt.executeQuery();
			return rs.next() ? rs.getInt(1) : 0;
		} finally {
			DBUtil.close(rs, pstmt, conn);
		}
	}

	/* 관리자 문의답변 목록 — 미답변만 보기를 기본으로 쓴다. */
	public PageDTO<QnaDTO> findAdminPage(String keyword, boolean onlyUnanswered,
			int page, int size) throws SQLException {
		StringBuilder where = new StringBuilder(" WHERE 1 = 1");
		List<Object> args = new ArrayList<Object>();
		if (onlyUnanswered) {
			where.append(" AND q.answered_at IS NULL");
		}
		if (keyword != null && keyword.length() > 0) {
			where.append(" AND (p.name LIKE ? OR m.login_id LIKE ? OR q.question LIKE ?)");
			String like = "%" + keyword + "%";
			args.add(like);
			args.add(like);
			args.add(like);
		}

		String countSql = "SELECT COUNT(*) FROM qna q"
						+ "  JOIN member m ON m.member_id = q.member_id"
						+ "  JOIN product p ON p.product_id = q.product_id" + where;

		int total = 0;
		List<QnaDTO> list = new ArrayList<QnaDTO>();
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

			/* 미답변이 먼저, 그 안에서 오래된 순 — 답변 대기열의 자연스러운 순서다. */
			pstmt = conn.prepareStatement(SELECT_BASE + where
					+ " ORDER BY (q.answered_at IS NOT NULL), q.created_at LIMIT ? OFFSET ?");
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
		return new PageDTO<QnaDTO>(list, page, size, total);
	}

	private List<QnaDTO> query(String sql, boolean mask, Object... args) throws SQLException {
		List<QnaDTO> list = new ArrayList<QnaDTO>();
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

	private QnaDTO map(ResultSet rs, boolean mask) throws SQLException {
		QnaDTO dto = new QnaDTO();
		dto.setQnaId(rs.getInt("qna_id"));
		dto.setMemberId(rs.getInt("member_id"));
		dto.setProductId(rs.getInt("product_id"));
		String name = rs.getString("member_name");
		dto.setMemberName(mask ? StringUtil.maskName(name) : name);
		dto.setProductName(rs.getString("product_name"));
		dto.setQuestion(rs.getString("question"));
		dto.setIsSecret(rs.getString("is_secret"));
		dto.setAnswer(rs.getString("answer"));
		dto.setCreatedAt(rs.getTimestamp("created_at"));
		dto.setAnsweredAt(rs.getTimestamp("answered_at"));
		return dto;
	}
}
