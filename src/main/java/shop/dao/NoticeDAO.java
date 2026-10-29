package shop.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import shop.dto.NoticeDTO;
import shop.dto.PageDTO;
import shop.util.DBUtil;
import shop.util.Paging;

/* notice — 공지사항. */
public class NoticeDAO {

	private static final String COLUMNS = "notice_id, title, content, view_count, created_at";

	/* 메인 상단 공지 */
	public List<NoticeDTO> findLatest(int limit) throws SQLException {
		String sql = "SELECT " + COLUMNS + " FROM notice ORDER BY notice_id DESC LIMIT ?";
		List<NoticeDTO> list = new ArrayList<NoticeDTO>();
		Connection conn = null;
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			conn = DBUtil.getConnection();
			pstmt = conn.prepareStatement(sql);
			pstmt.setInt(1, limit);
			rs = pstmt.executeQuery();
			while (rs.next()) {
				list.add(map(rs));
			}
			return list;
		} finally {
			DBUtil.close(rs, pstmt, conn);
		}
	}

	public PageDTO<NoticeDTO> findPage(String keyword, int page, int size) throws SQLException {
		StringBuilder where = new StringBuilder(" WHERE 1 = 1");
		List<Object> args = new ArrayList<Object>();
		if (keyword != null && keyword.length() > 0) {
			where.append(" AND title LIKE ?");
			args.add("%" + keyword + "%");
		}

		int total = 0;
		List<NoticeDTO> list = new ArrayList<NoticeDTO>();
		Connection conn = null;
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			conn = DBUtil.getConnection();
			pstmt = conn.prepareStatement("SELECT COUNT(*) FROM notice" + where);
			bind(pstmt, args);
			rs = pstmt.executeQuery();
			if (rs.next()) {
				total = rs.getInt(1);
			}
			DBUtil.close(rs);
			DBUtil.close(pstmt);

			pstmt = conn.prepareStatement("SELECT " + COLUMNS + " FROM notice" + where
					+ " ORDER BY notice_id DESC LIMIT ? OFFSET ?");
			int i = bind(pstmt, args);
			pstmt.setInt(i++, Paging.normalizeSize(size));
			pstmt.setInt(i, Paging.offset(page, size));
			rs = pstmt.executeQuery();
			while (rs.next()) {
				list.add(map(rs));
			}
		} finally {
			DBUtil.close(rs, pstmt, conn);
		}
		return new PageDTO<NoticeDTO>(list, page, size, total);
	}

	public NoticeDTO findById(int noticeId) throws SQLException {
		String sql = "SELECT " + COLUMNS + " FROM notice WHERE notice_id = ?";
		Connection conn = null;
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			conn = DBUtil.getConnection();
			pstmt = conn.prepareStatement(sql);
			pstmt.setInt(1, noticeId);
			rs = pstmt.executeQuery();
			return rs.next() ? map(rs) : null;
		} finally {
			DBUtil.close(rs, pstmt, conn);
		}
	}

	/* 조회수 증가 — 상세 진입마다 1 (CRUD.md 20번의 U) */
	public int increaseViewCount(int noticeId) throws SQLException {
		String sql = "UPDATE notice SET view_count = view_count + 1 WHERE notice_id = ?";
		Connection conn = null;
		PreparedStatement pstmt = null;
		try {
			conn = DBUtil.getConnection();
			pstmt = conn.prepareStatement(sql);
			pstmt.setInt(1, noticeId);
			return pstmt.executeUpdate();
		} finally {
			DBUtil.close(pstmt, conn);
		}
	}

	public int insert(String title, String content) throws SQLException {
		String sql = "INSERT INTO notice (title, content) VALUES (?, ?)";
		Connection conn = null;
		PreparedStatement pstmt = null;
		try {
			conn = DBUtil.getConnection();
			pstmt = conn.prepareStatement(sql);
			pstmt.setString(1, title);
			pstmt.setString(2, content);
			return pstmt.executeUpdate();
		} finally {
			DBUtil.close(pstmt, conn);
		}
	}

	public int update(int noticeId, String title, String content) throws SQLException {
		String sql = "UPDATE notice SET title = ?, content = ? WHERE notice_id = ?";
		Connection conn = null;
		PreparedStatement pstmt = null;
		try {
			conn = DBUtil.getConnection();
			pstmt = conn.prepareStatement(sql);
			pstmt.setString(1, title);
			pstmt.setString(2, content);
			pstmt.setInt(3, noticeId);
			return pstmt.executeUpdate();
		} finally {
			DBUtil.close(pstmt, conn);
		}
	}

	public int delete(int noticeId) throws SQLException {
		String sql = "DELETE FROM notice WHERE notice_id = ?";
		Connection conn = null;
		PreparedStatement pstmt = null;
		try {
			conn = DBUtil.getConnection();
			pstmt = conn.prepareStatement(sql);
			pstmt.setInt(1, noticeId);
			return pstmt.executeUpdate();
		} finally {
			DBUtil.close(pstmt, conn);
		}
	}

	private int bind(PreparedStatement pstmt, List<Object> args) throws SQLException {
		int i = 1;
		for (Object arg : args) {
			pstmt.setObject(i++, arg);
		}
		return i;
	}

	private NoticeDTO map(ResultSet rs) throws SQLException {
		NoticeDTO dto = new NoticeDTO();
		dto.setNoticeId(rs.getInt("notice_id"));
		dto.setTitle(rs.getString("title"));
		dto.setContent(rs.getString("content"));
		dto.setViewCount(rs.getInt("view_count"));
		dto.setCreatedAt(rs.getTimestamp("created_at"));
		return dto;
	}
}
