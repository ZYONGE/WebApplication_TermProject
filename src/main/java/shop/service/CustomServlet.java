package shop.service;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import shop.dao.CartDAO;
import shop.dao.CategoryDAO;
import shop.dao.ConfigDAO;
import shop.dao.ConflictDAO;
import shop.dao.ProductDAO;
import shop.model.Category;
import shop.model.Combo;
import shop.model.Item;
import shop.model.Product;

/* 조합 맞추기 (/custom) 와 내 조합 저장 · 삭제 */
@WebServlet({ "/custom", "/mypage/combo", "/mypage/combo/save", "/mypage/combo/delete" })
public class CustomServlet extends BaseServlet {

	private static final long serialVersionUID = 1L;

	@Override
	protected void get(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException, SQLException {
		int memberId = loginId(request);
		if (request.getServletPath().equals("/mypage/combo")) {
			List<Combo> combos = new ArrayList<Combo>();
			for (Combo c : CartDAO.combos(memberId)) {
				combos.add(CartDAO.combo(c.getComboId(), memberId));
			}
			request.setAttribute("combos", combos);
			render(request, response, "mypage/combo", "내 조합");
			return;
		}
		boolean user = loginMember(request) != null && "USER".equals(loginMember(request).getRole());
		int maxDays = ConfigDAO.maxDays();
		int days = intParam(request, "days", ConfigDAO.DEFAULT_DAYS);

		/* 탭 = 판매 중인 낱개가 있는 카테고리. 탭마다 그 카테고리의 낱개 목록 */
		List<Product> singles = ProductDAO.byType("SINGLE", true);
		List<Product> tracks = ProductDAO.byType("TRACK", true);
		Map<Integer, Product> singleById = new LinkedHashMap<Integer, Product>();
		Map<Integer, List<Product>> tabProducts = new LinkedHashMap<Integer, List<Product>>();
		for (Product p : singles) {
			singleById.put(p.getProductId(), p);
			if (!tabProducts.containsKey(p.getCategoryId())) {
				tabProducts.put(p.getCategoryId(), new ArrayList<Product>());
			}
			tabProducts.get(p.getCategoryId()).add(p);
		}
		List<Category> tabs = new ArrayList<Category>();
		for (Category c : CategoryDAO.ordered()) {
			if (tabProducts.containsKey(c.getCategoryId())) {
				tabs.add(c);
			}
		}

		/* 처음 고른 목록 — 내 조합(combo) · 트랙(track) · 주소의 pick 중 하나에서 */
		List<Integer> requested = new ArrayList<Integer>();
		Combo loadedCombo = null;
		Product loadedTrack = null;
		int comboId = intParam(request, "combo", 0);
		int trackId = intParam(request, "track", 0);
		if (comboId > 0 && user) {
			loadedCombo = CartDAO.combo(comboId, memberId);
			if (loadedCombo == null) {
				flashError(request, "조합을 찾을 수 없습니다.");
			} else {
				days = loadedCombo.getDays();
				for (Item item : loadedCombo.getItems()) {
					requested.add(item.getProductId());
				}
			}
		} else if (trackId > 0) {
			for (Product t : tracks) {
				if (t.getProductId() == trackId) {
					loadedTrack = t;
					for (Product item : t.getTrackItems()) {
						requested.add(item.getProductId());
					}
				}
			}
		} else if (param(request, "pick") != null) {
			for (String part : param(request, "pick").split(",")) {
				if (toInt(part, 0) > 0) {
					requested.add(toInt(part, 0));
				}
			}
		}

		/* 지금 살 수 있는 낱개만 고른 목록에 넣고, 내 조합에서 빠진 것은 이름을 알려 준다 */
		List<Product> picked = new ArrayList<Product>();
		Set<Integer> pickedIds = new LinkedHashSet<Integer>();
		List<String> unavailable = new ArrayList<String>();
		int packPrice = 0;
		for (Integer id : requested) {
			Product p = singleById.get(id);
			if (p != null && pickedIds.add(id)) {
				picked.add(p);
				packPrice += p.getPrice();
			} else if (p == null && loadedCombo != null) {
				for (Item item : loadedCombo.getItems()) {
					if (item.getProductId() == id) {
						unavailable.add(item.getProductName());
					}
				}
			}
		}
		days = Math.min(Math.max(days, 1), maxDays);
		String pickParam = joinIds(pickedIds);

		request.setAttribute("tabs", tabs);
		request.setAttribute("tabProducts", tabProducts);
		request.setAttribute("tracks", tracks);
		request.setAttribute("conflicts", ConflictDAO.all());
		request.setAttribute("picked", picked);
		request.setAttribute("pickedIds", pickedIds);
		request.setAttribute("pickParam", pickParam);
		request.setAttribute("unavailable", String.join(", ", unavailable));
		request.setAttribute("days", days);
		request.setAttribute("quickDays", ConfigDAO.quickDays());
		request.setAttribute("maxDays", maxDays);
		request.setAttribute("packPrice", packPrice);
		request.setAttribute("totalPrice", packPrice * days);
		request.setAttribute("deliveryFee", ConfigDAO.deliveryFee(packPrice * days));
		request.setAttribute("deliveryFeeBase", ConfigDAO.baseDeliveryFee());
		request.setAttribute("freeShippingMin", ConfigDAO.freeShippingMin());
		request.setAttribute("warnings", ConflictDAO.warnings(pickedIds));
		request.setAttribute("loadedCombo", loadedCombo);
		request.setAttribute("loadedTrack", loadedTrack);
		if (user) {
			request.setAttribute("myCombos", CartDAO.combos(memberId));
		}
		render(request, response, "custom/index", "조합 맞추기");
	}

	@Override
	protected void post(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException, SQLException {
		int memberId = loginId(request);
		if (request.getServletPath().equals("/mypage/combo/delete")) {
			if (CartDAO.deleteCombo(intParam(request, "comboId", 0), memberId) == 0) {
				flashError(request, "조합을 찾을 수 없습니다.");
			} else {
				flash(request, "조합을 삭제했습니다.");
			}
			redirect(request, response, "/mypage/combo");
			return;
		}
		if (!request.getServletPath().equals("/mypage/combo/save")) {
			redirect(request, response, "/mypage/combo");
			return;
		}
		List<Integer> ids = intParams(request, "productId");
		int days = intParam(request, "days", 0);
		String name = param(request, "comboName");
		try {
			if (isEmpty(name)) {
				throw new Fail("조합 이름을 입력하세요.");
			}
			if (name.length() > 50) {
				throw new Fail("조합 이름은 50자 이내로 입력하세요.");
			}
			checkDays(days);
			Set<Integer> unique = new LinkedHashSet<Integer>(ids);
			if (unique.isEmpty()) {
				throw new Fail("저장할 영양제를 하나 이상 고르세요.");
			}
			Map<Integer, Product> products = ProductDAO.byIds(unique);
			for (Integer id : unique) {
				if (products.get(id) == null || products.get(id).isTrack()) {
					throw new Fail("조합에는 낱개 영양제만 저장할 수 있습니다.");
				}
			}
			/* 같은 이름이 있으면 덮어쓰기 확인(overwrite=Y)을 받은 뒤에만 덮어쓴다 */
			int sameName = CartDAO.comboIdByName(memberId, name);
			if (sameName > 0 && !"Y".equals(param(request, "overwrite"))) {
				throw new Fail("'" + name + "' 이름의 조합이 이미 있습니다. 덮어쓰려면 확인을 눌러 다시 저장하세요.");
			}
			int comboId = CartDAO.saveCombo(sameName, memberId, name, days, unique);
			flash(request, "조합 '" + name + "' 을(를) 저장했습니다.");
			redirect(request, response, "/custom?combo=" + comboId + "&saved=1");
		} catch (Fail e) {
			flashError(request, e.getMessage());
			String pick = joinIds(ids);
			redirect(request, response, "/custom?pick=" + pick + "&days=" + days);
		}
	}
}
