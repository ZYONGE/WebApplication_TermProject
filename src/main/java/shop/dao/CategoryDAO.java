package shop.dao;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import shop.model.Category;

/* 카테고리 (category) — 대분류 / 소분류 2단계 */
public class CategoryDAO {

	public static List<Category> all() throws SQLException {
		return DB.list(Category.class, "SELECT c.*, p.name AS parent_name FROM category c"
				+ " LEFT JOIN category p ON p.category_id = c.parent_id ORDER BY c.sort_order, c.category_id");
	}

	public static Category byId(int categoryId) throws SQLException {
		return DB.one(Category.class, "SELECT * FROM category WHERE category_id = ?", categoryId);
	}

	/* 대분류 목록 — 각 대분류의 children 에 소분류가 들어 있다 */
	public static List<Category> tree() throws SQLException {
		List<Category> all = all();
		List<Category> roots = new ArrayList<Category>();
		for (Category c : all) {
			if (c.getParentId() == null) {
				roots.add(c);
			}
		}
		for (Category c : all) {
			for (Category root : roots) {
				if (c.getParentId() != null && c.getParentId() == root.getCategoryId()) {
					root.getChildren().add(c);
				}
			}
		}
		return roots;
	}

	/* 대분류 다음에 그 소분류가 오는 한 줄 목록 */
	public static List<Category> ordered() throws SQLException {
		List<Category> list = new ArrayList<Category>();
		for (Category root : tree()) {
			list.add(root);
			list.addAll(root.getChildren());
		}
		return list;
	}

	public static boolean existsRootName(String name, int exceptId) throws SQLException {
		return DB.number("SELECT COUNT(*) FROM category WHERE parent_id IS NULL AND name = ? AND category_id <> ?",
				name, exceptId) > 0;
	}

	/* 등록(categoryId 0) 또는 수정. parentId 0 이면 대분류 */
	public static void save(int categoryId, int parentId, String name, int sortOrder) throws SQLException {
		Integer parent = parentId > 0 ? Integer.valueOf(parentId) : null;
		if (categoryId > 0) {
			DB.update("UPDATE category SET parent_id = ?, name = ?, sort_order = ? WHERE category_id = ?",
					parent, name, sortOrder, categoryId);
		} else {
			DB.update("INSERT INTO category (parent_id, name, sort_order) VALUES (?, ?, ?)", parent, name, sortOrder);
		}
	}

	public static void delete(int categoryId) throws SQLException {
		DB.update("DELETE FROM category WHERE category_id = ?", categoryId);
	}

	public static int countChildren(int categoryId) throws SQLException {
		return DB.number("SELECT COUNT(*) FROM category WHERE parent_id = ?", categoryId);
	}

	public static int countProducts(int categoryId) throws SQLException {
		return DB.number("SELECT COUNT(*) FROM product WHERE category_id = ?", categoryId);
	}
}
