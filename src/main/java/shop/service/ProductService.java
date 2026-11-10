package shop.service;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import shop.dao.CategoryDAO;
import shop.dao.ProductDAO;
import shop.dao.QnaDAO;
import shop.dao.ReviewDAO;
import shop.dao.TrackItemDAO;
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
	private final TrackItemDAO trackItemDAO = new TrackItemDAO();

	public List<CategoryDTO> findCategoryTree() throws SQLException {
		return categoryDAO.findTree();
	}

	public List<CategoryDTO> findAllCategories() throws SQLException {
		return categoryDAO.findAllOrdered();
	}

	public CategoryDTO findCategory(int categoryId) throws SQLException {
		return categoryDAO.findById(categoryId);
	}

	public List<ProductDTO> findLatest(int limit) throws SQLException {
		return productDAO.findLatestOnSale(limit);
	}

	public PageDTO<ProductDTO> findList(Integer categoryId, String type, String keyword,
			String sort, int page, int size) throws SQLException {
		PageDTO<ProductDTO> result = productDAO.findOnSalePage(categoryId, type, keyword, sort, page, size);
		fillTrackItems(result.getList());
		return result;
	}

	/* 대분류 id → 그 밑 상품 구분 (목록 필터가 "탭" 을 정할 때) */
	public Map<Integer, String> findRootTypes() throws SQLException {
		return productDAO.findRootTypes();
	}

	public List<ProductDTO> findLatestSingles(int limit) throws SQLException {
		return productDAO.findLatestOnSaleByType(ProductDAO.SINGLE, limit);
	}

	/* 메인 트랙 패키지 영역 — 구성까지 채워 카드에 "비타민C + 비타민D …" 를 보여준다 */
	public List<ProductDTO> findLatestTracks(int limit) throws SQLException {
		List<ProductDTO> list = productDAO.findLatestOnSaleByType(ProductDAO.TRACK, limit);
		fillTrackItems(list);
		return list;
	}

	/* 조합 탭 — 고를 수 있는 낱개 (판매중 · 재고 > 0) */
	public List<ProductDTO> findSinglesOnSale() throws SQLException {
		return productDAO.findOnSaleByType(ProductDAO.SINGLE);
	}

	/* 조합 탭 — 출발점으로 불러올 수 있는 트랙 (판매중 · 재고 > 0) + 구성 */
	public List<ProductDTO> findTracksOnSale() throws SQLException {
		List<ProductDTO> list = productDAO.findOnSaleByType(ProductDAO.TRACK);
		fillTrackItems(list);
		return list;
	}

	/* 트랙 행에 구성 낱개를 한 번의 조회로 채운다 (트랙마다 조회하면 N+1) */
	private void fillTrackItems(List<ProductDTO> list) throws SQLException {
		List<Integer> trackIds = new ArrayList<Integer>();
		for (ProductDTO p : list) {
			if (p.isTrack()) {
				trackIds.add(Integer.valueOf(p.getProductId()));
			}
		}
		Map<Integer, List<ProductDTO>> items = trackItemDAO.findItemsByTracks(trackIds);
		for (ProductDTO p : list) {
			if (p.isTrack()) {
				p.setTrackItems(items.get(Integer.valueOf(p.getProductId())));
			}
		}
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
		if (product.isTrack()) {
			product.setTrackItems(trackItemDAO.findItems(productId));
		}
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
