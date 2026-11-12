package shop.controller;

import java.io.IOException;
import java.sql.SQLException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import shop.dao.ProductDAO;
import shop.dto.ProductDTO;
import shop.service.ConfigService;
import shop.service.IntakeService;
import shop.service.MypageService;
import shop.service.ProductService;
import shop.util.Paging;

/* 4 · 5번 상품목록 · 상품상세 (CRUD.md § 2) */
@WebServlet(name = "ProductServlet", urlPatterns = { "/product/list", "/product/view" })
public class ProductServlet extends BaseServlet {

	private static final long serialVersionUID = 1L;

	private final ProductService productService = new ProductService();
	private final MypageService mypageService = new MypageService();
	private final IntakeService intakeService = new IntakeService();
	private final ConfigService configService = new ConfigService();

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
		/* 낱개 / 트랙 구분 — 허용 값이 아니면 전체 */
		String type = param(request, "type");
		if (!ProductDAO.isType(type)) {
			type = null;
		}
		String keyword = param(request, "keyword");
		String sort = param(request, "sort", "latest");
		int page = intParam(request, "page", 1);

		request.setAttribute("pageTitle", "상품 목록");
		request.setAttribute("pageData", productService.findList(
				categoryId > 0 ? Integer.valueOf(categoryId) : null,
				type, keyword, sort, page, Paging.DEFAULT_SIZE));
		request.setAttribute("currentCategory",
				categoryId > 0 ? productService.findCategory(categoryId) : null);
		/* 화면 비교용 정수 — JSP 가 param.categoryId(문자열)를 숫자와 비교하면 'abc' 에서 ELException 이 난다 */
		request.setAttribute("selectedCategoryId", Integer.valueOf(categoryId));

		/*
		  카테고리 필터의 선택지 = 지금 탭의 대분류 묶음 (구분 필터는 두지 않는다).
		      전체 영양제 탭            → 모든 대분류와 소분류
		      낱개 영양제 · 트랙 패키지 탭 → 그 대분류와 그 밑 소분류만
		  탭은 categoryId 가 속한 대분류로 정한다. ?type=TRACK 처럼 구분으로 들어오면
		  그 구분의 상품이 있는 대분류로 정한다 — 대분류 id · 이름을 코드에 박지 않는다.
		*/
		java.util.List<shop.dto.CategoryDTO> tree = productService.findCategoryTree();
		java.util.List<shop.dto.CategoryDTO> tabRoots = new java.util.ArrayList<shop.dto.CategoryDTO>();
		if (categoryId > 0) {
			for (shop.dto.CategoryDTO root : tree) {
				boolean mine = root.getCategoryId() == categoryId;
				for (shop.dto.CategoryDTO sub : root.getChildren()) {
					mine = mine || sub.getCategoryId() == categoryId;
				}
				if (mine) {
					tabRoots.add(root);
				}
			}
		} else if (type != null) {
			java.util.Map<Integer, String> rootTypes = productService.findRootTypes();
			for (shop.dto.CategoryDTO root : tree) {
				if (type.equals(rootTypes.get(Integer.valueOf(root.getCategoryId())))) {
					tabRoots.add(root);
				}
			}
		}
		boolean tabMode = !tabRoots.isEmpty();
		request.setAttribute("filterRoots", tabMode ? tabRoots : tree);
		request.setAttribute("tabMode", Boolean.valueOf(tabMode));
		request.setAttribute("tabTitle", tabRoots.size() == 1 ? tabRoots.get(0).getName() : null);

		/* 페이지 번호만 갈아 끼우면 되도록 나머지 조건을 미리 붙여 둔다. */
		StringBuilder url = new StringBuilder("/product/list?");
		if (categoryId > 0) {
			url.append("categoryId=").append(categoryId).append("&");
		}
		if (type != null) {
			url.append("type=").append(type).append("&");
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

		/* 함께 드시지 말 것 — 트랙이면 구성품 전체 기준 (ERD § 7.1 경고 위치) */
		request.setAttribute("conflicts", intakeService.findConflictsFor(product));
		request.setAttribute("quickDays", configService.quickDays());
		request.setAttribute("maxDays", Integer.valueOf(configService.maxIntakeDays()));
		request.setAttribute("defaultDays", Integer.valueOf(ConfigService.DEFAULT_DAYS));

		render(request, response, "/product/view.jsp");
	}
}
