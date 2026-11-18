package shop.dao;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import shop.model.Page;
import shop.model.Product;

/* 상품 · 상품 이미지 · 트랙 구성 (product, product_image, track_item) */

public class ProductDAO {

	/* 고객에게 보이는 상품 = 판매중 + 재고 있음 */
	public static final String ON_SALE = " p.status = '판매중' AND p.stock > 0 ";

	private static final String SELECT =
		"SELECT p.*, c.name AS category_name,"
		+ " IFNULL(r.avg_rating, 0) AS avg_rating, IFNULL(r.review_count, 0) AS review_count";
	private static final String FROM =
		"FROM product p JOIN category c ON c.category_id = p.category_id"
		+ " LEFT JOIN (SELECT product_id, AVG(rating) AS avg_rating, COUNT(*) AS review_count"
		+ "   FROM review GROUP BY product_id) r ON r.product_id = p.product_id";
	/* 카테고리 조건 — 고른 카테고리와 그 소분류 */
	private static final String IN_CATEGORY =
		" AND (p.category_id = ? OR p.category_id IN (SELECT category_id FROM category WHERE parent_id = ?))";

	public static boolean isType(String type) {
		return "SINGLE".equals(type) || "TRACK".equals(type);
	}

	/* 판매 중인 최신 상품 (메인) */
	public static List<Product> latest(String type, int limit) throws SQLException {
		return withItems(DB.list(Product.class, SELECT + " " + FROM + " WHERE" + ON_SALE
				+ " AND p.product_type = ? ORDER BY p.created_at DESC, p.product_id DESC LIMIT ?", type, limit));
	}

	/* 구분별 전체 — 판매 중인 것만(onSale) 또는 전부 */
	public static List<Product> byType(String type, boolean onSale) throws SQLException {
		return withItems(DB.list(Product.class, SELECT + " " + FROM + " WHERE p.product_type = ?"
				+ (onSale ? " AND" + ON_SALE : "")
				+ " ORDER BY c.sort_order, p.category_id, p.name, p.product_id", type));
	}

	/* id 여러 개 → { id : 상품 } (상태와 상관없이) */
	public static Map<Integer, Product> byIds(Collection<Integer> ids) throws SQLException {
		Map<Integer, Product> map = new LinkedHashMap<Integer, Product>();
		if (!ids.isEmpty()) {
			for (Product p : DB.list(Product.class, SELECT + " " + FROM
					+ " WHERE p.product_id IN (" + DB.marks(ids.size()) + ")", ids)) {
				map.put(p.getProductId(), p);
			}
		}
		return map;
	}

	/* 상품 하나 — 이미지 · 트랙 구성까지. onSale 이면 고객에게 보이는 상품만 */
	public static Product byId(int productId, boolean onSale) throws SQLException {
		Product p = DB.one(Product.class, SELECT + " " + FROM + " WHERE p.product_id = ?"
				+ (onSale ? " AND" + ON_SALE : ""), productId);
		if (p != null) {
			p.setImages(DB.maps("SELECT * FROM product_image WHERE product_id = ? ORDER BY sort_order, image_id", productId));
			withItems(Collections.singletonList(p));
		}
		return p;
	}

	/* 상품 목록 (고객) — 카테고리 · 구분 · 검색어 · 정렬 */
	public static Page<Product> shopPage(int categoryId, String type, String keyword, String sort, int page)
			throws SQLException {
		StringBuilder where = new StringBuilder(" WHERE" + ON_SALE);
		List<Object> args = new ArrayList<Object>();
		if (isType(type)) {
			where.append(" AND p.product_type = ?");
			args.add(type);
		}
		if (categoryId > 0) {
			where.append(IN_CATEGORY);
			args.add(categoryId);
			args.add(categoryId);
		}
		if (keyword != null && keyword.length() > 0) {
			/* 트랙은 구성품 이름으로도 찾는다 — '오메가' 로 오메가3 가 든 트랙도 나온다 */
			where.append(" AND (p.name LIKE ? OR p.product_id IN (SELECT ti.track_id FROM track_item ti"
					+ " JOIN product i ON i.product_id = ti.item_id WHERE i.name LIKE ?))");
			args.add(like(keyword));
			args.add(like(keyword));
		}
		String order = "priceAsc".equals(sort) ? "ORDER BY p.price ASC, p.product_id DESC"
				: "priceDesc".equals(sort) ? "ORDER BY p.price DESC, p.product_id DESC"
				: "rating".equals(sort) ? "ORDER BY avg_rating DESC, review_count DESC, p.product_id DESC"
				: "name".equals(sort) ? "ORDER BY p.name ASC, p.product_id DESC"
				: "ORDER BY p.created_at DESC, p.product_id DESC";
		Page<Product> result = DB.page(Product.class, SELECT, FROM + where, order, page, 10, args);
		withItems(result.getList());
		return result;
	}

	/* 상품 목록 (관리자) — 숨김 · 품절도 포함 */
	public static Page<Product> adminPage(int categoryId, String type, String keyword, String status, int page, int size)
			throws SQLException {
		StringBuilder where = new StringBuilder(" WHERE 1 = 1");
		List<Object> args = new ArrayList<Object>();
		if (categoryId > 0) {
			where.append(IN_CATEGORY);
			args.add(categoryId);
			args.add(categoryId);
		}
		if (isType(type)) {
			where.append(" AND p.product_type = ?");
			args.add(type);
		}
		if (keyword != null && keyword.length() > 0) {
			where.append(" AND p.name LIKE ?");
			args.add(like(keyword));
		}
		if ("판매중".equals(status) || "숨김".equals(status)) {
			where.append(" AND p.status = ?");
			args.add(status);
		}
		return DB.page(Product.class, SELECT, FROM + where, "ORDER BY p.product_id DESC", page, size, args);
	}

	/* LIKE '%검색어%' — 검색어 안의 % _ \ 는 글자 그대로 찾는다 */
	public static String like(String keyword) {
		return "%" + keyword.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
	}

	/* 트랙 상품이면 구성 낱개 목록을 채운다 */
	private static List<Product> withItems(List<Product> list) throws SQLException {
		for (Product p : list) {
			if (p.isTrack()) {
				p.setTrackItems(trackItems(p.getProductId()));
			}
		}
		return list;
	}

	public static List<Product> trackItems(int trackId) throws SQLException {
		return DB.list(Product.class, "SELECT p.* FROM track_item ti JOIN product p ON p.product_id = ti.item_id"
				+ " WHERE ti.track_id = ? ORDER BY ti.sort_order, ti.item_id", trackId);
	}

	/* 대분류마다 상품 구분이 하나뿐이면 { 대분류 id : SINGLE 또는 TRACK } */
	public static Map<Integer, String> rootTypes() throws SQLException {
		Map<Integer, String> map = new LinkedHashMap<Integer, String>();
		for (Map<String, Object> row : DB.maps("SELECT IFNULL(c.parent_id, c.category_id) AS root_id,"
				+ " MIN(p.product_type) AS t1, MAX(p.product_type) AS t2"
				+ " FROM product p JOIN category c ON c.category_id = p.category_id"
				+ " GROUP BY IFNULL(c.parent_id, c.category_id)")) {
			if (row.get("t1").equals(row.get("t2"))) {
				map.put(((Number) row.get("rootId")).intValue(), (String) row.get("t1"));
			}
		}
		return map;
	}

	/* 판매 중인 상품이 있는 카테고리 id */
	public static Set<Integer> onSaleCategoryIds() throws SQLException {
		Set<Integer> ids = new HashSet<Integer>();
		for (Map<String, Object> row : DB.maps("SELECT DISTINCT p.category_id FROM product p WHERE" + ON_SALE)) {
			ids.add(((Number) row.get("categoryId")).intValue());
		}
		return ids;
	}

	/* 대분류 아래에 이 구분(type)의 다른 상품이 몇 개인가 */
	public static int countUnderRoot(int rootId, String type, int exceptId) throws SQLException {
		return DB.number("SELECT COUNT(*) FROM product p WHERE (p.category_id = ? OR p.category_id IN"
				+ " (SELECT category_id FROM category WHERE parent_id = ?)) AND p.product_type = ? AND p.product_id <> ?",
				rootId, rootId, type, exceptId);
	}

	public static int countOrderItems(int productId) throws SQLException {
		return DB.number("SELECT COUNT(*) FROM order_item WHERE product_id = ?", productId);
	}

	/* 등록(productId 0) 또는 수정 — 등록이면 새 id 를 돌려준다 */
	public static int save(int productId, int categoryId, String type, String name, int price, int stock,
			String description, String thumbnail, String status) throws SQLException {
		if (productId > 0) {
			DB.update("UPDATE product SET category_id = ?, product_type = ?, name = ?, price = ?, stock = ?,"
					+ " description = ?, thumbnail = ?, status = ? WHERE product_id = ?",
					categoryId, type, name, price, stock, description, thumbnail, status, productId);
			return productId;
		}
		return DB.insert("INSERT INTO product (category_id, product_type, name, price, stock, description, thumbnail, status)"
				+ " VALUES (?, ?, ?, ?, ?, ?, ?, ?)", categoryId, type, name, price, stock, description, thumbnail, status);
	}

	public static void delete(int productId) throws SQLException {
		DB.update("DELETE FROM product WHERE product_id = ?", productId);
	}

	/* 재고 빼기 — 재고가 모자라면 0 (아무것도 바꾸지 않음) */
	public static int decreaseStock(int productId, int quantity) throws SQLException {
		return DB.update("UPDATE product SET stock = stock - ? WHERE product_id = ? AND stock >= ?",
				quantity, productId, quantity);
	}

	public static void increaseStock(int productId, int quantity) throws SQLException {
		DB.update("UPDATE product SET stock = stock + ? WHERE product_id = ?", quantity, productId);
	}

	public static void setStock(int productId, int stock) throws SQLException {
		DB.update("UPDATE product SET stock = ? WHERE product_id = ?", stock, productId);
	}

	/* 추가 이미지 — 순서는 지금 마지막 + 1 */
	public static void addImage(int productId, String path) throws SQLException {
		int next = DB.number("SELECT IFNULL(MAX(sort_order), 0) + 1 FROM product_image WHERE product_id = ?", productId);
		DB.update("INSERT INTO product_image (product_id, img_path, sort_order) VALUES (?, ?, ?)", productId, path, next);
	}

	public static void deleteImage(int imageId) throws SQLException {
		DB.update("DELETE FROM product_image WHERE image_id = ?", imageId);
	}

	/* 트랙 구성 통째로 바꾸기 */
	public static void setTrackItems(int trackId, Collection<Integer> itemIds) throws SQLException {
		DB.begin();
		try {
			DB.update("DELETE FROM track_item WHERE track_id = ?", trackId);
			int order = 1;
			for (Integer id : itemIds) {
				DB.update("INSERT INTO track_item (track_id, item_id, sort_order) VALUES (?, ?, ?)", trackId, id, order++);
			}
			DB.commit();
		} finally {
			DB.end();
		}
	}

	public static int countTracksUsing(int itemId) throws SQLException {
		return DB.number("SELECT COUNT(*) FROM track_item WHERE item_id = ?", itemId);
	}

	public static int countTracksWithBoth(int a, int b) throws SQLException {
		return DB.number("SELECT COUNT(*) FROM track_item a JOIN track_item b ON b.track_id = a.track_id"
				+ " WHERE a.item_id = ? AND b.item_id = ?", a, b);
	}
}
