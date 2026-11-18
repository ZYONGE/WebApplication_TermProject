package shop.dao;

import java.sql.Date;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import shop.model.Page;

/* 관리 기능 — 감사 로그 (admin_log) · 대시보드 통계 · dev 데이터 도구 */

public class AdminDAO {

	/* ---------- 감사 로그 ---------- */

	public static final List<String> ACTIONS = Arrays.asList(
		"PRODUCT_CREATE", "PRODUCT_UPDATE", "PRODUCT_DELETE", "TRACK_ITEM_SET",
		"CONFLICT_CREATE", "CONFLICT_UPDATE", "CONFLICT_DELETE",
		"CATEGORY_CREATE", "CATEGORY_UPDATE", "CATEGORY_DELETE",
		"DELIVERY_NEXT_STEP", "DELIVERY_TRACKING_SET", "ORDER_CANCEL", "MEMBER_LEAVE", "QNA_ANSWER",
		"NOTICE_CREATE", "NOTICE_UPDATE", "NOTICE_DELETE", "REVIEW_DELETE",
		"FORCE_DELIVERY_STATUS", "FORCE_ORDER_STATUS", "FORCE_PAY_STATUS", "FORCE_STOCK_SET",
		"SHOP_STATUS_TOGGLE", "CONFIG_UPDATE", "SEED_GENERATE", "SEED_TRUNCATE_ALL", "SEED_TRUNCATE_TABLE",
		"LOGIN_AS", "LOGIN_AS_RETURN");

	/* 계정 전환 중인 요청이면 원래 DEV 의 id — BaseServlet 이 요청마다 넣고 뺀다 */
	public static final ThreadLocal<Integer> IMPERSONATOR = new ThreadLocal<Integer>();

	/* 로그 남기기 — mode 는 ADMIN / DEV. 계정 전환 중이면 detail 끝에 impersonated_by 를 붙인다 */
	public static void log(int memberId, String mode, String action, String table, Integer targetId, String detail)
			throws SQLException {
		Integer dev = IMPERSONATOR.get();
		if (dev != null) {
			detail = (detail == null ? "" : detail + " / ") + "impersonated_by=" + dev;
		}
		if (detail != null && detail.length() > 500) {
			detail = detail.substring(0, 500);
		}
		DB.update("INSERT INTO admin_log (member_id, mode, action, target_table, target_id, detail) VALUES (?, ?, ?, ?, ?, ?)",
				memberId, mode, action, table, targetId, detail);
	}

	public static void adminLog(int adminId, String action, String table, Integer targetId, String detail)
			throws SQLException {
		log(adminId, "ADMIN", action, table, targetId, detail);
	}

	public static void devLog(int devId, String action, String table, Integer targetId, String detail)
			throws SQLException {
		log(devId, "DEV", action, table, targetId, detail);
	}

	/* 로그 목록 (dev) — 모드 · 행위 · 대상 테이블 · 기간 필터 */
	public static Page<Map<String, Object>> logs(String mode, String action, String table,
			String fromDate, String toDate, int page) throws SQLException {
		StringBuilder where = new StringBuilder("FROM admin_log l JOIN member m ON m.member_id = l.member_id WHERE 1 = 1");
		List<Object> args = new ArrayList<Object>();
		if ("ADMIN".equals(mode) || "DEV".equals(mode)) {
			where.append(" AND l.mode = ?");
			args.add(mode);
		}
		if (action != null && action.length() > 0) {
			where.append(" AND l.action = ?");
			args.add(action);
		}
		if (table != null && table.length() > 0) {
			where.append(" AND l.target_table = ?");
			args.add(table);
		}
		if (fromDate != null && fromDate.length() > 0) {
			where.append(" AND l.created_at >= ?");
			args.add(fromDate + " 00:00:00");
		}
		if (toDate != null && toDate.length() > 0) {
			where.append(" AND l.created_at <= ?");
			args.add(toDate + " 23:59:59");
		}
		int total = DB.number("SELECT COUNT(*) " + where, args);
		List<Map<String, Object>> list = DB.maps("SELECT l.*, m.login_id AS member_name " + where
				+ " ORDER BY l.created_at DESC, l.log_id DESC LIMIT 10 OFFSET ?", args, (Math.max(page, 1) - 1) * 10);
		return new Page<Map<String, Object>>(list, page, 10, total);
	}

	/* ---------- 대시보드 ---------- */

	public static Map<String, Object> summary() throws SQLException {
		return DB.maps("SELECT"
			+ " (SELECT COUNT(*) FROM member WHERE status = 'ACTIVE') AS member_count,"
			+ " (SELECT COUNT(*) FROM product WHERE status = '판매중' AND stock > 0) AS product_count,"
			+ " (SELECT COUNT(*) FROM product WHERE status = '판매중' AND stock = 0) AS soldout_count,"
			+ " (SELECT COUNT(*) FROM orders WHERE order_status = '정상') AS order_count,"
			+ " (SELECT COUNT(*) FROM orders WHERE order_status = '정상' AND DATE(ordered_at) = CURDATE()) AS today_order_count,"
			+ " (SELECT IFNULL(SUM(total_price + delivery_fee), 0) FROM orders"
			+ "   WHERE order_status = '정상' AND DATE(ordered_at) = CURDATE()) AS today_amount,"
			+ " (SELECT COUNT(*) FROM qna WHERE answered_at IS NULL) AS qna_waiting").get(0);
	}

	/* 최근 n일 매출 — 오늘부터 거꾸로, 주문이 없는 날은 0 */
	public static List<Map<String, Object>> salesDaily(int days) throws SQLException {
		Map<String, Map<String, Object>> byDate = new LinkedHashMap<String, Map<String, Object>>();
		for (Map<String, Object> row : DB.maps("SELECT * FROM v_sales_daily WHERE sales_date >= CURDATE() - INTERVAL ? DAY",
				days - 1)) {
			byDate.put(String.valueOf(row.get("salesDate")), row);
		}
		List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
		for (int i = 0; i < days; i++) {
			Date day = Date.valueOf(LocalDate.now().minusDays(i));
			Map<String, Object> row = byDate.get(day.toString());
			if (row == null) {
				row = new LinkedHashMap<String, Object>();
				row.put("salesDate", day);
				row.put("orderCount", 0);
				row.put("goodsAmount", 0);
				row.put("deliveryAmount", 0);
				row.put("totalAmount", 0);
			}
			list.add(row);
		}
		return list;
	}

	/* 배송 단계별 주문 수 — 0건인 단계도 보여준다 */
	public static List<Map<String, Object>> deliveryCounts() throws SQLException {
		Map<String, Object> counts = new LinkedHashMap<String, Object>();
		for (String step : OrderDAO.FLOW) {
			counts.put(step, 0);
		}
		for (Map<String, Object> row : DB.maps("SELECT * FROM v_delivery_status_count")) {
			counts.put((String) row.get("deliveryStatus"), row.get("orderCount"));
		}
		List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
		for (Map.Entry<String, Object> e : counts.entrySet()) {
			Map<String, Object> row = new LinkedHashMap<String, Object>();
			row.put("deliveryStatus", e.getKey());
			row.put("orderCount", e.getValue());
			list.add(row);
		}
		return list;
	}

	public static List<Map<String, Object>> lowStock() throws SQLException {
		return DB.maps("SELECT * FROM v_low_stock ORDER BY stock, product_id");
	}

	/* ---------- dev 데이터 도구 ---------- */

	/* dev 화면에서 열어 볼 수 있는 테이블 · 뷰 (v_ 로 시작하면 뷰) */
	public static final List<String> TABLES = Arrays.asList(
		"member", "address", "category", "product", "track_item", "supplement_conflict", "product_image",
		"cart", "wishlist", "combo", "combo_item", "orders", "order_item", "payment", "delivery",
		"delivery_history", "review", "qna", "notice", "system_config", "admin_log",
		"v_sales_daily", "v_delivery_status_count", "v_admin_order", "v_admin_member", "v_low_stock");

	/* 전체 초기화 때 비우는 순서 (member 는 USER 만 지운다) */
	public static final String[] TRUNCATE_ORDER = {
		"delivery_history", "delivery", "payment", "review", "order_item", "orders",
		"cart", "wishlist", "combo_item", "combo", "qna", "product_image",
		"track_item", "supplement_conflict", "product", "category", "notice", "admin_log" };

	/* 테이블 한 페이지 — 정렬 컬럼은 실제 컬럼일 때만 쓴다 */
	public static Map<String, Object> browse(String table, String sort, boolean desc, int page, int size)
			throws SQLException {
		List<String> columns = new ArrayList<String>();
		for (Map<String, String> c : DB.raw("SELECT column_name FROM information_schema.columns"
				+ " WHERE table_schema = DATABASE() AND table_name = ? ORDER BY ordinal_position", table)) {
			columns.add(c.get("column_name"));
		}
		String order = columns.contains(sort) ? " ORDER BY `" + sort + "`" + (desc ? " DESC" : " ASC") : "";
		int total = DB.number("SELECT COUNT(*) FROM `" + table + "`");
		List<Object> rows = new ArrayList<Object>();
		for (Map<String, String> row : DB.raw("SELECT * FROM `" + table + "`" + order + " LIMIT ? OFFSET ?",
				size, (Math.max(page, 1) - 1) * size)) {
			rows.add(row.values());
		}
		Map<String, Object> result = new LinkedHashMap<String, Object>();
		result.put("table", table);
		result.put("columns", columns);
		result.put("total", total);
		result.put("rows", rows);
		result.put("page", Math.max(page, 1));
		result.put("size", size);
		result.put("totalPages", total <= 0 ? 1 : (total + size - 1) / size);
		return result;
	}

	/* information_schema 조회 — 값이 없으면 빈 글자 */
	public static List<Map<String, String>> schema(String sql) throws SQLException {
		List<Map<String, String>> list = DB.raw(sql);
		for (Map<String, String> row : list) {
			row.replaceAll((k, v) -> v == null ? "" : v);
		}
		return list;
	}

	/* 테이블 비우기 — 외래키 검사를 잠시 끄고 한 커넥션에서 처리한다. users 면 일반 회원도 지운다 */
	public static void truncate(boolean users, String... tables) throws SQLException {
		DB.begin();
		try {
			DB.update("SET FOREIGN_KEY_CHECKS = 0");
			try {
				for (String t : tables) {
					DB.update("TRUNCATE TABLE `" + t + "`");
				}
				if (users) {
					DB.update("DELETE FROM member WHERE role = 'USER'");
				}
			} finally {
				DB.update("SET FOREIGN_KEY_CHECKS = 1");
			}
			DB.commit();
		} finally {
			DB.end();
		}
	}
}
