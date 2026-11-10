package shop.service;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import shop.dao.AdminLogDAO;
import shop.dao.CategoryDAO;
import shop.dao.ConflictDAO;
import shop.dao.DeliveryDAO;
import shop.dao.MemberDAO;
import shop.dao.NoticeDAO;
import shop.dao.OrderDAO;
import shop.dao.ProductDAO;
import shop.dao.QnaDAO;
import shop.dao.ReviewDAO;
import shop.dao.StatsDAO;
import shop.dao.TrackItemDAO;
import shop.dto.CategoryDTO;
import shop.dto.ConflictDTO;
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
	private final TrackItemDAO trackItemDAO = new TrackItemDAO();
	private final ConflictDAO conflictDAO = new ConflictDAO();

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

	public PageDTO<ProductDTO> findProducts(Integer categoryId, String type, String keyword,
			String status, int page, int size) throws SQLException {
		return productDAO.findAnyPage(categoryId, type, keyword, status, page, size);
	}

	/* 상품 등록 팝업 — 대분류 id → 그 밑 상품 구분 */
	public java.util.Map<Integer, String> findRootTypes() throws SQLException {
		return productDAO.findRootTypes();
	}

	/* 트랙 구성 · 금지 조합에서 고를 낱개 전체 (숨김 · 재고 0 포함) */
	public List<ProductDTO> findAllSingles() throws SQLException {
		return productDAO.findAnyByType(ProductDAO.SINGLE);
	}

	public ProductDTO findProduct(int productId) throws SQLException {
		ProductDTO product = productDAO.findAnyById(productId);
		if (product != null) {
			product.setImages(productDAO.findImages(productId));
			if (product.isTrack()) {
				product.setTrackItems(trackItemDAO.findItems(productId));
			}
		}
		return product;
	}

	/*
	  등록. 트랙이면 구성(itemIds)을 함께 받는다.
	  구성 검사를 먼저 끝내고 상품을 넣는다 — 상품만 생기고 구성이 거부되는 반쪽 상태를 줄인다.
	*/
	public int createProduct(int adminId, ProductDTO product, List<Integer> itemIds) throws SQLException {
		validateProduct(product);
		if (product.isTrack()) {
			validateTrackItems(itemIds);
		}
		int productId = productDAO.insert(product);
		adminLogDAO.insert(adminId, AdminLogDAO.MODE_ADMIN, AdminLogDAO.PRODUCT_CREATE,
				"product", Integer.valueOf(productId),
				"등록: " + product.getName() + " / " + product.getPrice() + "원(1포) / 재고 "
				+ product.getStock() + "포 / " + product.getTypeLabel());
		if (product.isTrack()) {
			saveTrackItems(adminId, productId, itemIds);
		}
		return productId;
	}

	public void updateProduct(int adminId, ProductDTO product, List<Integer> itemIds) throws SQLException {
		validateProduct(product);
		ProductDTO before = productDAO.findAnyById(product.getProductId());
		if (before == null) {
			throw new ServiceException("상품을 찾을 수 없습니다.");
		}
		boolean typeChanged = !before.getProductType().equals(product.getProductType());
		if (typeChanged) {
			/* ERD § 8-15 — 판매 이력이 생긴 뒤에는 구분을 바꾸지 않는다 */
			if (productDAO.countOrderItems(product.getProductId()) > 0) {
				throw new ServiceException("판매 이력이 있는 상품은 낱개 / 트랙 구분을 바꿀 수 없습니다.");
			}
			/* 낱개 → 트랙: 다른 트랙의 구성품이거나 금지 조합에 걸린 낱개는 트랙이 될 수 없다 */
			if (product.isTrack()) {
				List<Integer> self = new ArrayList<Integer>();
				self.add(Integer.valueOf(product.getProductId()));
				if (trackItemDAO.countTracksUsing(product.getProductId()) > 0
						|| !conflictDAO.findInvolving(self).isEmpty()) {
					throw new ServiceException("트랙 구성품이거나 금지 조합에 걸린 낱개는 트랙으로 바꿀 수 없습니다.");
				}
			}
		}
		if (product.isTrack()) {
			validateTrackItems(itemIds);
		}
		productDAO.update(product);
		if (product.isTrack()) {
			saveTrackItems(adminId, product.getProductId(), itemIds);
		} else if (typeChanged) {
			/* 트랙 → 낱개: 남은 구성을 지운다 */
			saveTrackItems(adminId, product.getProductId(), new ArrayList<Integer>());
		}
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

	/*
	  상품 검사.
	  ★ 소분류에만 등록한다. 한 대분류 밑에 낱개와 트랙을 섞지 않는다 (ERD § 8-12).
	    대분류 이름을 코드에 박지 않는다 — 그 대분류에 이미 있는 상품의 구분을 보고 판정한다.
	    (샘플 데이터 기준 '낱개 영양제' = SINGLE, '트랙 패키지' = TRACK. 이름을 바꿔도 규칙은 그대로다)
	*/
	private void validateProduct(ProductDTO product) throws SQLException {
		if (StringUtil.isEmpty(product.getName())) {
			throw new ServiceException("상품명을 입력하세요.");
		}
		if (!ProductDAO.isType(product.getProductType())) {
			throw new ServiceException("낱개 / 트랙 구분을 선택하세요.");
		}
		if (product.getCategoryId() <= 0) {
			throw new ServiceException("카테고리를 선택하세요.");
		}
		CategoryDTO category = categoryDAO.findById(product.getCategoryId());
		if (category == null || category.getParentId() == null) {
			throw new ServiceException("상품은 소분류에만 등록할 수 있습니다.");
		}
		CategoryDTO root = categoryDAO.findById(category.getParentId().intValue());
		String otherType = product.isTrack() ? ProductDAO.SINGLE : ProductDAO.TRACK;
		if (root != null && productDAO.countUnderRoot(root.getCategoryId(), otherType,
				product.getProductId()) > 0) {
			throw new ServiceException("'" + root.getName() + "' 대분류에는 "
					+ (product.isTrack() ? "낱개" : "트랙") + " 상품이 있습니다. "
					+ product.getTypeLabel() + " 상품은 " + product.getTypeLabel()
					+ " 상품이 있는 대분류의 소분류에 등록하세요.");
		}
		if (product.getPrice() < 0) {
			throw new ServiceException("가격은 0원 이상이어야 합니다.");
		}
		if (product.getStock() < 0) {
			throw new ServiceException("재고는 0포 이상이어야 합니다.");
		}
		if (!"판매중".equals(product.getStatus()) && !"숨김".equals(product.getStatus())) {
			throw new ServiceException("상품 상태가 올바르지 않습니다.");
		}
	}

	/* ---------------- 트랙 구성 (ERD § 7.1 트랙 구성) ---------------- */

	/*
	  구성 검사 — 낱개만, 1개 이상, 구성품끼리 금지 조합이 없을 것 (ERD § 8-9 · § 8-14).
	*/
	private void validateTrackItems(List<Integer> itemIds) throws SQLException {
		LinkedHashSet<Integer> ids = new LinkedHashSet<Integer>(itemIds);
		if (ids.isEmpty()) {
			throw new ServiceException("트랙 구성 낱개를 하나 이상 고르세요.");
		}
		for (ProductDTO p : productDAO.findAnyByIds(ids).values()) {
			if (p.isTrack()) {
				throw new ServiceException("트랙 구성에는 낱개 영양제만 넣을 수 있습니다.");
			}
		}
		if (productDAO.findAnyByIds(ids).size() != ids.size()) {
			throw new ServiceException("없는 상품이 구성에 들어 있습니다.");
		}
		List<ConflictDTO> inside = conflictDAO.findAmong(ids);
		if (!inside.isEmpty()) {
			ConflictDTO c = inside.get(0);
			throw new ServiceException("구성품끼리 금지 조합이 있어 저장할 수 없습니다: "
					+ c.getProductAName() + " × " + c.getProductBName());
		}
	}

	/* 구성 저장 — 한 트랜잭션으로 지우고 다시 넣는다 + TRACK_ITEM_SET (detail 에 전 → 후) */
	private void saveTrackItems(int adminId, int trackId, List<Integer> itemIds) throws SQLException {
		String before = names(trackItemDAO.findItems(trackId));
		LinkedHashSet<Integer> ids = new LinkedHashSet<Integer>(itemIds);

		Connection conn = null;
		try {
			conn = DBUtil.beginTransaction();
			trackItemDAO.deleteByTrack(conn, trackId);
			int order = 1;
			for (Integer id : ids) {
				trackItemDAO.insert(conn, trackId, id.intValue(), order++);
			}
			DBUtil.commit(conn);
		} catch (SQLException e) {
			DBUtil.rollback(conn);
			throw e;
		} finally {
			DBUtil.release(conn);
		}

		String after = names(trackItemDAO.findItems(trackId));
		if (!before.equals(after)) {
			adminLogDAO.insert(adminId, AdminLogDAO.MODE_ADMIN, AdminLogDAO.TRACK_ITEM_SET,
					"track_item", Integer.valueOf(trackId), "구성 " + before + " -> " + after);
		}
	}

	private String names(List<ProductDTO> items) {
		if (items.isEmpty()) {
			return "(없음)";
		}
		StringBuilder sb = new StringBuilder();
		for (ProductDTO p : items) {
			if (sb.length() > 0) {
				sb.append(", ");
			}
			sb.append(p.getName());
		}
		return sb.toString();
	}

	/* ---------------- 금지 조합 (39번 /admin/conflicts) ---------------- */

	public List<ConflictDTO> findConflicts() throws SQLException {
		return conflictDAO.findAll();
	}

	public ConflictDTO findConflict(int conflictId) throws SQLException {
		return conflictDAO.findById(conflictId);
	}

	/*
	  등록.
	  ★ 두 상품 모두 낱개 (ERD § 8-10), 서로 달라야 한다. 작은 id 가 product_a (DAO 가 정렬).
	  ★ 이미 어떤 트랙 안에 같이 들어 있는 쌍은 거부한다 — 그 트랙이 금지 조합을 품게 된다 (§ 8-14).
	*/
	public void createConflict(int adminId, int x, int y, String message) throws SQLException {
		String text = checkConflictMessage(message);
		if (x <= 0 || y <= 0 || x == y) {
			throw new ServiceException("서로 다른 낱개 영양제 두 개를 고르세요.");
		}
		List<Integer> pair = new ArrayList<Integer>();
		pair.add(Integer.valueOf(x));
		pair.add(Integer.valueOf(y));
		java.util.Map<Integer, ProductDTO> products = productDAO.findAnyByIds(pair);
		ProductDTO a = products.get(Integer.valueOf(x));
		ProductDTO b = products.get(Integer.valueOf(y));
		if (a == null || b == null || a.isTrack() || b.isTrack()) {
			throw new ServiceException("금지 조합은 낱개 영양제끼리만 등록할 수 있습니다.");
		}
		if (conflictDAO.existsPair(x, y)) {
			throw new ServiceException("이미 등록된 조합입니다.");
		}
		if (trackItemDAO.countTracksContainingBoth(x, y) > 0) {
			throw new ServiceException("두 상품이 함께 들어 있는 트랙이 있습니다. 트랙 구성부터 고치세요.");
		}

		Connection conn = null;
		try {
			conn = DBUtil.beginTransaction();
			int conflictId = conflictDAO.insert(conn, x, y, text);
			adminLogDAO.insert(conn, adminId, AdminLogDAO.MODE_ADMIN, AdminLogDAO.CONFLICT_CREATE,
					"supplement_conflict", Integer.valueOf(conflictId),
					a.getName() + " × " + b.getName());
			DBUtil.commit(conn);
		} catch (SQLException e) {
			DBUtil.rollback(conn);
			throw e;
		} finally {
			DBUtil.release(conn);
		}
	}

	public void updateConflict(int adminId, int conflictId, String message) throws SQLException {
		String text = checkConflictMessage(message);
		ConflictDTO before = conflictDAO.findById(conflictId);
		if (before == null) {
			throw new ServiceException("금지 조합을 찾을 수 없습니다.");
		}
		Connection conn = null;
		try {
			conn = DBUtil.beginTransaction();
			conflictDAO.updateMessage(conn, conflictId, text);
			adminLogDAO.insert(conn, adminId, AdminLogDAO.MODE_ADMIN, AdminLogDAO.CONFLICT_UPDATE,
					"supplement_conflict", Integer.valueOf(conflictId),
					before.getProductAName() + " × " + before.getProductBName() + " 문구 수정");
			DBUtil.commit(conn);
		} catch (SQLException e) {
			DBUtil.rollback(conn);
			throw e;
		} finally {
			DBUtil.release(conn);
		}
	}

	public void deleteConflict(int adminId, int conflictId) throws SQLException {
		ConflictDTO before = conflictDAO.findById(conflictId);
		if (before == null) {
			throw new ServiceException("금지 조합을 찾을 수 없습니다.");
		}
		Connection conn = null;
		try {
			conn = DBUtil.beginTransaction();
			conflictDAO.delete(conn, conflictId);
			adminLogDAO.insert(conn, adminId, AdminLogDAO.MODE_ADMIN, AdminLogDAO.CONFLICT_DELETE,
					"supplement_conflict", Integer.valueOf(conflictId),
					"삭제: " + before.getProductAName() + " × " + before.getProductBName());
			DBUtil.commit(conn);
		} catch (SQLException e) {
			DBUtil.rollback(conn);
			throw e;
		} finally {
			DBUtil.release(conn);
		}
	}

	/* message 는 VARCHAR(200) */
	private String checkConflictMessage(String message) {
		String text = StringUtil.trim(message);
		if (StringUtil.isEmpty(text)) {
			throw new ServiceException("경고 문구를 입력하세요.");
		}
		if (text.length() > 200) {
			throw new ServiceException("경고 문구는 200자 이내로 입력하세요.");
		}
		return text;
	}

	/* ---------------- 카테고리 ---------------- */

	public List<CategoryDTO> findCategories() throws SQLException {
		return categoryDAO.findAllOrdered();
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
	  회원 탈퇴 처리 — 26번 화면의 유일한 member 변경 (ERD § 5.3).

	  역할 토글은 폐지됐다. 파라미터로 막는 것이 아니라 처리 코드를 두지 않는 방식이라,
	  이 서비스에는 역할을 바꾸는 메서드가 없다.
	*/
	public void leaveMember(int adminId, int memberId) throws SQLException {
		MemberDTO target = memberDAO.findById(memberId);
		if (target == null) {
			throw new ServiceException("회원을 찾을 수 없습니다.");
		}
		if (!"USER".equals(target.getRole())) {
			throw new ServiceException("일반 회원만 탈퇴 처리할 수 있습니다.");
		}
		if ("LEFT".equals(target.getStatus())) {
			throw new ServiceException("이미 탈퇴한 회원입니다.");
		}

		Connection conn = null;
		try {
			conn = DBUtil.beginTransaction();
			if (memberDAO.leaveByAdmin(conn, memberId) == 0) {
				throw new ServiceException("탈퇴 처리하지 못했습니다.");
			}
			adminLogDAO.insert(conn, adminId, AdminLogDAO.MODE_ADMIN, AdminLogDAO.MEMBER_LEAVE,
					"member", Integer.valueOf(memberId), target.getLoginId() + ": ACTIVE -> LEFT");
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
