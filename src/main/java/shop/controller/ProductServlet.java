package shop.controller;

import java.io.IOException;
import java.sql.SQLException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import shop.dto.ProductDTO;
import shop.service.MypageService;
import shop.service.ProductService;
import shop.util.Paging;

/* 4 · 5번 상품목록 · 상품상세 (CRUD.md § 2) */
@WebServlet(name = "ProductServlet", urlPatterns = { "/product/list", "/product/view" })
public class ProductServlet extends BaseServlet {

	private static final long serialVersionUID = 1L;

	private final ProductService productService = new ProductService();
	private final MypageService mypageService = new MypageService();

	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		String action = action(request);
		pageCss(request, "product");
		try {
			if ("view".equals(action)) {
				view(request, response);
			} else {
				list(request, response);
			}
		} catch (SQLException e) {
			throw wrap(e);
		}
	}

	private void list(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException, SQLException {
		int categoryId = intParam(request, "categoryId", 0);
		String keyword = param(request, "keyword");
		String sort = param(request, "sort", "latest");
		int page = intParam(request, "page", 1);

		request.setAttribute("pageTitle", "상품 목록");
		request.setAttribute("pageData", productService.findList(
				categoryId > 0 ? Integer.valueOf(categoryId) : null,
				keyword, sort, page, Paging.DEFAULT_SIZE));
		request.setAttribute("categories", productService.findAllCategories());
		request.setAttribute("currentCategory",
				categoryId > 0 ? productService.findCategory(categoryId) : null);

		/* 페이지 번호만 갈아 끼우면 되도록 나머지 조건을 미리 붙여 둔다. */
		StringBuilder url = new StringBuilder("/product/list?");
		if (categoryId > 0) {
			url.append("categoryId=").append(categoryId).append("&");
		}
		if (keyword != null && keyword.length() > 0) {
			url.append("keyword=").append(java.net.URLEncoder.encode(keyword, "UTF-8")).append("&");
		}
		url.append("sort=").append(sort).append("&");
		request.setAttribute("pageUrl", url.toString());

		render(request, response, "/product/list.jsp");
	}

	/*
	  상품 상세.

	  노출 조건을 통과하지 못하면(숨김 · 품절) null 이 온다. 직접 URL 로 들어와도
	  같다 — 조건은 서비스가 걸고, 화면은 그 결과만 본다 (ERD § 2.4).
	*/
	private void view(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException, SQLException {
		int productId = intParam(request, "productId", 0);
		ProductDTO product = productService.findDetail(productId);
		if (product == null) {
			flashError(request, "판매 중인 상품이 아닙니다.");
			redirect(request, response, "/product/list");
			return;
		}

		int viewerId = loginMemberId(request);
		request.setAttribute("pageTitle", product.getName());
		request.setAttribute("product", product);
		request.setAttribute("reviews", productService.findReviews(productId));
		request.setAttribute("qnaList",
				productService.findQnaList(productId, viewerId, isAdmin(request)));
		request.setAttribute("wished",
				Boolean.valueOf(mypageService.isWished(viewerId, productId)));

		render(request, response, "/product/view.jsp");
	}
}
