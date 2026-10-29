package shop.service;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import shop.dao.AddressDAO;
import shop.dao.DeliveryDAO;
import shop.dao.OrderDAO;
import shop.dao.QnaDAO;
import shop.dao.ReviewDAO;
import shop.dao.WishlistDAO;
import shop.dto.AddressDTO;
import shop.dto.DeliveryDTO;
import shop.dto.OrderDTO;
import shop.dto.OrderItemDTO;
import shop.dto.QnaDTO;
import shop.dto.ReviewDTO;
import shop.dto.WishItemDTO;
import shop.util.DBUtil;
import shop.util.StringUtil;

/*
  마이페이지 — 배송지 · 찜 · 리뷰 · 문의.

  이 서비스의 모든 수정 · 삭제에는 member_id 조건이 붙는다. 본인 글만 건드릴 수 있다
  (CRUD.md § 2 — 17 · 18번 주의).
*/
public class MypageService {

	private final AddressDAO addressDAO = new AddressDAO();
	private final WishlistDAO wishlistDAO = new WishlistDAO();
	private final ReviewDAO reviewDAO = new ReviewDAO();
	private final QnaDAO qnaDAO = new QnaDAO();
	private final OrderDAO orderDAO = new OrderDAO();
	private final DeliveryDAO deliveryDAO = new DeliveryDAO();

	/* ---------------- 배송지 ---------------- */

	public List<AddressDTO> findAddresses(int memberId) throws SQLException {
		return addressDAO.findByMember(memberId);
	}

	public AddressDTO findDefaultAddress(int memberId) throws SQLException {
		return addressDAO.findDefault(memberId);
	}

	public AddressDTO findAddress(int addrId, int memberId) throws SQLException {
		return addressDAO.findById(addrId, memberId);
	}

	/*
	  배송지 저장.

	  기본 배송지는 회원당 하나다. 'Y' 로 저장할 때는 기존 것을 전부 내린 뒤
	  하나만 올린다 — 두 문장이므로 트랜잭션으로 묶는다.
	  첫 배송지는 선택과 무관하게 기본으로 만든다. 그래야 주문서 자동입력이 항상 동작한다.
	*/
	public void saveAddress(AddressDTO addr, boolean makeDefault) throws SQLException {
		validateAddress(addr);

		boolean first = addressDAO.countByMember(addr.getMemberId()) == 0;
		boolean asDefault = makeDefault || first;
		addr.setIsDefault(asDefault ? "Y" : "N");

		Connection conn = null;
		try {
			conn = DBUtil.beginTransaction();
			if (asDefault) {
				addressDAO.clearDefault(conn, addr.getMemberId());
			}
			if (addr.getAddrId() > 0) {
				if (addressDAO.update(conn, addr) == 0) {
					throw new ServiceException("배송지를 찾을 수 없습니다.");
				}
			} else {
				addressDAO.insert(conn, addr);
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

	public void setDefaultAddress(int addrId, int memberId) throws SQLException {
		Connection conn = null;
		try {
			conn = DBUtil.beginTransaction();
			addressDAO.clearDefault(conn, memberId);
			if (addressDAO.markDefault(conn, addrId, memberId) == 0) {
				throw new ServiceException("배송지를 찾을 수 없습니다.");
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

	public void deleteAddress(int addrId, int memberId) throws SQLException {
		addressDAO.delete(addrId, memberId);
	}

	private void validateAddress(AddressDTO addr) {
		if (StringUtil.isEmpty(addr.getReceiver())) {
			throw new ServiceException("받는 사람을 입력하세요.");
		}
		if (StringUtil.isEmpty(addr.getReceiverPhone())) {
			throw new ServiceException("연락처를 입력하세요.");
		}
		if (StringUtil.isEmpty(addr.getZipcode())) {
			throw new ServiceException("우편번호를 입력하세요.");
		}
		if (StringUtil.isEmpty(addr.getAddr1())) {
			throw new ServiceException("주소를 입력하세요.");
		}
	}

	/* ---------------- 찜 ---------------- */

	public List<WishItemDTO> findWishlist(int memberId) throws SQLException {
		return wishlistDAO.findByMember(memberId);
	}

	public boolean isWished(int memberId, int productId) throws SQLException {
		return memberId > 0 && wishlistDAO.exists(memberId, productId);
	}

	public int countWishlist(int memberId) throws SQLException {
		return wishlistDAO.count(memberId);
	}

	/* 토글 — 수정이라는 개념이 없다. 결과(담김 여부)를 돌려준다. */
	public boolean toggleWish(int memberId, int productId) throws SQLException {
		if (wishlistDAO.exists(memberId, productId)) {
			wishlistDAO.delete(memberId, productId);
			return false;
		}
		wishlistDAO.insert(memberId, productId);
		return true;
	}

	public void removeWish(int memberId, int productId) throws SQLException {
		wishlistDAO.delete(memberId, productId);
	}

	/* ---------------- 리뷰 ---------------- */

	public List<ReviewDTO> findMyReviews(int memberId) throws SQLException {
		return reviewDAO.findByMember(memberId);
	}

	public ReviewDTO findReviewByItem(int itemId) throws SQLException {
		return reviewDAO.findByItem(itemId);
	}

	/*
	  리뷰 작성.

	  작성 조건은 세 가지다 (CRUD.md § 6-3).
	      1. 본인이 주문한 건일 것
	      2. 배송완료일 것
	      3. 그 주문 건(item_id)에 아직 리뷰가 없을 것 — UNIQUE 가 DB 에서도 막는다
	  작성은 강제가 아니다. 안 써도 된다.
	*/
	public void writeReview(int memberId, int itemId, int rating, String content)
			throws SQLException {
		validateReview(rating, content);

		OrderItemDTO target = null;
		OrderDTO order = null;
		/* item_id 로 주문을 거슬러 올라가 소유자와 배송 상태를 확인한다. */
		for (OrderDTO candidate : findOrdersHavingItem(memberId, itemId)) {
			for (OrderItemDTO item : candidate.getItems()) {
				if (item.getItemId() == itemId) {
					target = item;
					order = candidate;
				}
			}
		}
		if (target == null || order == null) {
			throw new ServiceException("본인이 주문한 상품만 리뷰를 쓸 수 있습니다.");
		}

		DeliveryDTO delivery = deliveryDAO.findByOrder(order.getOrderId());
		if (delivery == null || !"배송완료".equals(delivery.getStatus())) {
			throw new ServiceException("배송완료된 주문에만 리뷰를 쓸 수 있습니다.");
		}
		if (reviewDAO.findByItem(itemId) != null) {
			throw new ServiceException("이미 리뷰를 작성한 주문 건입니다.");
		}

		reviewDAO.insert(memberId, target.getProductId(), itemId, rating, content);
	}

	/* 리뷰 대상 후보를 찾기 위해 본인 주문을 훑는다. 샘플 규모에서는 충분하다. */
	private List<OrderDTO> findOrdersHavingItem(int memberId, int itemId) throws SQLException {
		List<OrderDTO> orders = orderDAO.findPageByMember(memberId, 1, 100).getList();
		for (OrderDTO order : orders) {
			order.setItems(orderDAO.findItems(order.getOrderId()));
		}
		return orders;
	}

	public void updateReview(int reviewId, int memberId, int rating, String content)
			throws SQLException {
		validateReview(rating, content);
		if (reviewDAO.update(reviewId, memberId, rating, content) == 0) {
			throw new ServiceException("본인이 쓴 리뷰만 수정할 수 있습니다.");
		}
	}

	public void deleteReview(int reviewId, int memberId) throws SQLException {
		if (reviewDAO.deleteByOwner(reviewId, memberId) == 0) {
			throw new ServiceException("본인이 쓴 리뷰만 삭제할 수 있습니다.");
		}
	}

	private void validateReview(int rating, String content) {
		if (rating < 1 || rating > 5) {
			throw new ServiceException("별점은 1~5점으로 선택하세요.");
		}
		if (StringUtil.isEmpty(content)) {
			throw new ServiceException("리뷰 내용을 입력하세요.");
		}
		if (content.length() > 1000) {
			throw new ServiceException("리뷰는 1000자 이내로 입력하세요.");
		}
	}

	/* ---------------- 문의 ---------------- */

	public List<QnaDTO> findMyQna(int memberId) throws SQLException {
		return qnaDAO.findByMember(memberId);
	}

	public void writeQna(int memberId, int productId, String question, boolean secret)
			throws SQLException {
		validateQuestion(question);
		qnaDAO.insert(memberId, productId, question, secret);
	}

	/* 답변이 달린 문의는 고칠 수 없다 — 답변이 다른 질문에 붙게 된다. */
	public void updateQna(int qnaId, int memberId, String question, boolean secret)
			throws SQLException {
		validateQuestion(question);
		QnaDTO qna = qnaDAO.findById(qnaId);
		if (qna == null || qna.getMemberId() != memberId) {
			throw new ServiceException("본인이 쓴 문의만 수정할 수 있습니다.");
		}
		if (qna.isAnswered()) {
			throw new ServiceException("답변이 등록된 문의는 수정할 수 없습니다.");
		}
		qnaDAO.updateQuestion(qnaId, memberId, question, secret);
	}

	public void deleteQna(int qnaId, int memberId) throws SQLException {
		if (qnaDAO.delete(qnaId, memberId) == 0) {
			throw new ServiceException("본인이 쓴 문의만 삭제할 수 있습니다.");
		}
	}

	private void validateQuestion(String question) {
		if (StringUtil.isEmpty(question)) {
			throw new ServiceException("문의 내용을 입력하세요.");
		}
		if (question.length() > 1000) {
			throw new ServiceException("문의는 1000자 이내로 입력하세요.");
		}
	}
}
