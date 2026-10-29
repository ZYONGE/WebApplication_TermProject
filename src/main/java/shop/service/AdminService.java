package shop.service;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import shop.dao.AdminLogDAO;
import shop.dao.CategoryDAO;
import shop.dao.DeliveryDAO;
import shop.dao.MemberDAO;
import shop.dao.NoticeDAO;
import shop.dao.OrderDAO;
import shop.dao.ProductDAO;
import shop.dao.QnaDAO;
import shop.dao.ReviewDAO;
import shop.dao.StatsDAO;
import shop.dto.CategoryDTO;
import shop.dto.DeliveryDTO;
import shop.dto.DeliveryStatusCountDTO;
import shop.dto.LowStockDTO;
import shop.dto.MemberDTO;
import shop.dto.OrderDTO;
import shop.dto.PageDTO;
import shop.dto.ProductDTO;
import shop.dto.QnaDTO;
import shop.dto.ReviewDTO;
import shop.dto.SalesDailyDTO;
import shop.util.DBUtil;
import shop.util.StringUtil;

/*
  관리자 기능.

  ★ 모든 변경은 admin_log 에 남는다. 본 작업과 같은 트랜잭션에 넣어
    "작업은 됐는데 로그가 없는" 상태를 만들지 않는다.
  ★ 조회는 노출 조건이 없는 findAny* 를 쓴다. 관리자는 숨김 · 품절도 봐야 한다.
*/
public class AdminService {

	private final ProductDAO productDAO = new ProductDAO();
	private final CategoryDAO categoryDAO = new CategoryDAO();
	private final OrderDAO orderDAO = new OrderDAO();
	private final DeliveryDAO deliveryDAO = new DeliveryDAO();
	private final MemberDAO memberDAO = new MemberDAO();
	private final QnaDAO qnaDAO = new QnaDAO();
	private final NoticeDAO noticeDAO = new NoticeDAO();
	private final ReviewDAO reviewDAO = new ReviewDAO();
	private final StatsDAO statsDAO = new StatsDAO();
	private final AdminLogDAO adminLogDAO = new AdminLogDAO();

	/* ---------------- 대시보드 ---------------- */

	public Map<String, Integer> findSummary() throws SQLException {
		return statsDAO.findSummary();
	}

	public List<SalesDailyDTO> findSalesDaily(int days) throws SQLException {
		return statsDAO.findSalesDaily(days);
	}

	public List<DeliveryStatusCountDTO> findDeliveryStatusCount() throws SQLException {
		return statsDAO.findDeliveryStatusCount();
	}

	public List<LowStockDTO> findLowStock() throws SQLException {
		return statsDAO.findLowStock();
	}

	/* ---------------- 상품 ---------------- */

	public PageDTO<ProductDTO> findProducts(Integer categoryId, String keyword, String status,
			int page, int size) throws SQLException {
		return productDAO.findAnyPage(categoryId, keyword, status, page, size);
	}

	public ProductDTO findProduct(int productId) throws SQLException {
		ProductDTO product = productDAO.findAnyById(productId);
		if (product != null) {
			product.setImages(productDAO.findImages(productId));
		}
		return product;
	}

	public int createProduct(int adminId, ProductDTO product) throws SQLException {
		validateProduct(product);
		int productId = productDAO.insert(product);
		adminLogDAO.insert(adminId, AdminLogDAO.MODE_ADMIN, AdminLogDAO.PRODUCT_CREATE,
				"product", Integer.valueOf(productId),
				"등록: " + product.getName() + " / " + product.getPrice() + "원 / 재고 "
				+ product.getStock());
		return productId;
	}

	public void updateProduct(int adminId, ProductDTO product) throws SQLException {
		validateProduct(product);
		ProductDTO before = productDAO.findAnyById(product.getProductId());
		if (before == null) {
			throw new ServiceException("상품을 찾을 수 없습니다.");
		}
		productDAO.update(product);
		adminLogDAO.insert(adminId, AdminLogDAO.MODE_ADMIN, AdminLogDAO.PRODUCT_UPDATE,
				"product", Integer.valueOf(product.getProductId()),
				"가격 " + before.getPrice() + " -> " + product.getPrice()
				+ " / 재고 " + before.getStock() + " -> " + product.getStock()
				+ " / 상태 " + before.getStatus() + " -> " + product.getStatus());
	}

	/*
	  상품 삭제.

	  order_item FK 가 RESTRICT 라 주문된 적 있는 상품은 삭제가 거부된다.
	  그래서 화면은 '숨김' 처리를 권하고, 삭제 실패를 그대로 알려 준다
	  (CRUD.md § 4 product 행).
	*/
	public void deleteProduct(int adminId, int productId) throws SQLException {
		ProductDTO product = productDAO.findAnyById(productId);
		if (product == null) {
			throw new ServiceException("상품을 찾을 수 없습니다.");
		}
		try {
			productDAO.delete(productId);
		} catch (SQLException e) {
			throw new ServiceException("주문 이력이 있는 상품은 삭제할 수 없습니다."
					+ " 상태를 '숨김' 으로 바꾸세요.");
		}
		adminLogDAO.insert(adminId, AdminLogDAO.MODE_ADMIN, AdminLogDAO.PRODUCT_DELETE,
				"product", Integer.valueOf(productId), "삭제: " + product.getName());
	}

	public void addProductImage(int productId, String imgPath) throws SQLException {
		productDAO.insertImage(productId, imgPath, productDAO.nextImageSortOrder(productId));
	}

	public void deleteProductImage(int imageId) throws SQLException {
		productDAO.deleteImage(imageId);
	}

	private void validateProduct(ProductDTO product) {
		if (StringUtil.isEmpty(product.getName())) {
			throw new ServiceException("상품명을 입력하세요.");
		}
		if (product.getCategoryId() <= 0) {
			throw new ServiceException("카테고리를 선택하세요.");
		}
		if (product.getPrice() < 0) {
			throw new ServiceException("가격은 0원 이상이어야 합니다.");
		}
		if (product.getStock() < 0) {
			throw new ServiceException("재고는 0개 이상이어야 합니다.");
		}
		if (!"판매중".equals(product.getStatus()) && !"숨김".equals(product.getStatus())) {
			throw new ServiceException("상품 상태가 올바르지 않습니다.");
		}
	}

	/* ---------------- 카테고리 ---------------- */

	public List<CategoryDTO> findCategories() throws SQLException {
		return categoryDAO.findAll();
	}

	public List<CategoryDTO> findCategoryTree() throws SQLException {
		return categoryDAO.findTree();
	}

	public void createCategory(int adminId, CategoryDTO category) throws SQLException {
		validateCategory(category, 0);
		categoryDAO.insert(category);
		adminLogDAO.insert(adminId, AdminLogDAO.MODE_ADMIN, AdminLogDAO.CATEGORY_CREATE,
				"category", null, "등록: " + category.getName());
	}

	public void updateCategory(int adminId, CategoryDTO category) throws SQLException {
		validateCategory(category, category.getCategoryId());
		categoryDAO.update(category);
		adminLogDAO.insert(adminId, AdminLogDAO.MODE_ADMIN, AdminLogDAO.CATEGORY_UPDATE,
				"category", Integer.valueOf(category.getCategoryId()),
				"수정: " + category.getName());
	}

	public void deleteCategory(int adminId, int categoryId) throws SQLException {
		if (categoryDAO.countChildren(categoryId) > 0) {
			throw new ServiceException("하위 카테고리가 있어 삭제할 수 없습니다.");
		}
		if (categoryDAO.countProducts(categoryId) > 0) {
			throw new ServiceException("이 카테고리에 속한 상품이 있어 삭제할 수 없습니다.");
		}
		categoryDAO.delete(categoryId);
		adminLogDAO.insert(adminId, AdminLogDAO.MODE_ADMIN, AdminLogDAO.CATEGORY_DELETE,
				"category", Integer.valueOf(categoryId), "삭제");
	}

	/*
	  대분류 이름 중복은 여기서 막는다.

	  UNIQUE (parent_id, name) 이 parent_id IS NULL 을 보호하지 못하기 때문이다
	  (schema.sql category 주석). DB 가 못 막는 자리를 화면 쪽에서 메운다.
	*/
	private void validateCategory(CategoryDTO category, int exceptId) throws SQLException {
		if (StringUtil.isEmpty(category.getName())) {
			throw new ServiceException("카테고리 이름을 입력하세요.");
		}
		boolean root = category.getParentId() == null || category.getParentId().intValue() == 0;
		if (root && categoryDAO.existsRootName(category.getName(), exceptId)) {
			throw new ServiceException("같은 이름의 대분류가 이미 있습니다.");
		}
	}

	/* ---------------- 주문 · 배송 ---------------- */

	public PageDTO<OrderDTO> findOrders(String keyword, String deliveryStatus,
			String orderStatus, int page, int size) throws SQLException {
		return orderDAO.findAdminPage(keyword, deliveryStatus, orderStatus, page, size);
	}

	/*
	  다음 단계로 (ERD § 6.2).

	      UPDATE delivery SET status = ?, <시각 컬럼> = NOW()
	       WHERE delivery_id = ? AND status = <직전 상태>

	  영향 행이 0 이면 잘못된 전이이거나 동시 클릭이므로 롤백한다.
	  배송중으로 넘어갈 때는 운송장이 필수다 — CHECK 가 DB 에서도 막는다.
	*/
	public void moveDeliveryNext(int adminId, int deliveryId, String courier, String trackingNo)
			throws SQLException {
		DeliveryDTO delivery = deliveryDAO.findById(deliveryId);
		if (delivery == null) {
			throw new ServiceException("배송 정보를 찾을 수 없습니다.");
		}
		String from = delivery.getStatus();
		String to = DeliveryDAO.nextOf(from);
		if (to == null) {
			throw new ServiceException("이미 배송완료된 주문입니다.");
		}
		if ("배송중".equals(to)
				&& (StringUtil.isEmpty(courier) || StringUtil.isEmpty(trackingNo))) {
			throw new ServiceException("택배사와 운송장 번호를 입력해야 배송중으로 넘어갑니다.");
		}

		Connection conn = null;
		try {
			conn = DBUtil.beginTransaction();

			int affected = deliveryDAO.moveNext(conn, deliveryId, from, to, courier, trackingNo);
			if (affected == 0) {
				throw new ServiceException("배송 상태가 이미 바뀌었습니다. 새로고침 후 다시 시도하세요.");
			}
			deliveryDAO.insertHistory(conn, deliveryId, from, to, Integer.valueOf(adminId), false);
			adminLogDAO.insert(conn, adminId, AdminLogDAO.MODE_ADMIN,
					AdminLogDAO.DELIVERY_NEXT_STEP, "delivery", Integer.valueOf(deliveryId),
					from + " -> " + to);
			if ("배송중".equals(to)) {
				adminLogDAO.insert(conn, adminId, AdminLogDAO.MODE_ADMIN,
						AdminLogDAO.DELIVERY_TRACKING_SET, "delivery", Integer.valueOf(deliveryId),
						courier + " / " + trackingNo);
			}

			DBUtil.commit(conn);
		} catch (SQLException e) {
			DBUtil.rollback(conn);
			throw e;
		} catch (RuntimeException e) {
			DBUtil.rollback(conn);
			throw e;
		} finally {
			DBUtil.release(conn);
		}
	}

	/* ---------------- 회원 ---------------- */

	public PageDTO<MemberDTO> findMembers(String keyword, String role, int page, int size)
			throws SQLException {
		return memberDAO.findPage(keyword, role, page, size);
	}

	/*
	  권한 변경.

	  ★ ADMIN 은 DEV 권한을 줄 수 없다 (ERD § 5).
	    필터는 "경로 접근" 만 막는다. 권한 상승은 처리 로직에서 막아야 하는 문제다.
	    요청 파라미터가 DEV 면 여기서 거부하고, DAO 와 SQL 에도 같은 가드를 뒀다.
	*/
	public void changeRole(int adminId, int memberId, String role) throws SQLException {
		if (!"USER".equals(role) && !"ADMIN".equals(role)) {
			throw new ServiceException("부여할 수 없는 권한입니다.");
		}
		MemberDTO target = memberDAO.findById(memberId);
		if (target == null) {
			throw new ServiceException("회원을 찾을 수 없습니다.");
		}
		if ("DEV".equals(target.getRole())) {
			throw new ServiceException("DEV 계정의 권한은 변경할 수 없습니다.");
		}
		if (adminId == memberId) {
			throw new ServiceException("자기 자신의 권한은 변경할 수 없습니다.");
		}

		Connection conn = null;
		try {
			conn = DBUtil.beginTransaction();
			if (memberDAO.updateRole(conn, memberId, role) == 0) {
				throw new ServiceException("권한을 변경하지 못했습니다.");
			}
			adminLogDAO.insert(conn, adminId, AdminLogDAO.MODE_ADMIN,
					AdminLogDAO.MEMBER_ROLE_CHANGE, "member", Integer.valueOf(memberId),
					target.getLoginId() + ": " + target.getRole() + " -> " + role);
			DBUtil.commit(conn);
		} catch (SQLException e) {
			DBUtil.rollback(conn);
			throw e;
		} catch (RuntimeException e) {
			DBUtil.rollback(conn);
			throw e;
		} finally {
			DBUtil.release(conn);
		}
	}

	/* ---------------- 문의 답변 ---------------- */

	public PageDTO<QnaDTO> findQna(String keyword, boolean onlyUnanswered, int page, int size)
			throws SQLException {
		return qnaDAO.findAdminPage(keyword, onlyUnanswered, page, size);
	}

	public void answerQna(int adminId, int qnaId, String answer) throws SQLException {
		if (StringUtil.isEmpty(answer)) {
			throw new ServiceException("답변 내용을 입력하세요.");
		}
		if (answer.length() > 1000) {
			throw new ServiceException("답변은 1000자 이내로 입력하세요.");
		}

		Connection conn = null;
		try {
			conn = DBUtil.beginTransaction();
			if (qnaDAO.answer(conn, qnaId, answer) == 0) {
				throw new ServiceException("문의를 찾을 수 없습니다.");
			}
			adminLogDAO.insert(conn, adminId, AdminLogDAO.MODE_ADMIN, AdminLogDAO.QNA_ANSWER,
					"qna", Integer.valueOf(qnaId), "답변 등록");
			DBUtil.commit(conn);
		} catch (SQLException e) {
			DBUtil.rollback(conn);
			throw e;
		} catch (RuntimeException e) {
			DBUtil.rollback(conn);
			throw e;
		} finally {
			DBUtil.release(conn);
		}
	}

	/* ---------------- 공지 ---------------- */

	public PageDTO<shop.dto.NoticeDTO> findNotices(String keyword, int page, int size)
			throws SQLException {
		return noticeDAO.findPage(keyword, page, size);
	}

	public shop.dto.NoticeDTO findNotice(int noticeId) throws SQLException {
		return noticeDAO.findById(noticeId);
	}

	public void createNotice(int adminId, String title, String content) throws SQLException {
		validateNotice(title, content);
		noticeDAO.insert(title, content);
		adminLogDAO.insert(adminId, AdminLogDAO.MODE_ADMIN, AdminLogDAO.NOTICE_CREATE,
				"notice", null, "등록: " + title);
	}

	public void updateNotice(int adminId, int noticeId, String title, String content)
			throws SQLException {
		validateNotice(title, content);
		if (noticeDAO.update(noticeId, title, content) == 0) {
			throw new ServiceException("공지를 찾을 수 없습니다.");
		}
		adminLogDAO.insert(adminId, AdminLogDAO.MODE_ADMIN, AdminLogDAO.NOTICE_UPDATE,
				"notice", Integer.valueOf(noticeId), "수정: " + title);
	}

	public void deleteNotice(int adminId, int noticeId) throws SQLException {
		noticeDAO.delete(noticeId);
		adminLogDAO.insert(adminId, AdminLogDAO.MODE_ADMIN, AdminLogDAO.NOTICE_DELETE,
				"notice", Integer.valueOf(noticeId), "삭제");
	}

	private void validateNotice(String title, String content) {
		if (StringUtil.isEmpty(title) || title.length() > 200) {
			throw new ServiceException("제목을 200자 이내로 입력하세요.");
		}
		if (StringUtil.isEmpty(content)) {
			throw new ServiceException("내용을 입력하세요.");
		}
	}

	/* ---------------- 리뷰 관리 ---------------- */

	public PageDTO<ReviewDTO> findReviews(String keyword, int page, int size)
			throws SQLException {
		return reviewDAO.findAdminPage(keyword, page, size);
	}

	/*
	  관리자 리뷰 삭제.

	  ADMIN 에게는 D 만 있고 U 가 없다 (CRUD.md § 6-3). 남의 글 내용을 고치는
	  경로를 아예 만들지 않고, 부적절한 리뷰는 내리는 것으로만 처리한다.
	*/
	public void deleteReview(int adminId, int reviewId) throws SQLException {
		ReviewDTO review = reviewDAO.findById(reviewId);
		if (review == null) {
			throw new ServiceException("리뷰를 찾을 수 없습니다.");
		}
		reviewDAO.deleteByAdmin(reviewId);
		adminLogDAO.insert(adminId, AdminLogDAO.MODE_ADMIN, AdminLogDAO.REVIEW_DELETE,
				"review", Integer.valueOf(reviewId),
				"삭제: " + review.getProductName() + " / 작성자 " + review.getMemberName());
	}
}
