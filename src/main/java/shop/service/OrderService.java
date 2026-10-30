package shop.service;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import shop.dao.AdminLogDAO;
import shop.dao.CartDAO;
import shop.dao.DeliveryDAO;
import shop.dao.OrderDAO;
import shop.dao.PaymentDAO;
import shop.dao.ProductDAO;
import shop.dto.CartItemDTO;
import shop.dto.DeliveryDTO;
import shop.dto.OrderDTO;
import shop.dto.OrderItemDTO;
import shop.dto.PageDTO;
import shop.dto.PaymentDTO;
import shop.util.DBUtil;
import shop.util.StringUtil;

/*
  주문 — 결제와 취소.

  이 프로젝트에서 트랜잭션이 가장 깊은 곳이다. 결제 한 번이 7개 테이블을 건드리고
  전부가 한 단위여야 한다 (CRUD.md § 2 — 8번 결제처리).

  커넥션 다루기는 세 곳 모두 같은 모양이다.
      beginTransaction() → 작업 → commit() → catch 에서 rollback() → finally 에서 release()
  release() 가 autoCommit 을 되돌린다. 이걸 빠뜨리면 풀에 돌아간 커넥션이
  다음 요청에서 커밋을 하지 않는다 (ERD § 6).
*/
public class OrderService {

	private final OrderDAO orderDAO = new OrderDAO();
	private final PaymentDAO paymentDAO = new PaymentDAO();
	private final DeliveryDAO deliveryDAO = new DeliveryDAO();
	private final ProductDAO productDAO = new ProductDAO();
	private final CartDAO cartDAO = new CartDAO();
	private final AdminLogDAO adminLogDAO = new AdminLogDAO();
	private final ConfigService configService = new ConfigService();

	/* ---------------- 결제 (ERD § 6.1) ---------------- */

	/*
	  결제 버튼 한 번 = 한 트랜잭션.

	      1. orders INSERT → order_id → order_no UPDATE
	      2. order_item INSERT (장바구니 줄마다)
	      3. payment INSERT
	      4. delivery INSERT (결제완료)
	      5. delivery_history INSERT (NULL → 결제완료, is_forced = 'N')
	      6. 재고 차감 — 영향 행 0 이면 재고 부족이므로 롤백
	      7. 장바구니 비우기

	  ★ 금액을 화면에서 받지 않는다. 장바구니를 트랜잭션 안에서 다시 읽어
	    서버가 계산한다. 폼에서 넘어온 금액을 믿으면 가격을 조작할 수 있다.
	  ★ 배송비도 이 시점에 계산해 복사 저장한다 (ERD § 2.8).
	*/
	public int pay(int memberId, OrderDTO form, String method) throws SQLException {
		validateOrderForm(form, method);

		Connection conn = null;
		try {
			conn = DBUtil.beginTransaction();

			/* 장바구니를 트랜잭션 안에서 다시 읽는다 — 화면을 띄운 뒤 바뀌었을 수 있다. */
			List<CartItemDTO> cart = cartDAO.findByMember(conn, memberId);
			if (cart.isEmpty()) {
				throw new ServiceException("장바구니가 비어 있습니다.");
			}

			int totalPrice = 0;
			int orderable = 0;
			for (CartItemDTO item : cart) {
				if (item.isOrderable()) {
					totalPrice += item.getLineTotal();
					orderable++;
				}
			}
			if (orderable == 0) {
				throw new ServiceException("주문할 수 있는 상품이 없습니다.");
			}

			int deliveryFee = configService.calculateDeliveryFee(totalPrice);

			/* 1. 주문 */
			form.setMemberId(memberId);
			form.setTotalPrice(totalPrice);
			form.setDeliveryFee(deliveryFee);
			int orderId = orderDAO.insertOrder(conn, form);
			if (orderId == 0) {
				throw new ServiceException("주문을 생성하지 못했습니다.");
			}

			/* 2 · 6. 주문 상품 스냅샷 + 재고 차감 */
			for (CartItemDTO item : cart) {
				if (!item.isOrderable()) {
					continue;
				}
				orderDAO.insertItem(conn, orderId, item.getProductId(), item.getProductName(),
						item.getPrice(), item.getQuantity());

				int affected = productDAO.decreaseStock(conn, item.getProductId(), item.getQuantity());
				if (affected == 0) {
					/* WHERE stock >= ? 에 걸렸다 — 결제 화면을 띄운 사이 재고가 빠졌다. */
					throw new ServiceException("'" + item.getProductName()
							+ "' 의 재고가 부족합니다. 장바구니를 다시 확인하세요.");
				}
			}

			/* 3. 결제 */
			paymentDAO.insert(conn, orderId, method, totalPrice + deliveryFee);

			/* 4 · 5. 배송 + 첫 이력 */
			int deliveryId = deliveryDAO.insert(conn, orderId);
			deliveryDAO.insertHistory(conn, deliveryId, null, "결제완료", null, false);

			/* 7. 장바구니 비우기 */
			cartDAO.clear(conn, memberId);

			DBUtil.commit(conn);
			return orderId;
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

	private void validateOrderForm(OrderDTO form, String method) {
		if (StringUtil.isEmpty(form.getReceiver())) {
			throw new ServiceException("받는 사람을 입력하세요.");
		}
		if (StringUtil.isEmpty(form.getReceiverPhone())) {
			throw new ServiceException("연락처를 입력하세요.");
		}
		if (StringUtil.isEmpty(form.getZipcode()) || StringUtil.isEmpty(form.getAddr1())) {
			throw new ServiceException("배송 주소를 입력하세요.");
		}
		if (!"카드".equals(method) && !"무통장".equals(method) && !"간편결제".equals(method)) {
			throw new ServiceException("결제 수단을 선택하세요.");
		}
	}

	/* ---------------- 취소 (ERD § 6.3) ---------------- */

	/*
	  주문 취소.

	      1. orders.order_status = '취소', canceled_at = NOW()
	      2. payment.pay_status = '환불', refunded_at = NOW()
	      3. 재고 복구 (주문 상품마다)
	      4. admin_log INSERT

	  ★ delivery 는 건드리지 않는다. 그래서 취소된 주문의 배송 상태가
	    '상품준비' 로 남는다 — ERD § 8 쟁점 1 로 보고된 의도적 공백이다.
	  ★ 배송이 시작된 뒤에는 취소할 수 없다. 판정은 delivery.status 로 한다.
	*/
	public void cancel(int orderId, int actorId, boolean admin) throws SQLException {
		OrderDTO order = orderDAO.findById(orderId);
		if (order == null) {
			throw new ServiceException("주문을 찾을 수 없습니다.");
		}
		if (!admin && order.getMemberId() != actorId) {
			throw new ServiceException("본인의 주문만 취소할 수 있습니다.");
		}
		if (!"정상".equals(order.getOrderStatus())) {
			throw new ServiceException("이미 취소된 주문입니다.");
		}

		DeliveryDTO delivery = deliveryDAO.findByOrder(orderId);
		if (delivery == null) {
			throw new ServiceException("배송 정보가 없어 취소할 수 없습니다.");
		}
		if (!"결제완료".equals(delivery.getStatus()) && !"상품준비".equals(delivery.getStatus())) {
			throw new ServiceException("이미 발송된 주문은 취소할 수 없습니다. (현재 "
					+ delivery.getStatus() + ")");
		}

		Connection conn = null;
		try {
			conn = DBUtil.beginTransaction();

			/* 1. 가드가 중복 취소를 막는다 — 영향 행 0 이면 누군가 먼저 취소했다. */
			if (orderDAO.cancel(conn, orderId) == 0) {
				throw new ServiceException("이미 취소된 주문입니다.");
			}

			/* 2. 환불 */
			paymentDAO.refund(conn, orderId);

			/* 3. 재고 복구 */
			List<OrderItemDTO> items = orderDAO.findItems(conn, orderId);
			for (OrderItemDTO item : items) {
				productDAO.increaseStock(conn, item.getProductId(), item.getQuantity());
			}

			/*
			  4. 기록 — 관리자 취소만 남긴다 (ERD § 6.3 개정).
			     admin_log.mode 에 USER 가 없다. 고객 본인 취소는 orders.canceled_at 이 기록이다.
			*/
			if (admin) {
				adminLogDAO.insert(conn, actorId, AdminLogDAO.MODE_ADMIN,
						AdminLogDAO.ORDER_CANCEL, "orders", Integer.valueOf(orderId),
						"주문 " + order.getOrderNo() + " 취소 · 재고 " + items.size() + "건 복구");
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

	/* ---------------- 조회 ---------------- */

	/* 주문 상세 — items · payment · delivery · history 를 모두 채운다. */
	public OrderDTO findDetail(int orderId, int memberId, boolean admin) throws SQLException {
		OrderDTO order = admin
				? orderDAO.findById(orderId)
				: orderDAO.findByIdForMember(orderId, memberId);
		if (order == null) {
			return null;
		}
		order.setItems(orderDAO.findItems(orderId));

		PaymentDTO payment = paymentDAO.findByOrder(orderId);
		order.setPayment(payment);

		DeliveryDTO delivery = deliveryDAO.findByOrder(orderId);
		if (delivery != null) {
			delivery.setHistory(deliveryDAO.findHistory(delivery.getDeliveryId()));
			order.setDelivery(delivery);
		}
		return order;
	}

	/*
	  주문 목록 — 줄마다 배송 상태가 필요하다.

	  주문 건수가 한 페이지(10건)뿐이라 건당 한 번씩 조회해도 왕복이 11회다.
	  관리자 목록처럼 뷰로 한 번에 받는 방법도 있지만, 고객 목록은 뷰가 없고
	  페이지 크기가 작아 그대로 둔다 — 늘어나면 전용 뷰를 요청한다 (트랙 문서 § 7).
	*/
	public PageDTO<OrderDTO> findMyOrders(int memberId, int page, int size) throws SQLException {
		PageDTO<OrderDTO> result = orderDAO.findPageByMember(memberId, page, size);
		for (OrderDTO order : result.getList()) {
			order.setItems(orderDAO.findItems(order.getOrderId()));
			order.setDelivery(deliveryDAO.findByOrder(order.getOrderId()));
		}
		return result;
	}
}
