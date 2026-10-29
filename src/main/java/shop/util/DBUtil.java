package shop.util;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

/*
  DB 커넥션 풀 접근 지점.

  접속 정보는 WebContents/META-INF/context.xml 의 Resource(jdbc/shop) 에만 둔다.
  소스에는 URL · 계정 · 비밀번호를 적지 않는다. (CLAUDE.md § 6)
*/
public class DBUtil {

	private static final String JNDI_NAME = "java:comp/env/jdbc/shop";

	private static DataSource dataSource;

	/* 커넥션 풀은 요청마다 찾지 않는다. 최초 1회만 JNDI 조회한다. */
	static {
		try {
			Context init = new InitialContext();
			dataSource = (DataSource) init.lookup(JNDI_NAME);
		} catch (NamingException e) {
			throw new ExceptionInInitializerError(
				"커넥션 풀(" + JNDI_NAME + ") 을 찾지 못했다. context.xml 을 확인한다. : " + e.getMessage());
		}
	}

	private DBUtil() {
	}

	public static Connection getConnection() throws SQLException {
		return dataSource.getConnection();
	}

	/*
	  트랜잭션을 시작한다.

	  풀에서 꺼낸 커넥션은 autoCommit = true 다. 이 값을 바꿨으면 반드시
	  release() 로 되돌려 반납해야 한다 — 되돌리지 않으면 다음 요청이
	  그 커넥션을 빌려 쓸 때 커밋이 일어나지 않는다. (ERD § 6)
	*/
	public static Connection beginTransaction() throws SQLException {
		Connection conn = getConnection();
		conn.setAutoCommit(false);
		return conn;
	}

	public static void commit(Connection conn) throws SQLException {
		if (conn != null) {
			conn.commit();
		}
	}

	public static void rollback(Connection conn) {
		if (conn != null) {
			try {
				conn.rollback();
			} catch (SQLException e) {
				/* 롤백 실패는 더 할 수 있는 일이 없다. 원인 예외를 가리지 않도록 삼킨다. */
			}
		}
	}

	/* 트랜잭션용 커넥션 반납 — autoCommit 복원이 핵심이다. */
	public static void release(Connection conn) {
		if (conn != null) {
			try {
				conn.setAutoCommit(true);
			} catch (SQLException e) {
				/* 복원 실패한 커넥션은 닫아서 풀에서 버리게 한다. */
			}
			close(conn);
		}
	}

	public static void close(Connection conn) {
		if (conn != null) {
			try {
				conn.close();
			} catch (SQLException e) {
			}
		}
	}

	public static void close(Statement stmt) {
		if (stmt != null) {
			try {
				stmt.close();
			} catch (SQLException e) {
			}
		}
	}

	public static void close(ResultSet rs) {
		if (rs != null) {
			try {
				rs.close();
			} catch (SQLException e) {
			}
		}
	}

	public static void close(Statement stmt, Connection conn) {
		close(stmt);
		close(conn);
	}

	public static void close(ResultSet rs, Statement stmt, Connection conn) {
		close(rs);
		close(stmt);
		close(conn);
	}
}
