package shop.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import shop.dto.DeliveryStatusCountDTO;
import shop.dto.LowStockDTO;
import shop.dto.SalesDailyDTO;
import shop.util.DBUtil;

/*
  관리자 대시보드가 쓰는 뷰 3개.
      v_sales_daily · v_delivery_status_count · v_low_stock
*/
public class StatsDAO {

	public List<SalesDailyDTO> findSalesDaily(int days) throws SQLException {
		String sql = "SELECT sales_date, order_count, goods_amount, delivery_amount, total_amount"
				   + "  FROM v_sales_daily"
				   + " ORDER BY sales_date DESC LIMIT ?";
		List<SalesDailyDTO> list = new ArrayList<SalesDailyDTO>();
		Connection conn = null;
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			conn = DBUtil.getConnection();
			pstmt = conn.prepareStatement(sql);
			pstmt.setInt(1, days);
			rs = pstmt.executeQuery();
			while (rs.next()) {
				SalesDailyDTO dto = new SalesDailyDTO();
				dto.setSalesDate(rs.getDate("sales_date"));
				dto.setOrderCount(rs.getInt("order_count"));
				dto.setGoodsAmount(rs.getInt("goods_amount"));
				dto.setDeliveryAmount(rs.getInt("delivery_amount"));
				dto.setTotalAmount(rs.getInt("total_amount"));
				list.add(dto);
			}
			return list;
		} finally {
			DBUtil.close(rs, pstmt, conn);
		}
	}

	/*
	  배송 상태별 대기 건수.

	  뷰는 건수가 0 인 상태의 행을 내보내지 않는다. 대시보드는 4단계를 항상
	  보여줘야 하므로 여기서 4개를 0 으로 깔고 조회 결과로 덮어쓴다
	  (schema.sql v_delivery_status_count 주석).
	*/
	public List<DeliveryStatusCountDTO> findDeliveryStatusCount() throws SQLException {
		Map<String, Integer> counts = new LinkedHashMap<String, Integer>();
		for (int i = 0; i < DeliveryDAO.FLOW.length; i++) {
			counts.put(DeliveryDAO.FLOW[i], Integer.valueOf(0));
		}

		String sql = "SELECT delivery_status, order_count FROM v_delivery_status_count";
		Connection conn = null;
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			conn = DBUtil.getConnection();
			pstmt = conn.prepareStatement(sql);
			rs = pstmt.executeQuery();
			while (rs.next()) {
				counts.put(rs.getString("delivery_status"), Integer.valueOf(rs.getInt("order_count")));
			}
		} finally {
			DBUtil.close(rs, pstmt, conn);
		}

		List<DeliveryStatusCountDTO> list = new ArrayList<DeliveryStatusCountDTO>();
		for (Map.Entry<String, Integer> entry : counts.entrySet()) {
			DeliveryStatusCountDTO dto = new DeliveryStatusCountDTO();
			dto.setDeliveryStatus(entry.getKey());
			dto.setOrderCount(entry.getValue().intValue());
			list.add(dto);
		}
		return list;
	}

	/*
	  재고 부족 상품.

	  선택 기능이 아니다. 노출 규칙 때문에 stock = 0 인 상품은 고객 화면에서
	  조용히 사라지므로, 관리자가 그 사실을 알 수 있는 유일한 경로다
	  (schema.sql v_low_stock 주석).
	*/
	public List<LowStockDTO> findLowStock() throws SQLException {
		String sql = "SELECT product_id, name, stock, status, category_name"
				   + "  FROM v_low_stock ORDER BY stock, product_id";
		List<LowStockDTO> list = new ArrayList<LowStockDTO>();
		Connection conn = null;
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			conn = DBUtil.getConnection();
			pstmt = conn.prepareStatement(sql);
			rs = pstmt.executeQuery();
			while (rs.next()) {
				LowStockDTO dto = new LowStockDTO();
				dto.setProductId(rs.getInt("product_id"));
				dto.setName(rs.getString("name"));
				dto.setStock(rs.getInt("stock"));
				dto.setStatus(rs.getString("status"));
				dto.setCategoryName(rs.getString("category_name"));
				list.add(dto);
			}
			return list;
		} finally {
			DBUtil.close(rs, pstmt, conn);
		}
	}

	/* 대시보드 상단 숫자 — 한 번의 왕복으로 받는다. */
	public Map<String, Integer> findSummary() throws SQLException {
		String sql = "SELECT"
				+ " (SELECT COUNT(*) FROM member  WHERE status = 'ACTIVE')            AS member_count,"
				+ " (SELECT COUNT(*) FROM product WHERE status = '판매중')            AS product_count,"
				+ " (SELECT COUNT(*) FROM orders  WHERE order_status = '정상')        AS order_count,"
				+ " (SELECT COUNT(*) FROM orders  WHERE order_status = '정상'"
				+ "    AND DATE(ordered_at) = CURDATE())                              AS today_order_count,"
				+ " (SELECT IFNULL(SUM(total_price + delivery_fee), 0) FROM orders"
				+ "   WHERE order_status = '정상' AND DATE(ordered_at) = CURDATE())   AS today_amount,"
				+ " (SELECT COUNT(*) FROM qna WHERE answered_at IS NULL)              AS qna_waiting";
		Map<String, Integer> map = new LinkedHashMap<String, Integer>();
		Connection conn = null;
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			conn = DBUtil.getConnection();
			pstmt = conn.prepareStatement(sql);
			rs = pstmt.executeQuery();
			if (rs.next()) {
				map.put("memberCount", Integer.valueOf(rs.getInt("member_count")));
				map.put("productCount", Integer.valueOf(rs.getInt("product_count")));
				map.put("orderCount", Integer.valueOf(rs.getInt("order_count")));
				map.put("todayOrderCount", Integer.valueOf(rs.getInt("today_order_count")));
				map.put("todayAmount", Integer.valueOf(rs.getInt("today_amount")));
				map.put("qnaWaiting", Integer.valueOf(rs.getInt("qna_waiting")));
			}
			return map;
		} finally {
			DBUtil.close(rs, pstmt, conn);
		}
	}
}
