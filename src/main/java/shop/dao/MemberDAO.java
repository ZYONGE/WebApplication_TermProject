package shop.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import shop.dto.MemberDTO;
import shop.dto.PageDTO;
import shop.util.DBUtil;
import shop.util.Paging;

/*
  member — 회원.

  물리 삭제가 없다. 주문 · 리뷰가 FK 로 참조하므로 탈퇴는 status = 'LEFT' 로 처리한다.
  (CRUD.md § 4)
*/
public class MemberDAO {

	private static final String COLUMNS =
		"member_id, login_id, password, name, email, phone, role, status, created_at";

	public MemberDTO findByLoginId(String loginId) throws SQLException {
		String sql = "SELECT " + COLUMNS + " FROM member WHERE login_id = ?";
		Connection conn = null;
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			conn = DBUtil.getConnection();
			pstmt = conn.prepareStatement(sql);
			pstmt.setString(1, loginId);
			rs = pstmt.executeQuery();
			return rs.next() ? map(rs) : null;
		} finally {
			DBUtil.close(rs, pstmt, conn);
		}
	}

	public MemberDTO findById(int memberId) throws SQLException {
		String sql = "SELECT " + COLUMNS + " FROM member WHERE member_id = ?";
		Connection conn = null;
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			conn = DBUtil.getConnection();
			pstmt = conn.prepareStatement(sql);
			pstmt.setInt(1, memberId);
			rs = pstmt.executeQuery();
			return rs.next() ? map(rs) : null;
		} finally {
			DBUtil.close(rs, pstmt, conn);
		}
	}

	public boolean existsLoginId(String loginId) throws SQLException {
		return exists("SELECT 1 FROM member WHERE login_id = ?", loginId);
	}

	public boolean existsEmail(String email) throws SQLException {
		return exists("SELECT 1 FROM member WHERE email = ?", email);
	}

	/* 수정 화면용 — 자기 자신은 중복에서 제외한다. */
	public boolean existsEmailExcept(String email, int memberId) throws SQLException {
		String sql = "SELECT 1 FROM member WHERE email = ? AND member_id <> ?";
		Connection conn = null;
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			conn = DBUtil.getConnection();
			pstmt = conn.prepareStatement(sql);
			pstmt.setString(1, email);
			pstmt.setInt(2, memberId);
			rs = pstmt.executeQuery();
			return rs.next();
		} finally {
			DBUtil.close(rs, pstmt, conn);
		}
	}

	private boolean exists(String sql, String value) throws SQLException {
		Connection conn = null;
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			conn = DBUtil.getConnection();
			pstmt = conn.prepareStatement(sql);
			pstmt.setString(1, value);
			rs = pstmt.executeQuery();
			return rs.next();
		} finally {
			DBUtil.close(rs, pstmt, conn);
		}
	}

	public int insert(MemberDTO member) throws SQLException {
		String sql = "INSERT INTO member (login_id, password, name, email, phone, role, status)"
				   + " VALUES (?, ?, ?, ?, ?, 'USER', 'ACTIVE')";
		Connection conn = null;
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			conn = DBUtil.getConnection();
			pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
			pstmt.setString(1, member.getLoginId());
			pstmt.setString(2, member.getPassword());
			pstmt.setString(3, member.getName());
			pstmt.setString(4, member.getEmail());
			pstmt.setString(5, member.getPhone());
			pstmt.executeUpdate();
			rs = pstmt.getGeneratedKeys();
			return rs.next() ? rs.getInt(1) : 0;
		} finally {
			DBUtil.close(rs, pstmt, conn);
		}
	}

	/* 정보 수정 — 비밀번호는 입력이 있을 때만 바꾸므로 별도 메서드로 분리한다. */
	public int update(MemberDTO member) throws SQLException {
		String sql = "UPDATE member SET name = ?, email = ?, phone = ? WHERE member_id = ?";
		Connection conn = null;
		PreparedStatement pstmt = null;
		try {
			conn = DBUtil.getConnection();
			pstmt = conn.prepareStatement(sql);
			pstmt.setString(1, member.getName());
			pstmt.setString(2, member.getEmail());
			pstmt.setString(3, member.getPhone());
			pstmt.setInt(4, member.getMemberId());
			return pstmt.executeUpdate();
		} finally {
			DBUtil.close(pstmt, conn);
		}
	}

	public int updatePassword(int memberId, String password) throws SQLException {
		String sql = "UPDATE member SET password = ? WHERE member_id = ?";
		Connection conn = null;
		PreparedStatement pstmt = null;
		try {
			conn = DBUtil.getConnection();
			pstmt = conn.prepareStatement(sql);
			pstmt.setString(1, password);
			pstmt.setInt(2, memberId);
			return pstmt.executeUpdate();
		} finally {
			DBUtil.close(pstmt, conn);
		}
	}

	/* 탈퇴 — 소프트 삭제 */
	public int leave(int memberId) throws SQLException {
		String sql = "UPDATE member SET status = 'LEFT' WHERE member_id = ? AND status = 'ACTIVE'";
		Connection conn = null;
		PreparedStatement pstmt = null;
		try {
			conn = DBUtil.getConnection();
			pstmt = conn.prepareStatement(sql);
			pstmt.setInt(1, memberId);
			return pstmt.executeUpdate();
		} finally {
			DBUtil.close(pstmt, conn);
		}
	}

	/*
	  권한 변경 — 관리자 회원관리.

	  SQL 에서도 DEV 를 한 번 더 막는다. 서비스 계층 검사가 1차지만,
	  권한 상승은 한 군데만 뚫려도 끝이라 DB 문장에도 가드를 남긴다. (ERD § 5)
	*/
	public int updateRole(Connection conn, int memberId, String role) throws SQLException {
		if (!"USER".equals(role) && !"ADMIN".equals(role)) {
			return 0;
		}
		String sql = "UPDATE member SET role = ? WHERE member_id = ? AND role <> 'DEV'";
		PreparedStatement pstmt = null;
		try {
			pstmt = conn.prepareStatement(sql);
			pstmt.setString(1, role);
			pstmt.setInt(2, memberId);
			return pstmt.executeUpdate();
		} finally {
			DBUtil.close(pstmt);
		}
	}

	/* 관리자 회원 목록 — 집계는 v_admin_member 가 들고 있다. */
	public PageDTO<MemberDTO> findPage(String keyword, String role, int page, int size)
			throws SQLException {
		StringBuilder where = new StringBuilder(" WHERE 1 = 1");
		List<Object> args = new ArrayList<Object>();
		if (keyword != null && keyword.length() > 0) {
			where.append(" AND (login_id LIKE ? OR name LIKE ? OR email LIKE ?)");
			String like = "%" + keyword + "%";
			args.add(like);
			args.add(like);
			args.add(like);
		}
		if ("USER".equals(role) || "ADMIN".equals(role) || "DEV".equals(role)) {
			where.append(" AND role = ?");
			args.add(role);
		}

		int total = count("SELECT COUNT(*) FROM member" + where, args);
		String sql = "SELECT " + COLUMNS + " FROM member" + where
				   + " ORDER BY member_id DESC LIMIT ? OFFSET ?";

		List<MemberDTO> list = new ArrayList<MemberDTO>();
		Connection conn = null;
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			conn = DBUtil.getConnection();
			pstmt = conn.prepareStatement(sql);
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
		return new PageDTO<MemberDTO>(list, page, size, total);
	}

	private int count(String sql, List<Object> args) throws SQLException {
		Connection conn = null;
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			conn = DBUtil.getConnection();
			pstmt = conn.prepareStatement(sql);
			bind(pstmt, args);
			rs = pstmt.executeQuery();
			return rs.next() ? rs.getInt(1) : 0;
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

	private MemberDTO map(ResultSet rs) throws SQLException {
		MemberDTO dto = new MemberDTO();
		dto.setMemberId(rs.getInt("member_id"));
		dto.setLoginId(rs.getString("login_id"));
		dto.setPassword(rs.getString("password"));
		dto.setName(rs.getString("name"));
		dto.setEmail(rs.getString("email"));
		dto.setPhone(rs.getString("phone"));
		dto.setRole(rs.getString("role"));
		dto.setStatus(rs.getString("status"));
		dto.setCreatedAt(rs.getTimestamp("created_at"));
		return dto;
	}
}
