package shop.controller;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Enumeration;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.oreilly.servlet.MultipartRequest;
import com.oreilly.servlet.multipart.DefaultFileRenamePolicy;

import shop.dto.ProductDTO;
import shop.service.AdminService;
import shop.service.ServiceException;
import shop.util.Paging;
import shop.util.WebUtil;

/*
  23번 상품 관리 (CRUD.md § 3)

  ★ 이미지 업로드는 cos.jar 를 쓴다 (CLAUDE.md § 3 Stage 2).
    저장 경로는 WebContents/upload/, 웹 참조 경로는 /upload/<파일명> 으로 고정이다
    (CLAUDE.md § 4.1 공유 지점).
  ★ multipart 요청은 request.getParameter() 가 동작하지 않는다. MultipartRequest
    객체에서 꺼내야 한다 — 그래서 등록 · 수정 경로만 처리가 다르다.
*/
@WebServlet(name = "AdminProductServlet", urlPatterns = { "/admin/products" })
public class AdminProductServlet extends BaseServlet {

	private static final long serialVersionUID = 1L;

	/* 업로드 한 건의 상한 — 넘으면 cos.jar 가 IOException 을 던진다. */
	private static final int MAX_UPLOAD_SIZE = 10 * 1024 * 1024;

	private final AdminService adminService = new AdminService();

	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		try {
			request.setAttribute("pageTitle", "상품 관리");
			pageCss(request, "admin");

			int categoryId = intParam(request, "categoryId", 0);
			request.setAttribute("pageData", adminService.findProducts(
					categoryId > 0 ? Integer.valueOf(categoryId) : null,
					param(request, "keyword"), param(request, "status"),
					intParam(request, "page", 1), Paging.DEFAULT_SIZE));
			request.setAttribute("categories", adminService.findCategories());
			request.setAttribute("pageUrl", "/admin/products?");

			int editId = intParam(request, "editId", 0);
			if (editId > 0) {
				request.setAttribute("editProduct", adminService.findProduct(editId));
			}
			render(request, response, "/admin/products.jsp");
		} catch (SQLException e) {
			throw wrap(e);
		}
	}

	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		int adminId = loginMemberId(request);
		String contentType = request.getContentType();

		try {
			if (contentType != null && contentType.toLowerCase().startsWith("multipart/")) {
				saveWithUpload(request, adminId);
			} else {
				String mode = param(request, "mode", "");
				if ("delete".equals(mode)) {
					adminService.deleteProduct(adminId, intParam(request, "productId", 0));
					flash(request, "상품을 삭제했습니다.");
				} else if ("deleteImage".equals(mode)) {
					adminService.deleteProductImage(intParam(request, "imageId", 0));
					flash(request, "이미지를 삭제했습니다.");
				}
			}
		} catch (ServiceException e) {
			flashError(request, e.getMessage());
		} catch (SQLException e) {
			throw wrap(e);
		}
		redirect(request, response, "/admin/products");
	}

	/* 등록 · 수정 — 파일이 함께 올 수 있다. */
	private void saveWithUpload(HttpServletRequest request, int adminId)
			throws IOException, SQLException {
		String uploadDir = getServletContext().getRealPath("/upload");
		File dir = new File(uploadDir);
		if (!dir.exists()) {
			dir.mkdirs();
		}

		/* 같은 이름의 파일이 와도 덮어쓰지 않게 cos.jar 가 이름을 바꿔 준다. */
		MultipartRequest multi = new MultipartRequest(request, uploadDir, MAX_UPLOAD_SIZE,
				"UTF-8", new DefaultFileRenamePolicy());

		ProductDTO product = new ProductDTO();
		product.setProductId(WebUtil.toInt(multi.getParameter("productId"), 0));
		product.setCategoryId(WebUtil.toInt(multi.getParameter("categoryId"), 0));
		product.setName(trim(multi.getParameter("name")));
		product.setPrice(WebUtil.toInt(multi.getParameter("price"), 0));
		product.setStock(WebUtil.toInt(multi.getParameter("stock"), 0));
		product.setDescription(multi.getParameter("description"));
		product.setStatus(trim(multi.getParameter("status")));

		/* 대표 이미지 — 새로 올라오지 않았으면 기존 값을 유지한다. */
		String thumbnailPath = webPath(multi.getFilesystemName("thumbnail"));
		if (thumbnailPath == null) {
			thumbnailPath = trim(multi.getParameter("currentThumbnail"));
		}
		product.setThumbnail(thumbnailPath);

		int productId;
		if (product.getProductId() > 0) {
			adminService.updateProduct(adminId, product);
			productId = product.getProductId();
			flash(request, "상품을 수정했습니다.");
		} else {
			productId = adminService.createProduct(adminId, product);
			flash(request, "상품을 등록했습니다.");
		}

		/* 추가 이미지 — 이름이 image 로 시작하는 입력을 모두 훑는다. */
		Enumeration<?> names = multi.getFileNames();
		while (names.hasMoreElements()) {
			String name = (String) names.nextElement();
			if ("thumbnail".equals(name)) {
				continue;
			}
			String path = webPath(multi.getFilesystemName(name));
			if (path != null) {
				adminService.addProductImage(productId, path);
			}
		}
	}

	/* 저장은 WebContents/upload/, 웹 참조는 /upload/<파일명> — 양 트랙 공통 계약이다. */
	private String webPath(String filename) {
		return filename == null ? null : "/upload/" + filename;
	}

	private String trim(String value) {
		return value == null ? null : value.trim();
	}
}
