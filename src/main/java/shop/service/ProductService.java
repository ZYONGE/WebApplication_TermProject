package shop.service;

import java.sql.SQLException;
import java.util.List;

import shop.dao.CategoryDAO;
import shop.dao.ProductDAO;
import shop.dao.QnaDAO;
import shop.dao.ReviewDAO;
import shop.dto.CategoryDTO;
import shop.dto.PageDTO;
import shop.dto.ProductDTO;
import shop.dto.QnaDTO;
import shop.dto.ReviewDTO;

/*
  상품 탐색 — 메인 · 목록 · 상세.

  ★ 이 서비스의 조회는 전부 노출 조건이 걸린 메서드를 쓴다
    (ProductDAO.findOnSale*). 관리자 조회는 AdminService 가 담당한다.
    두 경로를 섞지 않는 것이 노출 규칙을 지키는 방법이다 (ERD § 2.4).
*/
public class ProductService {

	private final ProductDAO productDAO = new ProductDAO();
	private final CategoryDAO categoryDAO = new CategoryDAO();
	private final ReviewDAO reviewDAO = new ReviewDAO();
	private final QnaDAO qnaDAO = new QnaDAO();

	public List<CategoryDTO> findCategoryTree() throws SQLException {
		return categoryDAO.findTree();
	}

	public List<CategoryDTO> findAllCategories() throws SQLException {
		return categoryDAO.findAll();
	}

	public CategoryDTO findCategory(int categoryId) throws SQLException {
		return categoryDAO.findById(categoryId);
	}

	public List<ProductDTO> findLatest(int limit) throws SQLException {
		return productDAO.findLatestOnSale(limit);
	}

	public PageDTO<ProductDTO> findList(Integer categoryId, String keyword, String sort,
			int page, int size) throws SQLException {
		return productDAO.findOnSalePage(categoryId, keyword, sort, page, size);
	}

	/*
	  상품 상세.

	  직접 URL 로 들어와도 노출 조건을 적용한다 — 숨김이거나 품절이면 null 이다.
	  이미지 갤러리는 여기서 함께 채운다.
	*/
	public ProductDTO findDetail(int productId) throws SQLException {
		ProductDTO product = productDAO.findOnSaleById(productId);
		if (product == null) {
			return null;
		}
		product.setImages(productDAO.findImages(productId));
		return product;
	}

	public List<ReviewDTO> findReviews(int productId) throws SQLException {
		return reviewDAO.findByProduct(productId);
	}

	/*
	  상품 문의 목록.

	  비밀글은 본문을 지운 채 돌려준다. 목록에서 "비밀글입니다" 로 표시하되
	  작성자 본인과 관리자에게는 보여준다.
	  ★ 가리는 일을 JSP 에 맡기지 않는다 — 화면이 안 가리면 그대로 샌다.
	*/
	public List<QnaDTO> findQnaList(int productId, int viewerId, boolean admin)
			throws SQLException {
		List<QnaDTO> list = qnaDAO.findByProduct(productId);
		for (QnaDTO qna : list) {
			boolean mine = viewerId > 0 && viewerId == qna.getMemberId();
			if (qna.isSecretPost() && !mine && !admin) {
				qna.setQuestion(null);
				qna.setAnswer(null);
			}
		}
		return list;
	}
}
