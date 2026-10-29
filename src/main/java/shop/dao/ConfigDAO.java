package shop.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import shop.dto.SystemConfigDTO;
import shop.util.DBUtil;

/*
  system_config — 전역 설정.

  키는 고정이라 INSERT · DELETE 가 없다 (CRUD.md § 4). 읽기와 값 변경만 한다.
*/
public class ConfigDAO {

	public static final String SHOP_STATUS = "SHOP_STATUS";
	public static final String DELIVERY_FEE = "DELIVERY_FEE";
	public static final String FREE_SHIPPING_MIN = "FREE_SHIPPING_MIN";
	public static final String LOW_STOCK_THRESHOLD = "LOW_STOCK_THRESHOLD";

	public String findValue(String key) throws SQLException {
		String sql = "SELECT config_value FROM system_config WHERE config_key = ?";
		Connection conn = null;
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			conn = DBUtil.getConnection();
			pstmt = conn.prepareStatement(sql);
			pstmt.setString(1, key);
			rs = pstmt.executeQuery();
			return rs.next() ? rs.getString(1) : null;
		} finally {
			DBUtil.close(rs, pstmt, conn);
		}
	}

	public int findInt(String key, int defaultValue) throws SQLException {
		String value = findValue(key);
		if (value == null) {
			return defaultValue;
		}
		try {
			return Integer.parseInt(value.trim());
		} catch (NumberFormatException e) {
			return defaultValue;
		}
	}

	/* 설정 전체 — /dev 상태 컨트롤러가 한 화면에 모두 보여준다. */
	public List<SystemConfigDTO> findAll() throws SQLException {
		String sql = "SELECT config_key, config_value, description, updated_at"
				   + "  FROM system_config ORDER BY config_key";
		List<SystemConfigDTO> list = new ArrayList<SystemConfigDTO>();
		Connection conn = null;
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			conn = DBUtil.getConnection();
			pstmt = conn.prepareStatement(sql);
			rs = pstmt.executeQuery();
			while (rs.next()) {
				SystemConfigDTO dto = new SystemConfigDTO();
				dto.setConfigKey(rs.getString("config_key"));
				dto.setConfigValue(rs.getString("config_value"));
				dto.setDescription(rs.getString("description"));
				dto.setUpdatedAt(rs.getTimestamp("updated_at"));
				list.add(dto);
			}
			return list;
		} finally {
			DBUtil.close(rs, pstmt, conn);
		}
	}

	/* 한 번의 조회로 필요한 설정을 모두 받는다 — 헤더가 매 요청마다 쓴다. */
	public Map<String, String> findAllAsMap() throws SQLException {
		Map<String, String> map = new LinkedHashMap<String, String>();
		for (SystemConfigDTO dto : findAll()) {
			map.put(dto.getConfigKey(), dto.getConfigValue());
		}
		return map;
	}

	public int updateValue(String key, String value) throws SQLException {
		Connection conn = null;
		try {
			conn = DBUtil.getConnection();
			return updateValue(conn, key, value);
		} finally {
			DBUtil.close(conn);
		}
	}

	/* 트랜잭션 참여용 — 커넥션을 밖에서 받는다. */
	public int updateValue(Connection conn, String key, String value) throws SQLException {
		String sql = "UPDATE system_config SET config_value = ? WHERE config_key = ?";
		PreparedStatement pstmt = null;
		try {
			pstmt = conn.prepareStatement(sql);
			pstmt.setString(1, value);
			pstmt.setString(2, key);
			return pstmt.executeUpdate();
		} finally {
			DBUtil.close(pstmt);
		}
	}
}
