package shop.service;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.oreilly.servlet.MultipartRequest;
import com.oreilly.servlet.multipart.DefaultFileRenamePolicy;

import shop.dao.AdminDAO;
import shop.dao.CategoryDAO;
import shop.dao.ConflictDAO;
import shop.dao.DB;
import shop.dao.ProductDAO;
import shop.model.Category;
import shop.model.Conflict;
import shop.model.Product;

/* 관리자 — 상품(이미지 업로드 포함) · 카테고리 · 금지 조합 */
@WebServlet({ "/admin/products", "/admin/categories", "/admin/conflicts" })

public class AdminProductServlet extends BaseServlet {

	private static final long serialVersionUID = 1L;

	@Override
	protected void get(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException, SQLException {
		int editId = intParam(request, "editId", 0);
		switch (request.getServletPath()) {
		case "/admin/categories":
			request.setAttribute("categories", CategoryDAO.ordered());
			request.setAttribute("tree", CategoryDAO.tree());
			render(request, response, "admin/categories", "카테고리 관리");
			break;
		case "/admin/conflicts":
			request.setAttribute("conflicts", ConflictDAO.all());
			request.setAttribute("singles", ProductDAO.byType("SINGLE", false));
			if (editId > 0) {
				request.setAttribute("editConflict", ConflictDAO.byId(editId));
			}
			render(request, response, "admin/conflicts", "금지 조합 관리");
			break;
		default:
			int categoryId = intParam(request, "categoryId", 0);
			request.setAttribute("selectedCategoryId", categoryId);
			request.setAttribute("pageData", ProductDAO.adminPage(categoryId, param(request, "type"),
					param(request, "keyword"), param(request, "status"), intParam(request, "page", 1), 10));
			request.setAttribute("categories", CategoryDAO.ordered());
			request.setAttribute("rootTypes", ProductDAO.rootTypes());
			request.setAttribute("singles", ProductDAO.byType("SINGLE", false));
			request.setAttribute("pageUrl", "/admin/products?");
			request.setAttribute("categoryTree", CategoryDAO.tree());
			Product edit = editId > 0 ? ProductDAO.byId(editId, false) : null;
			if (edit != null) {
				/* 수정 팝업의 트랙 구성 체크박스 — 지금 구성에 든 낱개 id */
				Set<Integer> trackItemIds = new HashSet<Integer>();
				for (Product item : edit.getTrackItems()) {
					trackItemIds.add(item.getProductId());
				}
				request.setAttribute("editProduct", edit);
				request.setAttribute("trackItemIds", trackItemIds);
			}
			render(request, response, "admin/products", "상품 관리");
		}
	}

	@Override
	protected void post(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException, SQLException {
		int adminId = loginId(request);
		String path = request.getServletPath();
		String mode = param(request, "mode", "");
		try {
			if (path.equals("/admin/categories")) {
				category(request, adminId, mode);
			} else if (path.equals("/admin/conflicts")) {
				conflict(request, adminId, mode);
			} else if (String.valueOf(request.getContentType()).toLowerCase().startsWith("multipart/")) {
				saveProduct(request, adminId);
			} else if (mode.equals("delete")) {
				int productId = intParam(request, "productId", 0);
				Product p = ProductDAO.byId(productId, false);
				if (p == null) {
					throw new Fail("상품을 찾을 수 없습니다.");
				}
				try {
					ProductDAO.delete(productId);
				} catch (SQLException e) {
					throw new Fail("주문 이력이 있는 상품은 삭제할 수 없습니다. 상태를 '숨김' 으로 바꾸세요.");
				}
				AdminDAO.adminLog(adminId, "PRODUCT_DELETE", "product", productId, "삭제: " + p.getName());
				flash(request, "상품을 삭제했습니다.");
			} else if (mode.equals("deleteImage")) {
				ProductDAO.deleteImage(intParam(request, "imageId", 0));
				flash(request, "이미지를 삭제했습니다.");
			}
		} catch (Fail e) {
			flashError(request, e.getMessage());
		}
		redirect(request, response, path);
	}

	/* ---------- 상품 등록 · 수정 (multipart 폼 — 대표 이미지 + 추가 이미지) ---------- */

	private void saveProduct(HttpServletRequest request, int adminId) throws IOException, SQLException {
		File dir = new File(getServletContext().getRealPath("/upload"));
		dir.mkdirs();
		MultipartRequest form = new MultipartRequest(request, dir.getPath(), 10 * 1024 * 1024, "UTF-8",
				new DefaultFileRenamePolicy());
		int productId = toInt(form.getParameter("productId"), 0);
		int categoryId = toInt(form.getParameter("categoryId"), 0);
		String type = trim(form.getParameter("productType"));
		String name = trim(form.getParameter("name"));
		int price = toInt(form.getParameter("price"), 0);
		int stock = toInt(form.getParameter("stock"), 0);
		String status = trim(form.getParameter("status"));
		String thumbnail = form.getFilesystemName("thumbnail") != null
				? "/upload/" + form.getFilesystemName("thumbnail") : trim(form.getParameter("currentThumbnail"));
		List<Integer> itemIds = new ArrayList<Integer>();
		if (form.getParameterValues("itemId") != null) {
			for (String v : form.getParameterValues("itemId")) {
				if (toInt(v, 0) > 0) {
					itemIds.add(toInt(v, 0));
				}
			}
		}
		boolean track = "TRACK".equals(type);

		checkProduct(productId, categoryId, type, name, price, stock, status);
		Product before = productId > 0 ? ProductDAO.byId(productId, false) : null;
		if (productId > 0 && before == null) {
			throw new Fail("상품을 찾을 수 없습니다.");
		}
		/* 구분(낱개 ↔ 트랙)을 바꿀 수 있는 경우는 제한된다 */
		boolean typeChanged = before != null && !before.getProductType().equals(type);
		if (typeChanged && ProductDAO.countOrderItems(productId) > 0) {
			throw new Fail("판매 이력이 있는 상품은 낱개 / 트랙 구분을 바꿀 수 없습니다.");
		}
		if (typeChanged && track && (ProductDAO.countTracksUsing(productId) > 0
				|| !ConflictDAO.involving(Collections.singletonList(productId)).isEmpty())) {
			throw new Fail("트랙 구성품이거나 금지 조합에 걸린 낱개는 트랙으로 바꿀 수 없습니다.");
		}
		if (track) {
			checkTrackItems(itemIds);
		}
		int savedId = ProductDAO.save(productId, categoryId, type, name, price, stock,
				form.getParameter("description"), thumbnail, status);
		if (before == null) {
			AdminDAO.adminLog(adminId, "PRODUCT_CREATE", "product", savedId, "등록: " + name + " / " + price
					+ "원(1포) / 재고 " + stock + "포 / " + (track ? "트랙" : "낱개"));
		}
		if (track || typeChanged) {
			saveTrackItems(adminId, savedId, track ? itemIds : new ArrayList<Integer>());
		}
		if (before != null) {
			AdminDAO.adminLog(adminId, "PRODUCT_UPDATE", "product", productId,
					"가격 " + before.getPrice() + " -> " + price + " / 재고 " + before.getStock() + " -> " + stock
					+ " / 상태 " + before.getStatus() + " -> " + status);
		}
		flash(request, before == null ? "상품을 등록했습니다." : "상품을 수정했습니다.");

		/* 대표 이미지 말고 함께 올린 파일은 추가 이미지로 */
		Enumeration<?> files = form.getFileNames();
		while (files.hasMoreElements()) {
			String field = (String) files.nextElement();
			if (!field.equals("thumbnail") && form.getFilesystemName(field) != null) {
				ProductDAO.addImage(savedId, "/upload/" + form.getFilesystemName(field));
			}
		}
	}

	private static String trim(String s) {
		return s == null ? null : s.trim();
	}

	private void checkProduct(int productId, int categoryId, String type, String name, int price, int stock,
			String status) throws SQLException {
		if (isEmpty(name)) {
			throw new Fail("상품명을 입력하세요.");
		}
		if (!ProductDAO.isType(type)) {
			throw new Fail("낱개 / 트랙 구분을 선택하세요.");
		}
		if (categoryId <= 0) {
			throw new Fail("카테고리를 선택하세요.");
		}
		Category category = CategoryDAO.byId(categoryId);
		if (category == null || category.getParentId() == null) {
			throw new Fail("상품은 소분류에만 등록할 수 있습니다.");
		}
		/* 한 대분류에는 낱개만 또는 트랙만 */
		Category root = CategoryDAO.byId(category.getParentId());
		boolean track = "TRACK".equals(type);
		if (root != null && ProductDAO.countUnderRoot(root.getCategoryId(), track ? "SINGLE" : "TRACK", productId) > 0) {
			String label = track ? "트랙" : "낱개";
			throw new Fail("'" + root.getName() + "' 대분류에는 " + (track ? "낱개" : "트랙") + " 상품이 있습니다. "
					+ label + " 상품은 " + label + " 상품이 있는 대분류의 소분류에 등록하세요.");
		}
		if (price < 0) {
			throw new Fail("가격은 0원 이상이어야 합니다.");
		}
		if (stock < 0) {
			throw new Fail("재고는 0포 이상이어야 합니다.");
		}
		if (!"판매중".equals(status) && !"숨김".equals(status)) {
			throw new Fail("상품 상태가 올바르지 않습니다.");
		}
	}

	/* 트랙 구성 — 있는 낱개만, 구성품끼리 금지 조합이 없어야 한다 */
	private void checkTrackItems(List<Integer> itemIds) throws SQLException {
		Set<Integer> ids = new LinkedHashSet<Integer>(itemIds);
		if (ids.isEmpty()) {
			throw new Fail("트랙 구성 낱개를 하나 이상 고르세요.");
		}
		Map<Integer, Product> products = ProductDAO.byIds(ids);
		for (Product p : products.values()) {
			if (p.isTrack()) {
				throw new Fail("트랙 구성에는 낱개 영양제만 넣을 수 있습니다.");
			}
		}
		if (products.size() != ids.size()) {
			throw new Fail("없는 상품이 구성에 들어 있습니다.");
		}
		List<Conflict> inside = ConflictDAO.among(ids);
		if (!inside.isEmpty()) {
			throw new Fail("구성품끼리 금지 조합이 있어 저장할 수 없습니다: "
					+ inside.get(0).getProductAName() + " × " + inside.get(0).getProductBName());
		}
	}

	/* 트랙 구성 저장 — 실제로 바뀌었을 때만 로그를 남긴다 */
	private void saveTrackItems(int adminId, int trackId, List<Integer> itemIds) throws SQLException {
		String before = names(ProductDAO.trackItems(trackId));
		ProductDAO.setTrackItems(trackId, new LinkedHashSet<Integer>(itemIds));
		String after = names(ProductDAO.trackItems(trackId));
		if (!before.equals(after)) {
			AdminDAO.adminLog(adminId, "TRACK_ITEM_SET", "track_item", trackId, "구성 " + before + " -> " + after);
		}
	}

	/* "비타민C, 아연" — 없으면 "(없음)" */
	private static String names(List<Product> items) {
		List<String> names = new ArrayList<String>();
		for (Product p : items) {
			names.add(p.getName());
		}
		return names.isEmpty() ? "(없음)" : String.join(", ", names);
	}

	/* ---------- 카테고리 ---------- */

	private void category(HttpServletRequest request, int adminId, String mode) throws SQLException {
		int categoryId = intParam(request, "categoryId", 0);
		int parentId = intParam(request, "parentId", 0);
		String name = param(request, "name");
		if (mode.equals("delete")) {
			if (CategoryDAO.countChildren(categoryId) > 0) {
				throw new Fail("하위 카테고리가 있어 삭제할 수 없습니다.");
			}
			if (CategoryDAO.countProducts(categoryId) > 0) {
				throw new Fail("이 카테고리에 속한 상품이 있어 삭제할 수 없습니다.");
			}
			CategoryDAO.delete(categoryId);
			AdminDAO.adminLog(adminId, "CATEGORY_DELETE", "category", categoryId, "삭제");
			flash(request, "카테고리를 삭제했습니다.");
			return;
		}
		if (isEmpty(name)) {
			throw new Fail("카테고리 이름을 입력하세요.");
		}
		if (parentId == 0 && CategoryDAO.existsRootName(name, categoryId)) {
			throw new Fail("같은 이름의 대분류가 이미 있습니다.");
		}
		CategoryDAO.save(categoryId, parentId, name, intParam(request, "sortOrder", 0));
		if (categoryId > 0) {
			AdminDAO.adminLog(adminId, "CATEGORY_UPDATE", "category", categoryId, "수정: " + name);
			flash(request, "카테고리를 수정했습니다.");
		} else {
			AdminDAO.adminLog(adminId, "CATEGORY_CREATE", "category", null, "등록: " + name);
			flash(request, "카테고리를 등록했습니다.");
		}
	}

	/* ---------- 금지 조합 ---------- */

	private void conflict(HttpServletRequest request, int adminId, String mode) throws SQLException {
		int conflictId = intParam(request, "conflictId", 0);
		String message = param(request, "message");
		if (!mode.equals("create") && !mode.equals("update") && !mode.equals("delete")) {
			return;
		}
		if (!mode.equals("delete")) {
			if (isEmpty(message)) {
				throw new Fail("경고 문구를 입력하세요.");
			}
			if (message.length() > 200) {
				throw new Fail("경고 문구는 200자 이내로 입력하세요.");
			}
		}
		Conflict before = ConflictDAO.byId(conflictId);
		String names = null;
		if (mode.equals("create")) {
			int x = intParam(request, "productA", 0);
			int y = intParam(request, "productB", 0);
			if (x <= 0 || y <= 0 || x == y) {
				throw new Fail("서로 다른 낱개 영양제 두 개를 고르세요.");
			}
			Map<Integer, Product> pair = ProductDAO.byIds(Arrays.asList(x, y));
			Product a = pair.get(x);
			Product b = pair.get(y);
			if (a == null || b == null || a.isTrack() || b.isTrack()) {
				throw new Fail("금지 조합은 낱개 영양제끼리만 등록할 수 있습니다.");
			}
			if (ConflictDAO.exists(x, y)) {
				throw new Fail("이미 등록된 조합입니다.");
			}
			if (ProductDAO.countTracksWithBoth(x, y) > 0) {
				throw new Fail("두 상품이 함께 들어 있는 트랙이 있습니다. 트랙 구성부터 고치세요.");
			}
			names = a.getName() + " × " + b.getName();
		} else if (before == null) {
			throw new Fail("금지 조합을 찾을 수 없습니다.");
		} else {
			names = before.getProductAName() + " × " + before.getProductBName();
		}
		DB.begin();
		try {
			if (mode.equals("create")) {
				conflictId = ConflictDAO.insert(intParam(request, "productA", 0), intParam(request, "productB", 0), message);
				AdminDAO.adminLog(adminId, "CONFLICT_CREATE", "supplement_conflict", conflictId, names);
			} else if (mode.equals("update")) {
				ConflictDAO.updateMessage(conflictId, message);
				AdminDAO.adminLog(adminId, "CONFLICT_UPDATE", "supplement_conflict", conflictId, names + " 문구 수정");
			} else {
				ConflictDAO.delete(conflictId);
				AdminDAO.adminLog(adminId, "CONFLICT_DELETE", "supplement_conflict", conflictId, "삭제: " + names);
			}
			DB.commit();
		} finally {
			DB.end();
		}
		flash(request, mode.equals("create") ? "금지 조합을 등록했습니다."
				: mode.equals("update") ? "경고 문구를 수정했습니다." : "금지 조합을 삭제했습니다.");
	}
}
