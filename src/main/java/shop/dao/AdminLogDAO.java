package shop.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import shop.dto.AdminLogDTO;
import shop.dto.PageDTO;
import shop.util.AuditContext;
import shop.util.DBUtil;
import shop.util.Paging;

/*
  admin_log — 관리자 · dev 조작 기록.

  append-only 다. UPDATE · DELETE 가 없다 (CRUD.md § 4).
  action 어휘는 ERD § 2.17 에 고정돼 있다 — 아래 상수가 그 목록이다.
  문자열을 직접 쓰지 않고 상수를 쓰면 오타가 감사 로그를 조용히 망가뜨리지 않는다.
*/
public class AdminLogDAO {

	public static final String MODE_ADMIN = "ADMIN";
	public static final String MODE_DEV = "DEV";

	/* ADMIN */
	public static final String PRODUCT_CREATE = "PRODUCT_CREATE";
	public static final String PRODUCT_UPDATE = "PRODUCT_UPDATE";
	public static final String PRODUCT_DELETE = "PRODUCT_DELETE";
	public static final String CATEGORY_CREATE = "CATEGORY_CREATE";
	public static final String CATEGORY_UPDATE = "CATEGORY_UPDATE";
	public static final String CATEGORY_DELETE = "CATEGORY_DELETE";
	public static final String DELIVERY_NEXT_STEP = "DELIVERY_NEXT_STEP";
	public static final String DELIVERY_TRACKING_SET = "DELIVERY_TRACKING_SET";
	public static final String ORDER_CANCEL = "ORDER_CANCEL";
	public static final String MEMBER_LEAVE = "MEMBER_LEAVE";
	public static final String QNA_ANSWER = "QNA_ANSWER";
	public static final String NOTICE_CREATE = "NOTICE_CREATE";
	public static final String NOTICE_UPDATE = "NOTICE_UPDATE";
	public static final String NOTICE_DELETE = "NOTICE_DELETE";
	public static final String REVIEW_DELETE = "REVIEW_DELETE";

	/* DEV */
	public static final String FORCE_DELIVERY_STATUS = "FORCE_DELIVERY_STATUS";
	public static final String FORCE_ORDER_STATUS = "FORCE_ORDER_STATUS";
	public static final String FORCE_PAY_STATUS = "FORCE_PAY_STATUS";
	public static final String FORCE_STOCK_SET = "FORCE_STOCK_SET";
	public static final String SHOP_STATUS_TOGGLE = "SHOP_STATUS_TOGGLE";
	/* 샌드박스 임시 어휘 — ERD § 2.17 에 설정 변경 action 이 없어 추가 요청 중 (트랙 문서 § 7) */
	public static final String CONFIG_UPDATE = "CONFIG_UPDATE";
	public static final String SEED_GENERATE = "SEED_GENERATE";
	public static final String SEED_TRUNCATE_ALL = "SEED_TRUNCATE_ALL";
	public static final String SEED_TRUNCATE_TABLE = "SEED_TRUNCATE_TABLE";
	public static final String LOGIN_AS = "LOGIN_AS";
	public static final String LOGIN_AS_RETURN = "LOGIN_AS_RETURN";

	/* 감사로그 조회 화면의 필터 셀렉트가 그대로 쓴다. */
	public static final String[] ACTIONS = {
		PRODUCT_CREATE, PRODUCT_UPDATE, PRODUCT_DELETE,
		CATEGORY_CREATE, CATEGORY_UPDATE, CATEGORY_DELETE,
		DELIVERY_NEXT_STEP, DELIVERY_TRACKING_SET, ORDER_CANCEL,
		MEMBER_LEAVE, QNA_ANSWER,
		NOTICE_CREATE, NOTICE_UPDATE, NOTICE_DELETE, REVIEW_DELETE,
		FORCE_DELIVERY_STATUS, FORCE_ORDER_STATUS, FORCE_PAY_STATUS,
		FORCE_STOCK_SET, SHOP_STATUS_TOGGLE, CONFIG_UPDATE,
		SEED_GENERATE, SEED_TRUNCATE_ALL, SEED_TRUNCATE_TABLE, LOGIN_AS, LOGIN_AS_RETURN };

	/* 트랜잭션에 참여하는 기록 — 본 작업과 같이 커밋되거나 같이 롤백된다. */
	public int insert(Connection conn, int memberId, String mode, String action,
			String targetTable, Integer targetId, String detail) throws SQLException {
		String sql = "INSERT INTO admin_log"
				   + " (member_id, mode, action, target_table, target_id, detail)"
				   + " VALUES (?, ?, ?, ?, ?, ?)";
		PreparedStatement pstmt = null;
		try {
			pstmt = conn.prepareStatement(sql);
			pstmt.setInt(1, memberId);
			pstmt.setString(2, mode);
			pstmt.setString(3, action);
			pstmt.setString(4, targetTable);
			if (targetId == null) {
				pstmt.setNull(5, java.sql.Types.INTEGER);
			} else {
				pstmt.setInt(5, targetId.intValue());
			}
			pstmt.setString(6, cut(withImpersonator(detail)));
			return pstmt.executeUpdate();
		} finally {
			DBUtil.close(pstmt);
		}
	}

	/* 단독 기록 — 트랜잭션이 필요 없는 조작용 */
	public int insert(int memberId, String mode, String action, String targetTable,
			Integer targetId, String detail) throws SQLException {
		Connection conn = null;
		try {
			conn = DBUtil.getConnection();
			return insert(conn, memberId, mode, action, targetTable, targetId, detail);
		} finally {
			DBUtil.close(conn);
		}
	}

	/*
	  계정 전환 중에 남는 기록에는 원래 DEV 를 덧붙인다 (ERD § 5.4-3).
	  전환되면 member_id 가 대상 계정이 되므로, 이게 없으면 실제 행위자가 사라진다.
	*/
	private String withImpersonator(String detail) {
		int devId = AuditContext.impersonator();
		if (devId <= 0) {
			return detail;
		}
		String suffix = "impersonated_by=" + devId;
		return detail == null ? suffix : detail + " / " + suffix;
	}

	/* detail 은 VARCHAR(500) 이다. 넘치면 INSERT 가 통째로 실패하므로 잘라서 넣는다. */
	private String cut(String detail) {
		if (detail == null) {
			return null;
		}
		return detail.length() <= 500 ? detail : detail.substring(0, 500);
	}

	/*
	  감사로그 조회 (CRUD.md § 6-2 — 36번 /dev/log).

	  정렬은 created_at DESC 고정이다. 감사 로그를 최신순 말고 다르게 볼 이유가 없다.
	  member 를 조인해 login_id 를 함께 준다 — 숫자 member_id 만으로는 읽을 수 없다.
	*/
	public PageDTO<AdminLogDTO> findPage(String mode, String action, String targetTable,
			String fromDate, String toDate, int page, int size) throws SQLException {
		StringBuilder where = new StringBuilder(" WHERE 1 = 1");
		List<Object> args = new ArrayList<Object>();
		if (MODE_ADMIN.equals(mode) || MODE_DEV.equals(mode)) {
			where.append(" AND l.mode = ?");
			args.add(mode);
		}
		if (action != null && action.length() > 0) {
			where.append(" AND l.action = ?");
			args.add(action);
		}
		if (targetTable != null && targetTable.length() > 0) {
			where.append(" AND l.target_table = ?");
			args.add(targetTable);
		}
		if (fromDate != null && fromDate.length() > 0) {
			where.append(" AND l.created_at >= ?");
			args.add(fromDate + " 00:00:00");
		}
		if (toDate != null && toDate.length() > 0) {
			where.append(" AND l.created_at <= ?");
			args.add(toDate + " 23:59:59");
		}

		String from = "  FROM admin_log l JOIN member m ON m.member_id = l.member_id";

		int total = 0;
		List<AdminLogDTO> list = new ArrayList<AdminLogDTO>();
		Connection conn = null;
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			conn = DBUtil.getConnection();
			pstmt = conn.prepareStatement("SELECT COUNT(*)" + from + where);
			bind(pstmt, args);
			rs = pstmt.executeQuery();
			if (rs.next()) {
				total = rs.getInt(1);
			}
			DBUtil.close(rs);
			DBUtil.close(pstmt);

			pstmt = conn.prepareStatement(
					"SELECT l.log_id, l.member_id, l.mode, l.action, l.target_table,"
				  + "       l.target_id, l.detail, l.created_at, m.login_id AS member_name"
				  + from + where
				  + " ORDER BY l.created_at DESC, l.log_id DESC LIMIT ? OFFSET ?");
			int i = bind(pstmt, args);
			pstmt.setInt(i++, Paging.normalizeSize(size));
			pstmt.setInt(i, Paging.offset(page, size));
			rs = pstmt.executeQuery();
			while (rs.next()) {
				AdminLogDTO dto = new AdminLogDTO();
				dto.setLogId(rs.getInt("log_id"));
				dto.setMemberId(rs.getInt("member_id"));
				dto.setMemberName(rs.getString("member_name"));
				dto.setMode(rs.getString("mode"));
				dto.setAction(rs.getString("action"));
				dto.setTargetTable(rs.getString("target_table"));
				int targetId = rs.getInt("target_id");
				dto.setTargetId(rs.wasNull() ? null : Integer.valueOf(targetId));
				dto.setDetail(rs.getString("detail"));
				dto.setCreatedAt(rs.getTimestamp("created_at"));
				list.add(dto);
			}
		} finally {
			DBUtil.close(rs, pstmt, conn);
		}
		return new PageDTO<AdminLogDTO>(list, page, size, total);
	}

	private int bind(PreparedStatement pstmt, List<Object> args) throws SQLException {
		int i = 1;
		for (Object arg : args) {
			pstmt.setObject(i++, arg);
		}
		return i;
	}
}
