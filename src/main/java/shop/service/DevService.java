package shop.service;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import shop.dao.AdminLogDAO;
import shop.dao.CategoryDAO;
import shop.dao.ConfigDAO;
import shop.dao.DeliveryDAO;
import shop.dao.DevDAO;
import shop.dao.MemberDAO;
import shop.dao.OrderDAO;
import shop.dao.PaymentDAO;
import shop.dao.ProductDAO;
import shop.dto.AdminLogDTO;
import shop.dto.CategoryDTO;
import shop.dto.DeliveryDTO;
import shop.dto.MemberDTO;
import shop.dto.OrderDTO;
import shop.dto.OrderItemDTO;
import shop.dto.PageDTO;
import shop.dto.ProductDTO;
import shop.util.DBUtil;

/*
  /dev 전용 — 시연을 위해 정상 규칙을 넘어서는 조작.

  ★ 여기서만 상태 전이 규칙을 어길 수 있다. 그 대신 흔적을 두 곳에 남긴다 (ERD § 6.4).
      delivery_history (is_forced = 'Y')  — 배송이 어떻게 흘렀나
      admin_log (mode = 'DEV')            — 누가 무엇을 건드렸나
  ★ 되돌릴 때는 이후 단계의 시각 컬럼을 비운다. 안 비우면 "배송완료 시각은 있는데
    상태는 상품준비" 같은 모순이 남는다.
*/
public class DevService {

	private final DevDAO devDAO = new DevDAO();
	private final DeliveryDAO deliveryDAO = new DeliveryDAO();
	private final OrderDAO orderDAO = new OrderDAO();
	private final PaymentDAO paymentDAO = new PaymentDAO();
	private final ProductDAO productDAO = new ProductDAO();
	private final CategoryDAO categoryDAO = new CategoryDAO();
	private final MemberDAO memberDAO = new MemberDAO();
	private final ConfigDAO configDAO = new ConfigDAO();
	private final AdminLogDAO adminLogDAO = new AdminLogDAO();

	/* ---------------- 강제 배송 상태 ---------------- */

	public void forceDeliveryStatus(int devId, int deliveryId, String toStatus,
			String courier, String trackingNo) throws SQLException {
		DeliveryDTO delivery = deliveryDAO.findById(deliveryId);
		if (delivery == null) {
			throw new ServiceException("배송 정보를 찾을 수 없습니다.");
		}
		if (DeliveryDAO.indexOf(toStatus) < 0) {
			throw new ServiceException("알 수 없는 배송 상태입니다.");
		}
		String from = delivery.getStatus();
		if (from.equals(toStatus)) {
			throw new ServiceException("이미 " + toStatus + " 상태입니다.");
		}

		/* 배송중 이상으로 밀어 올릴 때는 운송장이 있어야 CHECK 를 통과한다. */
		boolean needTracking = DeliveryDAO.indexOf(toStatus) >= DeliveryDAO.indexOf("배송중");
		if (needTracking) {
			if (courier == null || courier.trim().length() == 0) {
				courier = "DEV택배";
			}
			if (trackingNo == null || trackingNo.trim().length() == 0) {
				trackingNo = "DEV" + System.currentTimeMillis() % 100000000L;
			}
		}

		Connection conn = null;
		try {
			conn = DBUtil.beginTransaction();
			if (devDAOForceStatus(conn, deliveryId, toStatus, courier, trackingNo) == 0) {
				throw new ServiceException("상태를 변경하지 못했습니다.");
			}
			deliveryDAO.insertHistory(conn, deliveryId, from, toStatus,
					Integer.valueOf(devId), true);
			adminLogDAO.insert(conn, devId, AdminLogDAO.MODE_DEV,
					AdminLogDAO.FORCE_DELIVERY_STATUS, "delivery", Integer.valueOf(deliveryId),
					"강제: " + from + " -> " + toStatus);
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

	private int devDAOForceStatus(Connection conn, int deliveryId, String toStatus,
			String courier, String trackingNo) throws SQLException {
		return deliveryDAO.forceStatus(conn, deliveryId, toStatus, courier, trackingNo);
	}

	/* ---------------- 강제 주문 상태 ---------------- */

	/*
	  주문을 강제로 취소 · 복구한다.

	  재고 복구 여부를 선택할 수 있다 (ERD § 6.4). 시연에서 "재고는 그대로 둔 채
	  취소만 된 주문" 을 만들어 보여줘야 할 때가 있기 때문이다.
	*/
	public void forceOrderStatus(int devId, int orderId, String toStatus, boolean restoreStock)
			throws SQLException {
		OrderDTO order = orderDAO.findById(orderId);
		if (order == null) {
			throw new ServiceException("주문을 찾을 수 없습니다.");
		}
		if (!"정상".equals(toStatus) && !"취소".equals(toStatus)) {
			throw new ServiceException("알 수 없는 주문 상태입니다.");
		}
		String from = order.getOrderStatus();
		if (from.equals(toStatus)) {
			throw new ServiceException("이미 " + toStatus + " 상태입니다.");
		}

		Connection conn = null;
		try {
			conn = DBUtil.beginTransaction();

			if ("취소".equals(toStatus)) {
				orderDAO.cancel(conn, orderId);
			} else {
				orderDAO.restore(conn, orderId);
			}

			if (restoreStock) {
				List<OrderItemDTO> items = orderDAO.findItems(conn, orderId);
				for (OrderItemDTO item : items) {
					if ("취소".equals(toStatus)) {
						productDAO.increaseStock(conn, item.getProductId(), item.getQuantity());
					} else {
						productDAO.decreaseStock(conn, item.getProductId(), item.getQuantity());
					}
				}
			}

			adminLogDAO.insert(conn, devId, AdminLogDAO.MODE_DEV,
					AdminLogDAO.FORCE_ORDER_STATUS, "orders", Integer.valueOf(orderId),
					"강제: " + from + " -> " + toStatus + " / 재고반영 " + (restoreStock ? "O" : "X"));
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

	/* ---------------- 강제 결제 상태 ---------------- */

	public void forcePayStatus(int devId, int orderId, String toStatus) throws SQLException {
		if (!"완료".equals(toStatus) && !"환불".equals(toStatus)) {
			throw new ServiceException("알 수 없는 결제 상태입니다.");
		}
		Connection conn = null;
		try {
			conn = DBUtil.beginTransaction();
			int affected = "환불".equals(toStatus)
					? paymentDAO.refund(conn, orderId)
					: paymentDAO.restore(conn, orderId);
			if (affected == 0) {
				throw new ServiceException("결제 상태를 변경하지 못했습니다.");
			}
			adminLogDAO.insert(conn, devId, AdminLogDAO.MODE_DEV, AdminLogDAO.FORCE_PAY_STATUS,
					"payment", Integer.valueOf(orderId), "강제: -> " + toStatus);
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

	/* ---------------- 강제 재고 ---------------- */

	/*
	  재고를 임의 값으로 지정한다.

	  0 으로 두면 그 상품이 고객 화면에서 사라진다 — 노출 규칙을 눈으로 확인하는
	  가장 빠른 방법이고, 그래서 이 버튼이 시연에 필요하다 (ERD § 2.4).
	*/
	public void forceStock(int devId, int productId, int stock) throws SQLException {
		if (stock < 0) {
			throw new ServiceException("재고는 0 이상이어야 합니다.");
		}
		ProductDTO product = productDAO.findAnyById(productId);
		if (product == null) {
			throw new ServiceException("상품을 찾을 수 없습니다.");
		}

		Connection conn = null;
		try {
			conn = DBUtil.beginTransaction();
			productDAO.setStock(conn, productId, stock);
			adminLogDAO.insert(conn, devId, AdminLogDAO.MODE_DEV, AdminLogDAO.FORCE_STOCK_SET,
					"product", Integer.valueOf(productId),
					product.getName() + ": " + product.getStock() + " -> " + stock);
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

	/* ---------------- 점검 모드 ---------------- */

	public String toggleShopStatus(int devId) throws SQLException {
		String current = configDAO.findValue(ConfigDAO.SHOP_STATUS);
		String next = "MAINTENANCE".equals(current) ? "OPEN" : "MAINTENANCE";

		Connection conn = null;
		try {
			conn = DBUtil.beginTransaction();
			configDAO.updateValue(conn, ConfigDAO.SHOP_STATUS, next);
			adminLogDAO.insert(conn, devId, AdminLogDAO.MODE_DEV, AdminLogDAO.SHOP_STATUS_TOGGLE,
					"system_config", null, current + " -> " + next);
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
		return next;
	}

	public void updateConfig(int devId, String key, String value) throws SQLException {
		if (ConfigDAO.SHOP_STATUS.equals(key)
				&& !"OPEN".equals(value) && !"MAINTENANCE".equals(value)) {
			throw new ServiceException("SHOP_STATUS 는 OPEN 또는 MAINTENANCE 만 가능합니다.");
		}
		Connection conn = null;
		try {
			conn = DBUtil.beginTransaction();
			if (configDAO.updateValue(conn, key, value) == 0) {
				throw new ServiceException("설정 키를 찾을 수 없습니다: " + key);
			}
			adminLogDAO.insert(conn, devId, AdminLogDAO.MODE_DEV, AdminLogDAO.SHOP_STATUS_TOGGLE,
					"system_config", null, key + " = " + value);
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

	/* ---------------- 데이터 관리 ---------------- */

	public void truncateAll(int devId) throws SQLException {
		Connection conn = null;
		try {
			conn = DBUtil.beginTransaction();
			devDAO.truncateAll(conn);
			adminLogDAO.insert(conn, devId, AdminLogDAO.MODE_DEV, AdminLogDAO.SEED_TRUNCATE_ALL,
					null, null, "전체 초기화 (member 는 role='USER' 만 삭제)");
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

	public void truncateTable(int devId, String table) throws SQLException {
		if (!DevDAO.isAllowed(table)) {
			throw new ServiceException("허용되지 않은 테이블입니다: " + table);
		}
		Connection conn = null;
		try {
			conn = DBUtil.beginTransaction();
			devDAO.truncateTable(conn, table);
			adminLogDAO.insert(conn, devId, AdminLogDAO.MODE_DEV,
					AdminLogDAO.SEED_TRUNCATE_TABLE, table, null, table + " 비움");
			DBUtil.commit(conn);
		} catch (IllegalArgumentException e) {
			DBUtil.rollback(conn);
			throw new ServiceException(e.getMessage());
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

	/*
	  더미 상품 생성.

	  data.sql 을 다시 돌리지 않고도 목록 · 페이징을 채워 볼 수 있게 한다.
	  카테고리가 하나도 없으면 먼저 하나 만든다 — product.category_id 가 NOT NULL 이다.
	*/
	public int generateProducts(int devId, int count) throws SQLException {
		if (count < 1 || count > 100) {
			throw new ServiceException("생성 개수는 1~100 사이로 입력하세요.");
		}
		List<CategoryDTO> categories = categoryDAO.findAll();
		int categoryId;
		if (categories.isEmpty()) {
			CategoryDTO seed = new CategoryDTO();
			seed.setName("DEV 생성");
			seed.setSortOrder(99);
			categoryDAO.insert(seed);
			categories = categoryDAO.findAll();
		}
		categoryId = categories.get(0).getCategoryId();

		long stamp = System.currentTimeMillis() % 100000L;
		for (int i = 1; i <= count; i++) {
			ProductDTO product = new ProductDTO();
			product.setCategoryId(categoryId);
			product.setName("DEV 더미상품 " + stamp + "-" + i);
			product.setPrice(1000 * i);
			product.setStock(10);
			product.setDescription("/dev 에서 생성한 더미 상품");
			product.setStatus("판매중");
			productDAO.insert(product);
		}
		adminLogDAO.insert(devId, AdminLogDAO.MODE_DEV, AdminLogDAO.SEED_GENERATE,
				"product", null, "더미 상품 " + count + "건 생성");
		return count;
	}

	/* ---------------- 계정 전환 ---------------- */

	/*
	  login-as.

	  ★ 감사 구멍이 하나 열린다 (ERD § 8 쟁점 7). 전환한 뒤의 조작은
	    admin_log.member_id 에 "전환된 회원" 으로 찍혀 실제 행위자가 사라진다.
	    그래서 전환 시점에 detail 에 impersonated_by 를 남긴다 —
	    /dev/log 가 이 값을 보여주면 추적이 복원된다 (CRUD.md § 6-2).
	*/
	public MemberDTO loginAs(int devId, int targetMemberId) throws SQLException {
		MemberDTO target = memberDAO.findById(targetMemberId);
		if (target == null) {
			throw new ServiceException("회원을 찾을 수 없습니다.");
		}
		adminLogDAO.insert(devId, AdminLogDAO.MODE_DEV, AdminLogDAO.LOGIN_AS,
				"member", Integer.valueOf(targetMemberId),
				"impersonated_by=" + devId + " target=" + target.getLoginId()
				+ " role=" + target.getRole());
		return target;
	}

	/* ---------------- 조회 ---------------- */

	public Map<String, Object> browse(String table, String sortColumn, boolean desc,
			int page, int size) throws SQLException {
		if (!DevDAO.isAllowed(table)) {
			throw new ServiceException("허용되지 않은 테이블입니다: " + table);
		}
		return devDAO.browse(table, sortColumn, desc, page, size);
	}

	public List<Map<String, String>> findSchemaColumns() throws SQLException {
		return devDAO.findSchemaColumns();
	}

	public List<Map<String, String>> findSchemaConstraints() throws SQLException {
		return devDAO.findSchemaConstraints();
	}

	public List<Map<String, String>> findSchemaChecks() throws SQLException {
		return devDAO.findSchemaChecks();
	}

	public PageDTO<AdminLogDTO> findLogs(String mode, String action, String targetTable,
			String fromDate, String toDate, int page, int size) throws SQLException {
		return adminLogDAO.findPage(mode, action, targetTable, fromDate, toDate, page, size);
	}

	public List<MemberDTO> findAllMembers() throws SQLException {
		return memberDAO.findPage(null, null, 1, 100).getList();
	}
}
