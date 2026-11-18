package shop.dao;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import shop.model.Conflict;
import shop.model.Product;

/* 금지 조합 (supplement_conflict) 과 동시 섭취 경고 */
public class ConflictDAO {

	private static final String SELECT = "SELECT sc.*, pa.name AS product_a_name, pb.name AS product_b_name"
		+ " FROM supplement_conflict sc JOIN product pa ON pa.product_id = sc.product_a"
		+ " JOIN product pb ON pb.product_id = sc.product_b";

	public static List<Conflict> all() throws SQLException {
		return DB.list(Conflict.class, SELECT + " ORDER BY sc.product_a, sc.product_b");
	}

	public static Conflict byId(int conflictId) throws SQLException {
		return DB.one(Conflict.class, SELECT + " WHERE sc.conflict_id = ?", conflictId);
	}

	/* ids 안의 두 상품끼리 걸린 금지 조합 */
	public static List<Conflict> among(Collection<Integer> ids) throws SQLException {
		if (ids.size() < 2) {
			return new ArrayList<Conflict>();
		}
		String in = DB.marks(ids.size());
		return DB.list(Conflict.class, SELECT + " WHERE sc.product_a IN (" + in + ") AND sc.product_b IN (" + in + ")"
				+ " ORDER BY sc.product_a, sc.product_b", ids, ids);
	}

	/* ids 중 하나라도 걸린 금지 조합 */
	public static List<Conflict> involving(Collection<Integer> ids) throws SQLException {
		String in = DB.marks(ids.size());
		return DB.list(Conflict.class, SELECT + " WHERE sc.product_a IN (" + in + ") OR sc.product_b IN (" + in + ")"
				+ " ORDER BY sc.product_a, sc.product_b", ids, ids);
	}

	/* 상품 화면 — 이 상품(트랙이면 구성품)과 함께 먹으면 안 되는 다른 영양제 */
	public static List<Conflict> conflictsFor(Product product) throws SQLException {
		Set<Integer> ids = new LinkedHashSet<Integer>();
		if (product.isTrack()) {
			for (Product item : product.getTrackItems()) {
				ids.add(item.getProductId());
			}
		} else {
			ids.add(product.getProductId());
		}
		List<Conflict> result = new ArrayList<Conflict>();
		if (ids.isEmpty()) {
			return result;
		}
		for (Conflict c : involving(ids)) {
			if (!(ids.contains(c.getProductA()) && ids.contains(c.getProductB()))) {
				result.add(c);
			}
		}
		return result;
	}

	/*
	  동시 섭취 경고 — 고른 상품들(트랙은 구성품으로 펼침) 사이의
	    1. 금지 조합 (CONFLICT)
	    2. 같은 영양제가 두 번 이상 (DUPLICATE)
	*/
	public static List<Conflict> warnings(Collection<Integer> productIds) throws SQLException {
		List<Conflict> warnings = new ArrayList<Conflict>();
		Set<Integer> ids = new LinkedHashSet<Integer>(productIds);
		if (ids.isEmpty()) {
			return warnings;
		}
		/* 영양제 id → 그 영양제가 들어간 곳들 { 이름, 트랙 이름(직접 고른 낱개면 null) } */
		Map<Integer, List<String[]>> found = new LinkedHashMap<Integer, List<String[]>>();
		Map<Integer, Product> products = ProductDAO.byIds(ids);
		for (Integer id : ids) {
			Product p = products.get(id);
			if (p == null) {
				continue;
			}
			if (p.isTrack()) {
				for (Product item : ProductDAO.trackItems(id)) {
					put(found, item.getProductId(), item.getName(), p.getName());
				}
			} else {
				put(found, id, p.getName(), null);
			}
		}
		for (Conflict c : among(found.keySet())) {
			String[] a = found.get(c.getProductA()).get(0);
			String[] b = found.get(c.getProductB()).get(0);
			warnings.add(new Conflict("CONFLICT", c.getProductA(), a[0], a[1], c.getProductB(), b[0], b[1], c.getMessage()));
		}
		for (Map.Entry<Integer, List<String[]>> e : found.entrySet()) {
			List<String[]> same = e.getValue();
			if (same.size() >= 2) {
				warnings.add(new Conflict("DUPLICATE", e.getKey(), same.get(0)[0], same.get(0)[1], e.getKey(),
						same.get(1)[0], same.get(1)[1], "'" + same.get(0)[0] + "' 이(가) " + same.size()
						+ "번 들어갑니다. 같은 영양소를 겹쳐 드시게 됩니다."));
			}
		}
		return warnings;
	}

	private static void put(Map<Integer, List<String[]>> found, int id, String name, String via) {
		if (!found.containsKey(id)) {
			found.put(id, new ArrayList<String[]>());
		}
		found.get(id).add(new String[] { name, via });
	}

	public static boolean exists(int x, int y) throws SQLException {
		return DB.number("SELECT COUNT(*) FROM supplement_conflict WHERE product_a = ? AND product_b = ?",
				Math.min(x, y), Math.max(x, y)) > 0;
	}

	/* 작은 id 를 product_a 로 저장한다 */
	public static int insert(int x, int y, String message) throws SQLException {
		return DB.insert("INSERT INTO supplement_conflict (product_a, product_b, message) VALUES (?, ?, ?)",
				Math.min(x, y), Math.max(x, y), message);
	}

	public static void updateMessage(int conflictId, String message) throws SQLException {
		DB.update("UPDATE supplement_conflict SET message = ? WHERE conflict_id = ?", message, conflictId);
	}

	public static void delete(int conflictId) throws SQLException {
		DB.update("DELETE FROM supplement_conflict WHERE conflict_id = ?", conflictId);
	}
}
