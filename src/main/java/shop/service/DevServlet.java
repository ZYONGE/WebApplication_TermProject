package shop.service;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import shop.dao.AdminDAO;
import shop.dao.CategoryDAO;
import shop.dao.ConfigDAO;
import shop.dao.DB;
import shop.dao.MemberDAO;
import shop.dao.OrderDAO;
import shop.dao.ProductDAO;
import shop.model.Category;
import shop.model.Member;
import shop.model.Order;
import shop.model.Product;

/*
  개발자(DEV) 도구 — 시연 · 테스트용.
    /dev            설정 · 메뉴         /dev/state    주문 · 배송 · 결제 · 재고 · 설정 강제 변경
    /dev/data       테이블 들여다보기    /dev/seed     더미 데이터 · 테이블 비우기
    /dev/log        감사 로그           /dev/loginAs  다른 계정으로 전환 (/return 으로 복귀)
    /dev/erd        ERD 그림 · 스키마
*/
@WebServlet({ "/dev", "/dev/", "/dev/data", "/dev/erd", "/dev/erd/diagram.svg", "/dev/log", "/dev/loginAs",
		"/dev/loginAs/return", "/dev/seed", "/dev/state" })

public class DevServlet extends BaseServlet {

	private static final long serialVersionUID = 1L;

	@Override
	protected void get(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException, SQLException {
		request.setAttribute("tables", AdminDAO.TABLES);
		switch (request.getServletPath()) {
		case "/dev/data":
			String table = param(request, "table");
			if (!isEmpty(table) && !AdminDAO.TABLES.contains(table)) {
				request.setAttribute("errorMessage", "허용되지 않은 테이블입니다: " + table);
			} else if (!isEmpty(table)) {
				int size = intParam(request, "size", 20);
				size = size < 1 ? 10 : Math.min(size, 100);
				request.setAttribute("result", AdminDAO.browse(table, param(request, "sort"),
						"desc".equals(param(request, "dir")), intParam(request, "page", 1), size));
			}
			render(request, response, "dev/data", "dev · 데이터 브라우저");
			break;
		case "/dev/erd":
			String[] source = erdSource();
			request.setAttribute("erdOrigin", source[1]);
			if (source[0] != null) {
				Erd erd = Erd.parse(source[0]);
				request.setAttribute("erdEntityCount", erd.entityCount());
				request.setAttribute("erdRelationCount", erd.relationCount());
				request.setAttribute("erdSvg", erd.toSvg(true));
			}
			request.setAttribute("columns", AdminDAO.schema("SELECT table_name, column_name, ordinal_position,"
					+ " column_type, is_nullable, column_key, column_default, extra FROM information_schema.columns"
					+ " WHERE table_schema = DATABASE() ORDER BY table_name, ordinal_position"));
			request.setAttribute("constraints", AdminDAO.schema("SELECT tc.table_name, tc.constraint_name,"
					+ " tc.constraint_type, kcu.column_name, kcu.referenced_table_name, kcu.referenced_column_name,"
					+ " rc.delete_rule, rc.update_rule FROM information_schema.table_constraints tc"
					+ " LEFT JOIN information_schema.key_column_usage kcu ON kcu.constraint_name = tc.constraint_name"
					+ " AND kcu.table_schema = tc.table_schema AND kcu.table_name = tc.table_name"
					+ " LEFT JOIN information_schema.referential_constraints rc ON rc.constraint_name = tc.constraint_name"
					+ " AND rc.constraint_schema = tc.table_schema WHERE tc.table_schema = DATABASE()"
					+ " ORDER BY tc.table_name, tc.constraint_type, kcu.ordinal_position"));
			request.setAttribute("checks", AdminDAO.schema("SELECT cc.constraint_name, cc.check_clause"
					+ " FROM information_schema.check_constraints cc WHERE cc.constraint_schema = DATABASE()"
					+ " ORDER BY cc.constraint_name"));
			render(request, response, "dev/erd", "dev · 설계 보기");
			break;
		case "/dev/erd/diagram.svg":
			String[] src = erdSource();
			if (src[0] == null) {
				response.sendError(404, "ERD 원본(mermaid)을 찾지 못했습니다.");
				return;
			}
			byte[] svg = Erd.parse(src[0]).toSvg(false).getBytes(StandardCharsets.UTF_8);
			response.setContentType("image/svg+xml; charset=UTF-8");
			response.setHeader("Cache-Control", "no-cache");
			response.setContentLength(svg.length);
			response.getOutputStream().write(svg);
			break;
		case "/dev/log":
			String[] filters = { "mode", "action", "targetTable", "fromDate", "toDate" };
			StringBuilder url = new StringBuilder("/dev/log?");
			for (String f : filters) {
				if (!isEmpty(param(request, f))) {
					url.append(f).append("=").append(URLEncoder.encode(param(request, f), "UTF-8")).append("&");
				}
			}
			request.setAttribute("actions", AdminDAO.ACTIONS);
			request.setAttribute("pageUrl", url.toString());
			request.setAttribute("pageData", AdminDAO.logs(param(request, "mode"), param(request, "action"),
					param(request, "targetTable"), param(request, "fromDate"), param(request, "toDate"),
					intParam(request, "page", 1)));
			render(request, response, "dev/log", "dev · 감사 로그");
			break;
		case "/dev/loginAs":
			request.setAttribute("members", MemberDAO.page(null, null, 1, 100).getList());
			render(request, response, "dev/loginAs", "dev · 계정 전환");
			break;
		case "/dev/loginAs/return":
			returnToDev(request, response);
			break;
		case "/dev/seed":
			List<String> truncatable = new ArrayList<String>();
			for (String t : AdminDAO.TABLES) {
				if (!t.equals("member") && !t.startsWith("v_")) {
					truncatable.add(t);
				}
			}
			request.setAttribute("truncatable", truncatable);
			request.setAttribute("truncateCount", AdminDAO.TRUNCATE_ORDER.length);
			render(request, response, "dev/seed", "dev · 데이터 관리");
			break;
		case "/dev/state":
			request.setAttribute("pageData", OrderDAO.adminPage(param(request, "keyword"), null, null, intParam(request, "page", 1)));
			request.setAttribute("pageUrl", "/dev/state?");
			request.setAttribute("flow", OrderDAO.FLOW);
			request.setAttribute("configs", ConfigDAO.all());
			request.setAttribute("products", ProductDAO.adminPage(0, null, null, null, 1, 100).getList());
			if (intParam(request, "orderId", 0) > 0) {
				request.setAttribute("detail", OrderDAO.detail(intParam(request, "orderId", 0), 0));
			}
			render(request, response, "dev/state", "dev · 상태 컨트롤러");
			break;
		default:
			int views = 0;
			for (String t : AdminDAO.TABLES) {
				views += t.startsWith("v_") ? 1 : 0;
			}
			request.setAttribute("configs", ConfigDAO.all());
			request.setAttribute("tableCount", AdminDAO.TABLES.size() - views);
			request.setAttribute("viewCount", views);
			render(request, response, "dev/index", "dev");
		}
	}

	/*
	  ERD 원본 { mermaid 글, 어디서 읽었는지 } — 프로젝트의 README.md 를 먼저 보고,
	  배포 환경이라 README 를 못 읽으면 같은 내용의 사본(shop/service/erd.mmd)을 쓴다.
	*/
	private String[] erdSource() throws IOException {
		String webRoot = getServletContext().getRealPath("/");
		File readme = webRoot == null ? null : new File(new File(webRoot).getParentFile(), "README.md");
		if (readme != null && readme.isFile()) {
			String block = Erd.extractBlock(new String(Files.readAllBytes(readme.toPath()), StandardCharsets.UTF_8));
			if (block != null) {
				return new String[] { block, "README.md" };
			}
		}
		try (InputStream in = getClass().getResourceAsStream("erd.mmd")) {
			if (in == null) {
				return new String[] { null, "없음" };
			}
			return new String[] { new String(in.readAllBytes(), StandardCharsets.UTF_8),
				"사본 shop/service/erd.mmd (README 를 읽을 수 없는 배포 환경)" };
		}
	}

	@Override
	protected void post(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException, SQLException {
		int devId = loginId(request);
		String mode = param(request, "mode", "");
		String path = request.getServletPath();
		if (path.equals("/dev/loginAs")) {
			loginAs(request, response, devId);
			return;
		}
		if (!path.equals("/dev/seed") && !path.equals("/dev/state")) {
			super.post(request, response);
			return;
		}
		try {
			if (path.equals("/dev/seed")) {
				seed(request, devId, mode);
			} else {
				state(request, devId, mode);
			}
		} catch (Fail e) {
			flashError(request, e.getMessage());
		}
		redirect(request, response, path.equals("/dev/seed") ? "/dev/seed" : "/dev/state");
	}

	/* ---------- 계정 전환 ---------- */

	private void loginAs(HttpServletRequest request, HttpServletResponse response, int devId)
			throws IOException, SQLException {
		Member target = MemberDAO.byId(intParam(request, "memberId", 0));
		try {
			if (target == null) {
				throw new Fail("회원을 찾을 수 없습니다.");
			}
			if ("LEFT".equals(target.getStatus())) {
				throw new Fail("탈퇴한 회원으로는 전환할 수 없습니다.");
			}
			if ("DEV".equals(target.getRole())) {
				throw new Fail("개발자 계정으로는 전환할 수 없습니다.");
			}
		} catch (Fail e) {
			flashError(request, e.getMessage());
			redirect(request, response, "/dev/loginAs");
			return;
		}
		AdminDAO.devLog(devId, "LOGIN_AS", "member", target.getMemberId(),
				"impersonated_by=" + devId + " target=" + target.getLoginId() + " role=" + target.getRole());
		request.getSession().setAttribute("impersonatorId", devId);
		request.getSession().setAttribute("loginMember", target);
		flash(request, target.getLoginId() + " 계정으로 전환했습니다. 복귀는 상단 '원래 계정으로'.");
		redirect(request, response, homeOf(target.getRole()));
	}

	private void returnToDev(HttpServletRequest request, HttpServletResponse response) throws IOException, SQLException {
		Integer devId = (Integer) request.getSession().getAttribute("impersonatorId");
		Member dev = devId == null ? null : MemberDAO.byId(devId);
		if (dev == null || !"DEV".equals(dev.getRole())) {
			flashError(request, "원래 개발자 계정을 찾을 수 없습니다.");
			redirect(request, response, "/dev/");
			return;
		}
		AdminDAO.devLog(devId, "LOGIN_AS_RETURN", "member", loginId(request),
				"복귀: #" + loginId(request) + " -> " + dev.getLoginId());
		request.getSession().removeAttribute("impersonatorId");
		request.getSession().setAttribute("loginMember", dev);
		flash(request, "원래 계정으로 돌아왔습니다.");
		redirect(request, response, "/dev/");
	}

	/* ---------- 데이터 관리 ---------- */

	private void seed(HttpServletRequest request, int devId, String mode) throws SQLException {
		if (mode.equals("truncateAll")) {
			AdminDAO.truncate(true, AdminDAO.TRUNCATE_ORDER);
			AdminDAO.devLog(devId, "SEED_TRUNCATE_ALL", null, null, "전체 초기화 (member 는 role='USER' 만 삭제)");
			flash(request, "전체 초기화했습니다. (ADMIN · DEV 계정은 남았습니다)");
		} else if (mode.equals("truncateTable")) {
			String table = param(request, "table");
			if (!AdminDAO.TABLES.contains(table)) {
				throw new Fail("허용되지 않은 테이블입니다: " + table);
			}
			if (table.equals("member") || table.startsWith("v_")) {
				throw new Fail("비울 수 없는 대상: " + table);
			}
			AdminDAO.truncate(false, table);
			AdminDAO.devLog(devId, "SEED_TRUNCATE_TABLE", table, null, table + " 비움");
			flash(request, table + " 테이블을 비웠습니다.");
		} else if (mode.equals("generate")) {
			int count = intParam(request, "count", 10);
			if (count < 1 || count > 100) {
				throw new Fail("생성 개수는 1~100 사이로 입력하세요.");
			}
			/* 더미 상품은 첫 소분류에 (카테고리가 하나도 없으면 하나 만든다) */
			List<Category> categories = CategoryDAO.all();
			if (categories.isEmpty()) {
				CategoryDAO.save(0, 0, "DEV 생성", 99);
				categories = CategoryDAO.all();
			}
			int categoryId = categories.get(0).getCategoryId();
			for (Category c : categories) {
				if (c.getParentId() != null) {
					categoryId = c.getCategoryId();
					break;
				}
			}
			long stamp = System.currentTimeMillis() % 100000L;
			for (int i = 1; i <= count; i++) {
				ProductDAO.save(0, categoryId, "SINGLE", "DEV 더미 영양제 " + stamp + "-" + i, 100 * i, 100,
						"/dev 에서 생성한 더미 상품", null, "판매중");
			}
			AdminDAO.devLog(devId, "SEED_GENERATE", "product", null, "더미 상품 " + count + "건 생성");
			flash(request, "더미 상품 " + count + "건을 생성했습니다.");
		}
	}

	/* ---------- 상태 강제 변경 ---------- */

	private void state(HttpServletRequest request, int devId, String mode) throws SQLException {
		String to = param(request, "status");
		int orderId = intParam(request, "orderId", 0);
		DB.begin();
		try {
			switch (mode) {
			case "delivery":
				forceDelivery(request, devId, to);
				flash(request, "배송 상태를 강제로 변경했습니다. (is_forced = 'Y')");
				break;
			case "order":
				Order order = OrderDAO.detail(orderId, 0);
				if (order == null) {
					throw new Fail("주문을 찾을 수 없습니다.");
				}
				if (!"정상".equals(to) && !"취소".equals(to)) {
					throw new Fail("알 수 없는 주문 상태입니다.");
				}
				if (order.getOrderStatus().equals(to)) {
					throw new Fail("이미 " + to + " 상태입니다.");
				}
				boolean cancel = "취소".equals(to);
				boolean stock = "Y".equals(param(request, "restoreStock"));
				OrderDAO.setOrderStatus(orderId, cancel);
				for (int i = 0; stock && i < order.getItems().size(); i++) {
					int pid = order.getItems().get(i).getProductId();
					int qty = order.getItems().get(i).getQuantity();
					if (cancel) {
						ProductDAO.increaseStock(pid, qty);
					} else {
						ProductDAO.decreaseStock(pid, qty);
					}
				}
				AdminDAO.devLog(devId, "FORCE_ORDER_STATUS", "orders", orderId,
						"강제: " + order.getOrderStatus() + " -> " + to + " / 재고반영 " + (stock ? "O" : "X"));
				flash(request, "주문 상태를 강제로 변경했습니다.");
				break;
			case "pay":
				if (!"완료".equals(to) && !"환불".equals(to)) {
					throw new Fail("알 수 없는 결제 상태입니다.");
				}
				if (OrderDAO.setPayStatus(orderId, "환불".equals(to)) == 0) {
					throw new Fail("결제 상태를 변경하지 못했습니다.");
				}
				AdminDAO.devLog(devId, "FORCE_PAY_STATUS", "payment", orderId, "강제: -> " + to);
				flash(request, "결제 상태를 강제로 변경했습니다.");
				break;
			case "stock":
				int newStock = intParam(request, "stock", 0);
				if (newStock < 0) {
					throw new Fail("재고는 0 이상이어야 합니다.");
				}
				Product p = ProductDAO.byId(intParam(request, "productId", 0), false);
				if (p == null) {
					throw new Fail("상품을 찾을 수 없습니다.");
				}
				ProductDAO.setStock(p.getProductId(), newStock);
				AdminDAO.devLog(devId, "FORCE_STOCK_SET", "product", p.getProductId(),
						p.getName() + ": " + p.getStock() + " -> " + newStock);
				flash(request, "재고를 변경했습니다.");
				break;
			case "shop":
				String now = ConfigDAO.value("SHOP_STATUS");
				String next = "MAINTENANCE".equals(now) ? "OPEN" : "MAINTENANCE";
				ConfigDAO.update("SHOP_STATUS", next);
				AdminDAO.devLog(devId, "SHOP_STATUS_TOGGLE", "system_config", null, "SHOP_STATUS: " + now + " -> " + next);
				flash(request, "점검 모드: " + next);
				break;
			case "config":
				String key = param(request, "configKey");
				String value = param(request, "configValue");
				if ("SHOP_STATUS".equals(key) && !"OPEN".equals(value) && !"MAINTENANCE".equals(value)) {
					throw new Fail("SHOP_STATUS 는 OPEN 또는 MAINTENANCE 만 가능합니다.");
				}
				String before = ConfigDAO.value(key);
				if (ConfigDAO.update(key, value) == 0) {
					throw new Fail("설정 키를 찾을 수 없습니다: " + key);
				}
				AdminDAO.devLog(devId, "SHOP_STATUS".equals(key) ? "SHOP_STATUS_TOGGLE" : "CONFIG_UPDATE",
						"system_config", null, key + ": " + before + " -> " + value);
				flash(request, "설정을 변경했습니다.");
				break;
			}
			DB.commit();
		} finally {
			DB.end();
		}
	}

	/* 배송 단계 강제 변경 — 배송중 이후로 보낼 때 운송장이 비었으면 DEV 값으로 채운다 */
	private void forceDelivery(HttpServletRequest request, int devId, String to) throws SQLException {
		int deliveryId = intParam(request, "deliveryId", 0);
		String from = OrderDAO.deliveryStatus(deliveryId);
		if (from == null) {
			throw new Fail("배송 정보를 찾을 수 없습니다.");
		}
		if (OrderDAO.step(to) < 0) {
			throw new Fail("알 수 없는 배송 상태입니다.");
		}
		if (from.equals(to)) {
			throw new Fail("이미 " + to + " 상태입니다.");
		}
		String courier = param(request, "courier");
		String trackingNo = param(request, "trackingNo");
		if (OrderDAO.step(to) >= OrderDAO.step("배송중")) {
			courier = isEmpty(courier) ? "DEV택배" : courier;
			trackingNo = isEmpty(trackingNo) ? "DEV" + System.currentTimeMillis() % 100000000L : trackingNo;
		}
		if (OrderDAO.force(deliveryId, to, courier, trackingNo) == 0) {
			throw new Fail("상태를 변경하지 못했습니다.");
		}
		OrderDAO.addHistory(deliveryId, from, to, devId, true);
		AdminDAO.devLog(devId, "FORCE_DELIVERY_STATUS", "delivery", deliveryId, "강제: " + from + " -> " + to);
	}
}
