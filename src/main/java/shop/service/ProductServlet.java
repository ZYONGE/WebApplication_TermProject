package shop.service;

import java.io.IOException;
import java.net.URLEncoder;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import shop.dao.BoardDAO;
import shop.dao.CartDAO;
import shop.dao.CategoryDAO;
import shop.dao.ConfigDAO;
import shop.dao.ConflictDAO;
import shop.dao.ProductDAO;
import shop.model.Category;
import shop.model.Product;

/* 상품 목록 · 상품 상세 */
@WebServlet({ "/product/list", "/product/view" })
public class ProductServlet extends BaseServlet {

	private static final long serialVersionUID = 1L;

	@Override
	protected void get(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException, SQLException {
		if (request.getServletPath().equals("/product/view")) {
			view(request, response);
		} else {
			list(request, response);
		}
	}

	private void list(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException, SQLException {
		int categoryId = intParam(request, "categoryId", 0);
		String type = ProductDAO.isType(param(request, "type")) ? param(request, "type") : null;
		String keyword = param(request, "keyword");
		String sort = param(request, "sort", "latest");
		Category current = categoryId > 0 ? CategoryDAO.byId(categoryId) : null;

		/*
		  카테고리 고르기 칸에 보일 대분류 —
		  카테고리나 구분(낱개 / 트랙)으로 들어왔으면 그 대분류만(탭 모드), 아니면 전부
		*/
		List<Category> tree = CategoryDAO.tree();
		List<Category> tabRoots = new ArrayList<Category>();
		Map<Integer, String> rootTypes = ProductDAO.rootTypes();
		for (Category root : tree) {
			boolean mine = root.getCategoryId() == categoryId;
			for (Category sub : root.getChildren()) {
				mine = mine || sub.getCategoryId() == categoryId;
			}
			boolean sameType = categoryId == 0 && type != null && type.equals(rootTypes.get(root.getCategoryId()));
			if ((categoryId > 0 && mine) || sameType) {
				tabRoots.add(root);
			}
		}
		boolean tabMode = !tabRoots.isEmpty();
		String heading = current != null ? current.getName()
				: tabRoots.size() == 1 ? tabRoots.get(0).getName()
				: keyword != null && !keyword.isEmpty() ? "'" + keyword + "' 검색 결과" : "전체 영양제";

		String url = "/product/list?" + (categoryId > 0 ? "categoryId=" + categoryId + "&" : "")
				+ (type != null ? "type=" + type + "&" : "")
				+ (keyword != null && !keyword.isEmpty() ? "keyword=" + URLEncoder.encode(keyword, "UTF-8") + "&" : "")
				+ "sort=" + sort + "&";

		request.setAttribute("heading", heading);
		request.setAttribute("pageData", ProductDAO.shopPage(categoryId, type, keyword, sort, intParam(request, "page", 1)));
		request.setAttribute("filterRoots", tabMode ? tabRoots : tree);
		request.setAttribute("tabMode", tabMode);
		request.setAttribute("selectedCategoryId", categoryId);
		request.setAttribute("pageUrl", url);
		render(request, response, "product/list", "상품 목록");
	}

	private void view(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException, SQLException {
		Product product = ProductDAO.byId(intParam(request, "productId", 0), true);
		if (product == null) {
			flashError(request, "판매 중인 상품이 아닙니다.");
			redirect(request, response, "/product/list");
			return;
		}
		int memberId = loginId(request);
		boolean admin = loginMember(request) != null && "ADMIN".equals(loginMember(request).getRole());

		/* 섭취 일수 — 재고 · 상한보다 많이 고를 수 없다. 처음 값은 30일 */
		int dayLimit = Math.min(product.getStock(), ConfigDAO.maxDays());
		List<Integer> quickDays = new ArrayList<Integer>();
		for (int d : ConfigDAO.quickDays()) {
			if (d <= dayLimit) {
				quickDays.add(d);
			}
		}
		request.setAttribute("product", product);
		request.setAttribute("reviews", BoardDAO.productReviews(product.getProductId()));
		request.setAttribute("qnaList", BoardDAO.productQna(product.getProductId(), memberId, admin));
		request.setAttribute("wished", memberId > 0 && CartDAO.isWished(memberId, product.getProductId()));
		request.setAttribute("conflicts", ConflictDAO.conflictsFor(product));
		request.setAttribute("quickDays", quickDays);
		request.setAttribute("dayLimit", dayLimit);
		request.setAttribute("dayInit", Math.min(dayLimit, ConfigDAO.DEFAULT_DAYS));
		render(request, response, "product/view", product.getName());
	}
}
