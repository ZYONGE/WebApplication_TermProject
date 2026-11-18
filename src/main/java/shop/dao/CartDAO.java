package shop.dao;

import java.sql.SQLException;
import java.util.Collection;
import java.util.List;

import shop.model.Combo;
import shop.model.Item;

/* 장바구니 · 찜 · 내 조합 (cart, wishlist, combo, combo_item) */
public class CartDAO {

	private static final String PRODUCT_COLS =
		"p.product_type, p.name AS product_name, p.thumbnail, p.status, p.price, p.stock";

	/* 장바구니 — newestFirst 면 최근에 담은 것부터 */
	public static List<Item> cart(int memberId, boolean newestFirst) throws SQLException {
		return DB.list(Item.class, "SELECT ct.*, " + PRODUCT_COLS
				+ " FROM cart ct JOIN product p ON p.product_id = ct.product_id WHERE ct.member_id = ?"
				+ " ORDER BY ct.cart_id" + (newestFirst ? " DESC" : ""), memberId);
	}

	public static int countCart(int memberId) throws SQLException {
		return DB.number("SELECT COUNT(*) FROM cart WHERE member_id = ?", memberId);
	}

	/* 담기 — 이미 담긴 상품이면 일수를 새 값으로 바꾼다 */
	public static void add(int memberId, int productId, int days) throws SQLException {
		DB.update("INSERT INTO cart (member_id, product_id, quantity) VALUES (?, ?, ?)"
				+ " ON DUPLICATE KEY UPDATE quantity = ?", memberId, productId, days, days);
	}

	public static int updateDays(int cartId, int memberId, int days) throws SQLException {
		return DB.update("UPDATE cart SET quantity = ? WHERE cart_id = ? AND member_id = ?", days, cartId, memberId);
	}

	public static void delete(int cartId, int memberId) throws SQLException {
		DB.update("DELETE FROM cart WHERE cart_id = ? AND member_id = ?", cartId, memberId);
	}

	public static void clear(int memberId) throws SQLException {
		DB.update("DELETE FROM cart WHERE member_id = ?", memberId);
	}

	/* ---------- 찜 ---------- */

	public static List<Item> wishes(int memberId) throws SQLException {
		return DB.list(Item.class, "SELECT w.*, " + PRODUCT_COLS
				+ " FROM wishlist w JOIN product p ON p.product_id = w.product_id WHERE w.member_id = ?"
				+ " ORDER BY w.created_at DESC", memberId);
	}

	public static boolean isWished(int memberId, int productId) throws SQLException {
		return DB.number("SELECT COUNT(*) FROM wishlist WHERE member_id = ? AND product_id = ?", memberId, productId) > 0;
	}

	public static int countWishes(int memberId) throws SQLException {
		return DB.number("SELECT COUNT(*) FROM wishlist WHERE member_id = ?", memberId);
	}

	public static void wish(int memberId, int productId) throws SQLException {
		DB.update("INSERT IGNORE INTO wishlist (member_id, product_id) VALUES (?, ?)", memberId, productId);
	}

	public static void unwish(int memberId, int productId) throws SQLException {
		DB.update("DELETE FROM wishlist WHERE member_id = ? AND product_id = ?", memberId, productId);
	}

	/* ---------- 내 조합 ---------- */

	private static final String COMBO = "SELECT cb.*,"
		+ " (SELECT COUNT(*) FROM combo_item ci WHERE ci.combo_id = cb.combo_id) AS item_count FROM combo cb";

	public static List<Combo> combos(int memberId) throws SQLException {
		return DB.list(Combo.class, COMBO + " WHERE cb.member_id = ? ORDER BY cb.updated_at DESC, cb.combo_id DESC", memberId);
	}

	/* 조합 하나 — 담긴 영양제까지 (내 조합이 아니면 null) */
	public static Combo combo(int comboId, int memberId) throws SQLException {
		Combo combo = DB.one(Combo.class, COMBO + " WHERE cb.combo_id = ? AND cb.member_id = ?", comboId, memberId);
		if (combo != null) {
			combo.setItems(DB.list(Item.class, "SELECT ci.*, " + PRODUCT_COLS
					+ " FROM combo_item ci JOIN product p ON p.product_id = ci.product_id WHERE ci.combo_id = ?"
					+ " ORDER BY ci.sort_order, ci.product_id", comboId));
		}
		return combo;
	}

	public static int comboIdByName(int memberId, String name) throws SQLException {
		return DB.number("SELECT combo_id FROM combo WHERE member_id = ? AND name = ?", memberId, name);
	}

	/* 저장 — comboId 가 있으면 덮어쓰고, 없으면 새로 만든다. 저장된 조합 id 를 돌려준다 */
	public static int saveCombo(int comboId, int memberId, String name, int days, Collection<Integer> productIds)
			throws SQLException {
		DB.begin();
		try {
			if (comboId > 0) {
				DB.update("UPDATE combo SET days = ?, updated_at = CURRENT_TIMESTAMP WHERE combo_id = ? AND member_id = ?",
						days, comboId, memberId);
				DB.update("DELETE FROM combo_item WHERE combo_id = ?", comboId);
			} else {
				comboId = DB.insert("INSERT INTO combo (member_id, name, days) VALUES (?, ?, ?)", memberId, name, days);
			}
			int order = 1;
			for (Integer id : productIds) {
				DB.update("INSERT INTO combo_item (combo_id, product_id, sort_order) VALUES (?, ?, ?)", comboId, id, order++);
			}
			DB.commit();
			return comboId;
		} finally {
			DB.end();
		}
	}

	public static int deleteCombo(int comboId, int memberId) throws SQLException {
		return DB.update("DELETE FROM combo WHERE combo_id = ? AND member_id = ?", comboId, memberId);
	}
}
