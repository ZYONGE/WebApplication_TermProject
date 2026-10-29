package shop.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import shop.dto.DeliveryDTO;
import shop.dto.DeliveryHistoryDTO;
import shop.util.DBUtil;

/*
  delivery · delivery_history — 배송.

  정상 경로는 단방향이다 (ERD § 4).
      결제완료 → 상품준비 → 배송중 → 배송완료
  역방향 · 건너뛰기는 /dev 만 할 수 있고, 그때 is_forced = 'Y' 로 구분된다.

  delivery_history 는 append-only 다. UPDATE · DELETE 가 없다 (CRUD.md § 4).
*/
public class DeliveryDAO {

	/* 상태 선언 순서 = 생애주기 순서. 전이 판정을 이 배열 하나로 끝낸다. */
	public static final String[] FLOW = { "결제완료", "상품준비", "배송중", "배송완료" };

	private static final String COLUMNS =
		"delivery_id, order_id, status, courier, tracking_no,"
	  + " paid_at, ready_at, shipped_at, delivered_at";

	public static int indexOf(String status) {
		for (int i = 0; i < FLOW.length; i++) {
			if (FLOW[i].equals(status)) {
				return i;
			}
		}
		return -1;
	}

	public static String nextOf(String status) {
		int i = indexOf(status);
		return (i < 0 || i >= FLOW.length - 1) ? null : FLOW[i + 1];
	}

	/* 상태에 대응하는 시각 컬럼 — 전이와 강제 변경이 같은 표를 본다. */
	private static String timeColumn(String status) {
		if ("결제완료".equals(status)) {
			return "paid_at";
		}
		if ("상품준비".equals(status)) {
			return "ready_at";
		}
		if ("배송중".equals(status)) {
			return "shipped_at";
		}
		if ("배송완료".equals(status)) {
			return "delivered_at";
		}
		return null;
	}

	/* ---------------- 생성 ---------------- */

	public int insert(Connection conn, int orderId) throws SQLException {
		String sql = "INSERT INTO delivery (order_id, status, paid_at)"
				   + " VALUES (?, '결제완료', NOW())";
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
			pstmt.setInt(1, orderId);
			pstmt.executeUpdate();
			rs = pstmt.getGeneratedKeys();
			return rs.next() ? rs.getInt(1) : 0;
		} finally {
			DBUtil.close(rs);
			DBUtil.close(pstmt);
		}
	}

	/* ---------------- 조회 ---------------- */

	public DeliveryDTO findByOrder(int orderId) throws SQLException {
		String sql = "SELECT " + COLUMNS + " FROM delivery WHERE order_id = ?";
		return findOne(sql, orderId);
	}

	public DeliveryDTO findById(int deliveryId) throws SQLException {
		String sql = "SELECT " + COLUMNS + " FROM delivery WHERE delivery_id = ?";
		return findOne(sql, deliveryId);
	}

	private DeliveryDTO findOne(String sql, int id) throws SQLException {
		Connection conn = null;
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			conn = DBUtil.getConnection();
			pstmt = conn.prepareStatement(sql);
			pstmt.setInt(1, id);
			rs = pstmt.executeQuery();
			return rs.next() ? map(rs) : null;
		} finally {
			DBUtil.close(rs, pstmt, conn);
		}
	}

	/* ---------------- 정상 전이 (관리자) ---------------- */

	/*
	  다음 단계로 (ERD § 6.2).

	  WHERE status = <직전 상태> 가 동시 클릭과 잘못된 전이를 함께 막는다.
	  영향 행이 0 이면 호출부가 롤백한다.

	  배송중으로 넘어갈 때는 courier · tracking_no 를 같은 문장에서 채운다 —
	  chk_delivery_tracking CHECK 가 운송장 없는 배송중을 거부하기 때문이다.
	*/
	public int moveNext(Connection conn, int deliveryId, String fromStatus, String toStatus,
			String courier, String trackingNo) throws SQLException {
		String column = timeColumn(toStatus);
		if (column == null) {
			return 0;
		}
		StringBuilder sql = new StringBuilder("UPDATE delivery SET status = ?, ");
		sql.append(column).append(" = NOW()");
		boolean withTracking = "배송중".equals(toStatus);
		if (withTracking) {
			sql.append(", courier = ?, tracking_no = ?");
		}
		sql.append(" WHERE delivery_id = ? AND status = ?");

		PreparedStatement pstmt = null;
		try {
			pstmt = conn.prepareStatement(sql.toString());
			int i = 1;
			pstmt.setString(i++, toStatus);
			if (withTracking) {
				pstmt.setString(i++, courier);
				pstmt.setString(i++, trackingNo);
			}
			pstmt.setInt(i++, deliveryId);
			pstmt.setString(i, fromStatus);
			return pstmt.executeUpdate();
		} finally {
			DBUtil.close(pstmt);
		}
	}

	/* ---------------- 강제 전이 (/dev) ---------------- */

	/*
	  임의 상태로 강제 이동 (ERD § 6.4).

	  - 목표 단계의 시각 컬럼을 채운다.
	  - 목표보다 뒤 단계의 시각 컬럼은 NULL 로 비운다.
	  - 배송중 이전으로 되돌리면 courier · tracking_no 도 비운다.
	    (안 비우면 "운송장은 있는데 상품준비" 라는 모순이 남는다)
	  - 앞으로 건너뛰는 경우 운송장이 필요하면 호출부가 값을 넘겨야 한다.
	    없으면 chk_delivery_tracking 에 걸려 SQLException 이 난다 — 규칙을 빠뜨린
	    코드를 즉시 드러내는 장치다 (schema.sql delivery 주석).
	*/
	public int forceStatus(Connection conn, int deliveryId, String toStatus,
			String courier, String trackingNo) throws SQLException {
		int target = indexOf(toStatus);
		if (target < 0) {
			return 0;
		}

		StringBuilder sql = new StringBuilder("UPDATE delivery SET status = ?");
		List<Object> args = new ArrayList<Object>();
		args.add(toStatus);

		for (int i = 0; i < FLOW.length; i++) {
			String column = timeColumn(FLOW[i]);
			if (i <= target) {
				/* 지나온 단계는 비어 있으면 지금 시각으로 메운다 — 타임라인에 구멍을 남기지 않는다. */
				sql.append(", ").append(column).append(" = IFNULL(").append(column).append(", NOW())");
			} else {
				sql.append(", ").append(column).append(" = NULL");
			}
		}

		if (target >= indexOf("배송중")) {
			sql.append(", courier = ?, tracking_no = ?");
			args.add(courier);
			args.add(trackingNo);
		} else {
			sql.append(", courier = NULL, tracking_no = NULL");
		}
		sql.append(" WHERE delivery_id = ?");
		args.add(Integer.valueOf(deliveryId));

		PreparedStatement pstmt = null;
		try {
			pstmt = conn.prepareStatement(sql.toString());
			int i = 1;
			for (Object arg : args) {
				pstmt.setObject(i++, arg);
			}
			return pstmt.executeUpdate();
		} finally {
			DBUtil.close(pstmt);
		}
	}

	/* ---------------- delivery_history ---------------- */

	/*
	  이력 추가.

	  prev_status 가 new_status 와 같으면 chk_delivery_history_changed 에 걸린다.
	  변화 없는 이력 행은 애초에 만들지 않는 것이 맞으므로 여기서 걸러 낸다.
	*/
	public int insertHistory(Connection conn, int deliveryId, String prevStatus,
			String newStatus, Integer changedBy, boolean forced) throws SQLException {
		if (newStatus == null || newStatus.equals(prevStatus)) {
			return 0;
		}
		String sql = "INSERT INTO delivery_history"
				   + " (delivery_id, prev_status, new_status, changed_by, is_forced)"
				   + " VALUES (?, ?, ?, ?, ?)";
		PreparedStatement pstmt = null;
		try {
			pstmt = conn.prepareStatement(sql);
			pstmt.setInt(1, deliveryId);
			if (prevStatus == null) {
				pstmt.setNull(2, java.sql.Types.VARCHAR);
			} else {
				pstmt.setString(2, prevStatus);
			}
			pstmt.setString(3, newStatus);
			if (changedBy == null) {
				pstmt.setNull(4, java.sql.Types.INTEGER);
			} else {
				pstmt.setInt(4, changedBy.intValue());
			}
			pstmt.setString(5, forced ? "Y" : "N");
			return pstmt.executeUpdate();
		} finally {
			DBUtil.close(pstmt);
		}
	}

	public List<DeliveryHistoryDTO> findHistory(int deliveryId) throws SQLException {
		String sql = "SELECT h.history_id, h.delivery_id, h.prev_status, h.new_status,"
				   + "       h.changed_by, h.is_forced, h.changed_at,"
				   + "       m.name AS changed_by_name"
				   + "  FROM delivery_history h"
				   + "  LEFT JOIN member m ON m.member_id = h.changed_by"
				   + " WHERE h.delivery_id = ?"
				   + " ORDER BY h.changed_at, h.history_id";
		List<DeliveryHistoryDTO> list = new ArrayList<DeliveryHistoryDTO>();
		Connection conn = null;
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			conn = DBUtil.getConnection();
			pstmt = conn.prepareStatement(sql);
			pstmt.setInt(1, deliveryId);
			rs = pstmt.executeQuery();
			while (rs.next()) {
				DeliveryHistoryDTO dto = new DeliveryHistoryDTO();
				dto.setHistoryId(rs.getInt("history_id"));
				dto.setDeliveryId(rs.getInt("delivery_id"));
				dto.setPrevStatus(rs.getString("prev_status"));
				dto.setNewStatus(rs.getString("new_status"));
				int changedBy = rs.getInt("changed_by");
				dto.setChangedBy(rs.wasNull() ? null : Integer.valueOf(changedBy));
				dto.setChangedByName(rs.getString("changed_by_name"));
				dto.setIsForced(rs.getString("is_forced"));
				dto.setChangedAt(rs.getTimestamp("changed_at"));
				list.add(dto);
			}
			return list;
		} finally {
			DBUtil.close(rs, pstmt, conn);
		}
	}

	private DeliveryDTO map(ResultSet rs) throws SQLException {
		DeliveryDTO dto = new DeliveryDTO();
		dto.setDeliveryId(rs.getInt("delivery_id"));
		dto.setOrderId(rs.getInt("order_id"));
		dto.setStatus(rs.getString("status"));
		dto.setCourier(rs.getString("courier"));
		dto.setTrackingNo(rs.getString("tracking_no"));
		dto.setPaidAt(rs.getTimestamp("paid_at"));
		dto.setReadyAt(rs.getTimestamp("ready_at"));
		dto.setShippedAt(rs.getTimestamp("shipped_at"));
		dto.setDeliveredAt(rs.getTimestamp("delivered_at"));
		return dto;
	}
}
