package shop.dao;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.naming.InitialContext;
import javax.sql.DataSource;

import shop.model.Page;

/*
  DB 접근 도우미 — 모든 DAO 가 이 클래스로 SQL 을 실행한다.

  - 접속 정보는 META-INF/context.xml 의 jdbc/shop 커넥션 풀에만 있다.
  - SELECT 결과는 컬럼 이름(product_name)과 같은 이름의 필드(productName)에 채운다.
  - 인자에 List 를 넘기면 ? 여러 개로 펼친다 (IN 조건용, marks(n) 과 함께 쓴다).
  - begin() 으로 트랜잭션을 열면 commit() / end() 전까지 같은 커넥션을 쓴다.
*/

public class DB {

	private static DataSource pool;
	private static final ThreadLocal<Connection> TX = new ThreadLocal<Connection>();

	private static Connection connect() throws SQLException {
		if (pool == null) {
			try {
				pool = (DataSource) new InitialContext().lookup("java:comp/env/jdbc/shop");
			} catch (Exception e) {
				throw new SQLException("커넥션 풀(jdbc/shop)을 찾지 못했습니다. context.xml 을 확인하세요.", e);
			}
		}
		return TX.get() != null ? TX.get() : pool.getConnection();
	}

	/* 트랜잭션이 아니면 쓰고 바로 닫는다 */
	private static void release(Connection conn) throws SQLException {
		if (TX.get() == null) {
			conn.close();
		}
	}

	public static void begin() throws SQLException {
		Connection conn = connect();
		conn.setAutoCommit(false);
		TX.set(conn);
	}

	public static void commit() throws SQLException {
		TX.get().commit();
	}

	/* finally 에서 부른다 — commit 하지 않았으면 되돌리고 커넥션을 반납한다 */
	public static void end() {
		Connection conn = TX.get();
		TX.remove();
		try {
			conn.rollback();
			conn.setAutoCommit(true);
			conn.close();
		} catch (SQLException ignore) {
		}
	}

	/* "?, ?, ?" — IN 조건의 자리표시 */
	public static String marks(int n) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < n; i++) {
			sb.append(i == 0 ? "?" : ", ?");
		}
		return sb.toString();
	}

	private static PreparedStatement prepare(Connection conn, String sql, Object[] args, boolean keys)
			throws SQLException {
		PreparedStatement ps = keys ? conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)
				: conn.prepareStatement(sql);
		int i = 1;
		for (Object arg : args) {
			if (arg instanceof Collection) {
				for (Object one : (Collection<?>) arg) {
					ps.setObject(i++, one);
				}
			} else {
				ps.setObject(i++, arg);
			}
		}
		return ps;
	}

	/* INSERT · UPDATE · DELETE — 바뀐 행 수 */
	public static int update(String sql, Object... args) throws SQLException {
		Connection conn = connect();
		try (PreparedStatement ps = prepare(conn, sql, args, false)) {
			return ps.executeUpdate();
		} finally {
			release(conn);
		}
	}

	/* INSERT — 새로 생긴 키(AUTO_INCREMENT) */
	public static int insert(String sql, Object... args) throws SQLException {
		Connection conn = connect();
		try (PreparedStatement ps = prepare(conn, sql, args, true)) {
			ps.executeUpdate();
			ResultSet rs = ps.getGeneratedKeys();
			return rs.next() ? rs.getInt(1) : 0;
		} finally {
			release(conn);
		}
	}

	/* 여러 행 → 객체 목록 */
	public static <T> List<T> list(Class<T> type, String sql, Object... args) throws SQLException {
		List<T> list = new ArrayList<T>();
		Connection conn = connect();
		try (PreparedStatement ps = prepare(conn, sql, args, false); ResultSet rs = ps.executeQuery()) {
			while (rs.next()) {
				list.add(toObject(type, rs));
			}
		} finally {
			release(conn);
		}
		return list;
	}

	/* 한 행 → 객체 (없으면 null) */
	public static <T> T one(Class<T> type, String sql, Object... args) throws SQLException {
		List<T> list = list(type, sql, args);
		return list.isEmpty() ? null : list.get(0);
	}

	/* 여러 행 → Map 목록 (키는 camelCase 컬럼 이름) — 화면에 보여주기만 하는 표에 쓴다 */
	public static List<Map<String, Object>> maps(String sql, Object... args) throws SQLException {
		List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
		Connection conn = connect();
		try (PreparedStatement ps = prepare(conn, sql, args, false); ResultSet rs = ps.executeQuery()) {
			ResultSetMetaData meta = rs.getMetaData();
			while (rs.next()) {
				Map<String, Object> row = new LinkedHashMap<String, Object>();
				for (int i = 1; i <= meta.getColumnCount(); i++) {
					int type = meta.getColumnType(i);
					Object value = type == Types.TIMESTAMP ? rs.getTimestamp(i)
							: type == Types.DATE ? rs.getDate(i) : rs.getObject(i);
					row.put(camel(meta.getColumnLabel(i)), value);
				}
				list.add(row);
			}
		} finally {
			release(conn);
		}
		return list;
	}

	/* 여러 행 → 글자 그대로의 Map 목록 (키는 소문자 컬럼 이름) — dev 데이터 브라우저 · 스키마 화면용 */
	public static List<Map<String, String>> raw(String sql, Object... args) throws SQLException {
		List<Map<String, String>> list = new ArrayList<Map<String, String>>();
		Connection conn = connect();
		try (PreparedStatement ps = prepare(conn, sql, args, false); ResultSet rs = ps.executeQuery()) {
			ResultSetMetaData meta = rs.getMetaData();
			while (rs.next()) {
				Map<String, String> row = new LinkedHashMap<String, String>();
				for (int i = 1; i <= meta.getColumnCount(); i++) {
					Object value = rs.getObject(i);
					row.put(meta.getColumnLabel(i).toLowerCase(), value == null ? null : String.valueOf(value));
				}
				list.add(row);
			}
		} finally {
			release(conn);
		}
		return list;
	}

	/* 첫 행 첫 칸 — COUNT(*) 같은 숫자 하나 (없으면 0) */
	public static int number(String sql, Object... args) throws SQLException {
		String value = text(sql, args);
		return value == null ? 0 : (int) Double.parseDouble(value);
	}

	/* 첫 행 첫 칸 — 문자열 하나 (없으면 null) */
	public static String text(String sql, Object... args) throws SQLException {
		Connection conn = connect();
		try (PreparedStatement ps = prepare(conn, sql, args, false); ResultSet rs = ps.executeQuery()) {
			return rs.next() ? rs.getString(1) : null;
		} finally {
			release(conn);
		}
	}

	/*
	  목록 한 페이지 — "SELECT COUNT(*) " + from 으로 전체 건수를 세고,
	  select + from + order 에 LIMIT 을 붙여 그 페이지만 읽는다.
	*/
	public static <T> Page<T> page(Class<T> type, String select, String from, String order,
			int page, int size, Object... args) throws SQLException {
		int total = number("SELECT COUNT(*) " + from, args);
		int limit = size < 1 ? 10 : Math.min(size, 100);
		Object[] all = new Object[args.length + 2];
		System.arraycopy(args, 0, all, 0, args.length);
		all[args.length] = limit;
		all[args.length + 1] = (Math.max(page, 1) - 1) * limit;
		List<T> list = list(type, select + " " + from + " " + order + " LIMIT ? OFFSET ?", all);
		return new Page<T>(list, page, size, total);
	}

	/* product_name → productName */
	private static String camel(String column) {
		StringBuilder sb = new StringBuilder();
		boolean up = false;
		for (char ch : column.toLowerCase().toCharArray()) {
			if (ch == '_') {
				up = true;
			} else {
				sb.append(up ? Character.toUpperCase(ch) : ch);
				up = false;
			}
		}
		return sb.toString();
	}

	/* 한 행을 객체로 — 컬럼과 이름이 같은 필드에 타입에 맞춰 넣는다 */
	private static <T> T toObject(Class<T> type, ResultSet rs) throws SQLException {
		try {
			T obj = type.getDeclaredConstructor().newInstance();
			ResultSetMetaData meta = rs.getMetaData();
			for (int i = 1; i <= meta.getColumnCount(); i++) {
				Field f;
				try {
					f = type.getDeclaredField(camel(meta.getColumnLabel(i)));
				} catch (NoSuchFieldException e) {
					continue;
				}
				f.setAccessible(true);
				Class<?> t = f.getType();
				if (t == int.class) {
					f.setInt(obj, rs.getInt(i));
				} else if (t == double.class) {
					f.setDouble(obj, rs.getDouble(i));
				} else if (t == boolean.class) {
					f.setBoolean(obj, rs.getInt(i) != 0);
				} else if (t == Integer.class) {
					int v = rs.getInt(i);
					f.set(obj, rs.wasNull() ? null : Integer.valueOf(v));
				} else if (t == Timestamp.class) {
					f.set(obj, rs.getTimestamp(i));
				} else if (t == Date.class) {
					f.set(obj, rs.getDate(i));
				} else if (t == String.class) {
					f.set(obj, rs.getString(i));
				}
			}
			return obj;
		} catch (ReflectiveOperationException e) {
			throw new SQLException(type.getSimpleName() + " 에 값을 채우지 못했습니다.", e);
		}
	}
}
