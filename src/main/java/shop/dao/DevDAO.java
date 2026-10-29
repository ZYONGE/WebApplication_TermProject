package shop.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import shop.util.DBUtil;
import shop.util.Paging;

/*
  /dev 전용 — 데이터 브라우저 · 스키마 조회 · 초기화.

  ★ 이 클래스가 이 프로젝트에서 유일하게 SQL 에 식별자를 문자열로 붙인다.
    테이블 · 컬럼 이름은 ? 바인딩이 되지 않기 때문이다 (ERD § 6.5).
    그래서 들어오는 이름은 전부 아래 허용 목록을 통과해야 하고, 통과한 이름만
    백틱으로 감싸 넣는다. 목록 밖의 값은 즉시 거부한다.
*/
public class DevDAO {

	/* 17 테이블 + 뷰 5개. 이 Set 밖의 이름은 받지 않는다. */
	public static final Set<String> ALLOWED_TABLES;

	static {
		Set<String> set = new LinkedHashSet<String>();
		set.add("member");
		set.add("address");
		set.add("category");
		set.add("product");
		set.add("product_image");
		set.add("cart");
		set.add("wishlist");
		set.add("orders");
		set.add("order_item");
		set.add("payment");
		set.add("delivery");
		set.add("delivery_history");
		set.add("review");
		set.add("qna");
		set.add("notice");
		set.add("system_config");
		set.add("admin_log");
		set.add("v_sales_daily");
		set.add("v_delivery_status_count");
		set.add("v_admin_order");
		set.add("v_admin_member");
		set.add("v_low_stock");
		ALLOWED_TABLES = Collections.unmodifiableSet(set);
	}

	/* TRUNCATE 순서 — FK 자식부터. member 는 목록에 없다 (ERD § 6.7). */
	private static final String[] TRUNCATE_ORDER = {
		"delivery_history", "delivery", "payment", "review", "order_item", "orders",
		"cart", "wishlist", "qna", "product_image", "product", "category",
		"notice", "admin_log" };

	public static boolean isAllowed(String table) {
		return table != null && ALLOWED_TABLES.contains(table);
	}

	/* ---------------- 데이터 브라우저 ---------------- */

	/*
	  테이블 한 페이지를 열 이름과 함께 통째로 읽는다.

	  컬럼 구성을 미리 알 수 없으므로 ResultSetMetaData 로 받는다.
	  정렬 컬럼도 파라미터로 받지만, 실제 컬럼 목록에 있는 이름만 허용한다 —
	  테이블명과 같은 이유로 ORDER BY 도 인젝션 표면이다 (ERD § 6.5).
	*/
	public Map<String, Object> browse(String table, String sortColumn, boolean desc,
			int page, int size) throws SQLException {
		if (!isAllowed(table)) {
			throw new IllegalArgumentException("허용되지 않은 테이블: " + table);
		}

		List<String> columns = findColumnNames(table);
		String orderBy = "";
		if (sortColumn != null && columns.contains(sortColumn)) {
			orderBy = " ORDER BY `" + sortColumn + "`" + (desc ? " DESC" : " ASC");
		}

		Map<String, Object> result = new LinkedHashMap<String, Object>();
		result.put("table", table);
		result.put("columns", columns);

		Connection conn = null;
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			conn = DBUtil.getConnection();

			pstmt = conn.prepareStatement("SELECT COUNT(*) FROM `" + table + "`");
			rs = pstmt.executeQuery();
			int total = rs.next() ? rs.getInt(1) : 0;
			result.put("total", Integer.valueOf(total));
			DBUtil.close(rs);
			DBUtil.close(pstmt);

			pstmt = conn.prepareStatement("SELECT * FROM `" + table + "`" + orderBy
					+ " LIMIT ? OFFSET ?");
			pstmt.setInt(1, Paging.normalizeSize(size));
			pstmt.setInt(2, Paging.offset(page, size));
			rs = pstmt.executeQuery();

			ResultSetMetaData meta = rs.getMetaData();
			int columnCount = meta.getColumnCount();
			List<List<String>> rows = new ArrayList<List<String>>();
			while (rs.next()) {
				List<String> row = new ArrayList<String>();
				for (int i = 1; i <= columnCount; i++) {
					Object value = rs.getObject(i);
					row.add(value == null ? null : String.valueOf(value));
				}
				rows.add(row);
			}
			result.put("rows", rows);
			result.put("page", Integer.valueOf(Paging.normalizePage(page)));
			result.put("size", Integer.valueOf(Paging.normalizeSize(size)));
			result.put("totalPages", Integer.valueOf(Paging.totalPages(total, size)));
		} finally {
			DBUtil.close(rs, pstmt, conn);
		}
		return result;
	}

	public List<String> findColumnNames(String table) throws SQLException {
		if (!isAllowed(table)) {
			throw new IllegalArgumentException("허용되지 않은 테이블: " + table);
		}
		/* 테이블명은 바인딩할 수 있다 — information_schema 에서는 값이기 때문이다. */
		String sql = "SELECT column_name FROM information_schema.columns"
				   + " WHERE table_schema = DATABASE() AND table_name = ?"
				   + " ORDER BY ordinal_position";
		List<String> list = new ArrayList<String>();
		Connection conn = null;
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			conn = DBUtil.getConnection();
			pstmt = conn.prepareStatement(sql);
			pstmt.setString(1, table);
			rs = pstmt.executeQuery();
			while (rs.next()) {
				list.add(rs.getString(1));
			}
			return list;
		} finally {
			DBUtil.close(rs, pstmt, conn);
		}
	}

	/* ---------------- 설계 보기 (information_schema) ---------------- */

	/*
	  DATABASE() 를 쓰므로 스키마명을 파라미터로 받을 필요가 없다 —
	  인젝션 표면이 아예 생기지 않는다 (ERD § 6.6).
	*/
	public List<Map<String, String>> findSchemaColumns() throws SQLException {
		String sql = "SELECT table_name, column_name, ordinal_position, column_type,"
				   + "       is_nullable, column_key, column_default, extra"
				   + "  FROM information_schema.columns"
				   + " WHERE table_schema = DATABASE()"
				   + " ORDER BY table_name, ordinal_position";
		return queryAsMaps(sql);
	}

	public List<Map<String, String>> findSchemaConstraints() throws SQLException {
		String sql = "SELECT tc.table_name, tc.constraint_name, tc.constraint_type,"
				   + "       kcu.column_name, kcu.referenced_table_name,"
				   + "       kcu.referenced_column_name, rc.delete_rule, rc.update_rule"
				   + "  FROM information_schema.table_constraints tc"
				   + "  LEFT JOIN information_schema.key_column_usage kcu"
				   + "         ON kcu.constraint_name = tc.constraint_name"
				   + "        AND kcu.table_schema    = tc.table_schema"
				   + "        AND kcu.table_name      = tc.table_name"
				   + "  LEFT JOIN information_schema.referential_constraints rc"
				   + "         ON rc.constraint_name   = tc.constraint_name"
				   + "        AND rc.constraint_schema = tc.table_schema"
				   + " WHERE tc.table_schema = DATABASE()"
				   + " ORDER BY tc.table_name, tc.constraint_type, kcu.ordinal_position";
		return queryAsMaps(sql);
	}

	public List<Map<String, String>> findSchemaChecks() throws SQLException {
		String sql = "SELECT cc.constraint_name, cc.check_clause"
				   + "  FROM information_schema.check_constraints cc"
				   + " WHERE cc.constraint_schema = DATABASE()"
				   + " ORDER BY cc.constraint_name";
		return queryAsMaps(sql);
	}

	private List<Map<String, String>> queryAsMaps(String sql) throws SQLException {
		List<Map<String, String>> list = new ArrayList<Map<String, String>>();
		Connection conn = null;
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			conn = DBUtil.getConnection();
			pstmt = conn.prepareStatement(sql);
			rs = pstmt.executeQuery();
			ResultSetMetaData meta = rs.getMetaData();
			int columnCount = meta.getColumnCount();
			while (rs.next()) {
				Map<String, String> row = new LinkedHashMap<String, String>();
				for (int i = 1; i <= columnCount; i++) {
					Object value = rs.getObject(i);
					/* 라벨 대소문자는 MySQL 버전에 따라 달라진다. 소문자로 고정해 화면을 안정시킨다. */
					String label = meta.getColumnLabel(i).toLowerCase();
					row.put(label, value == null ? "" : String.valueOf(value));
				}
				list.add(row);
			}
			return list;
		} finally {
			DBUtil.close(rs, pstmt, conn);
		}
	}

	/* ---------------- 초기화 ---------------- */

	/*
	  전체 비우기 (ERD § 6.7).

	  ★ member 는 TRUNCATE 하지 않는다. DEV 계정이 사라지면 /dev 재진입 경로가
	    없어지고, ADMIN 은 DEV 권한을 줄 수 없어서(ERD § 5) 복구 수단이 사라진다.
	    대신 role = 'USER' 인 행만 DELETE 한다.
	  FK 때문에 TRUNCATE 는 순서가 중요하고, 참조되는 테이블은 거부되므로
	  FOREIGN_KEY_CHECKS 를 잠시 끈다. 반드시 다시 켠다.
	*/
	public void truncateAll(Connection conn) throws SQLException {
		Statement stmt = null;
		try {
			stmt = conn.createStatement();
			stmt.execute("SET FOREIGN_KEY_CHECKS = 0");
			try {
				for (int i = 0; i < TRUNCATE_ORDER.length; i++) {
					stmt.execute("TRUNCATE TABLE `" + TRUNCATE_ORDER[i] + "`");
				}
				/* 일반 회원만 지운다. ADMIN · DEV 는 남긴다. */
				stmt.executeUpdate("DELETE FROM member WHERE role = 'USER'");
			} finally {
				stmt.execute("SET FOREIGN_KEY_CHECKS = 1");
			}
		} finally {
			DBUtil.close(stmt);
		}
	}

	/* 테이블 하나만 비우기 — 허용 목록을 통과한 이름만 받는다. */
	public void truncateTable(Connection conn, String table) throws SQLException {
		if (!isAllowed(table) || "member".equals(table) || table.startsWith("v_")) {
			throw new IllegalArgumentException("비울 수 없는 대상: " + table);
		}
		Statement stmt = null;
		try {
			stmt = conn.createStatement();
			stmt.execute("SET FOREIGN_KEY_CHECKS = 0");
			try {
				stmt.execute("TRUNCATE TABLE `" + table + "`");
			} finally {
				stmt.execute("SET FOREIGN_KEY_CHECKS = 1");
			}
		} finally {
			DBUtil.close(stmt);
		}
	}
}
