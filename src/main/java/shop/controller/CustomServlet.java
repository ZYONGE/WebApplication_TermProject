package shop.controller;

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

import shop.dto.CategoryDTO;
import shop.dto.ComboDTO;
import shop.dto.ComboItemDTO;
import shop.dto.ProductDTO;
import shop.service.ComboService;
import shop.service.ConfigService;
import shop.service.IntakeService;
import shop.service.ProductService;
import shop.util.RoleUtil;

/*
  37번 조합 탭 — /custom (CRUD.md · ERD § 7.1). 레퍼런스: 다나와 "PC 견적 맞추기".

      분류 탭 (낱개 소분류)  |  탭별 낱개 목록 (체크)  |  내 조합 (일수 · 합계 · 경고 · 담기 · 저장)

  ★ 공개 열람이다 (비로그인 포함). 담기 · 저장 · 내 조합 불러오기는 USER 에게만 보인다.
    담기는 /cart/addAll, 저장은 /mypage/combo/save 로 보내고 둘 다 UserFilter 를 탄다.
  ★ 상태는 URL 쿼리로도 둔다 — /custom?pick=1,4,7&days=30 (IMPACT K3, 새로고침 · 링크 공유).
    출발점 불러오기: ?track=13 (트랙 구성), ?combo=2 (내 조합 — 세션 회원 것만 열린다).
  ★ 화면의 즉시 계산(js/custom.js)은 편의일 뿐이다. 담을 때 서버가 다시 검사한다.
*/
@WebServlet(name = "CustomServlet", urlPatterns = { "/custom" })
public class CustomServlet extends BaseServlet {

	private static final long serialVersionUID = 1L;

	private final ProductService productService = new ProductService();
	private final IntakeService intakeService = new IntakeService();
	private final ComboService comboService = new ComboService();
	private final ConfigService configService = new ConfigService();

	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		try {
			show(request, response);
		} catch (SQLException e) {
			throw wrap(e);
		}
	}

	private void show(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException, SQLException {
		boolean user = RoleUtil.is(request, "USER");
		int memberId = loginMemberId(request);
		int maxDays = configService.maxIntakeDays();

		/* 고를 수 있는 낱개 · 불러올 수 있는 트랙 (노출 조건 통과분) */
		List<ProductDTO> singles = productService.findSinglesOnSale();
		List<ProductDTO> tracks = productService.findTracksOnSale();
		Map<Integer, ProductDTO> singleById = new LinkedHashMap<Integer, ProductDTO>();
		for (ProductDTO p : singles) {
			singleById.put(Integer.valueOf(p.getProductId()), p);
		}

		/*
		  분류 탭 — 고를 수 있는 낱개가 실제로 들어 있는 카테고리만, 카테고리 트리 순서대로.
		  대분류 이름을 코드에 박지 않는다. 카테고리를 추가 · 이름 변경해도 탭이 데이터를 따라간다.
		*/
		Set<Integer> singleCats = new LinkedHashSet<Integer>();
		for (ProductDTO p : singles) {
			singleCats.add(Integer.valueOf(p.getCategoryId()));
		}
		List<CategoryDTO> tabs = new ArrayList<CategoryDTO>();
		for (CategoryDTO c : productService.findAllCategories()) {
			if (singleCats.contains(Integer.valueOf(c.getCategoryId()))) {
				tabs.add(c);
			}
		}

		/* 출발점 — 내 조합 > 트랙 > pick 쿼리 */
		int days = intParam(request, "days", ConfigService.DEFAULT_DAYS);
		List<Integer> requested = new ArrayList<Integer>();
		List<String> unavailable = new ArrayList<String>();
		ComboDTO loadedCombo = null;
		ProductDTO loadedTrack = null;

		int comboId = intParam(request, "combo", 0);
		int trackId = intParam(request, "track", 0);
		if (comboId > 0 && user) {
			loadedCombo = comboService.findMyCombo(comboId, memberId);
			if (loadedCombo == null) {
				flashError(request, "조합을 찾을 수 없습니다.");
			} else {
				days = loadedCombo.getDays();
				for (ComboItemDTO item : loadedCombo.getItems()) {
					requested.add(Integer.valueOf(item.getProductId()));
				}
			}
		} else if (trackId > 0) {
			for (ProductDTO t : tracks) {
				if (t.getProductId() == trackId) {
					loadedTrack = t;
					for (ProductDTO item : t.getTrackItems()) {
						requested.add(Integer.valueOf(item.getProductId()));
					}
				}
			}
		} else {
			requested.addAll(parseIds(param(request, "pick")));
		}

		/* 담을 수 있는 것만 고른 상태로 둔다. 판매 중지 · 재고 없는 낱개는 이름만 알린다 */
		List<ProductDTO> picked = new ArrayList<ProductDTO>();
		Set<Integer> pickedIds = new LinkedHashSet<Integer>();
		for (Integer id : requested) {
			ProductDTO p = singleById.get(id);
			if (p != null) {
				if (pickedIds.add(id)) {
					picked.add(p);
				}
			} else if (loadedCombo != null) {
				for (ComboItemDTO item : loadedCombo.getItems()) {
					if (item.getProductId() == id.intValue()) {
						unavailable.add(item.getProductName());
					}
				}
			}
		}
		if (days < 1 || days > maxDays) {
			days = Math.min(Math.max(days, 1), maxDays);
		}

		int packPrice = 0;
		for (ProductDTO p : picked) {
			packPrice += p.getPrice();
		}
		int total = packPrice * days;

		request.setAttribute("pageTitle", "조합 맞추기");
		pageCss(request, "custom");
		request.setAttribute("tabs", tabs);
		request.setAttribute("singles", singles);
		request.setAttribute("tracks", tracks);
		request.setAttribute("conflicts", intakeService.findAllConflicts());

		request.setAttribute("picked", picked);
		request.setAttribute("pickedIds", pickedIds);
		request.setAttribute("pickParam", join(pickedIds));
		request.setAttribute("unavailable", unavailable);
		request.setAttribute("days", Integer.valueOf(days));
		request.setAttribute("quickDays", configService.quickDays());
		request.setAttribute("maxDays", Integer.valueOf(maxDays));
		request.setAttribute("packPrice", Integer.valueOf(packPrice));
		request.setAttribute("totalPrice", Integer.valueOf(total));
		request.setAttribute("deliveryFee", Integer.valueOf(configService.calculateDeliveryFee(total)));
		request.setAttribute("deliveryFeeBase", Integer.valueOf(configService.deliveryFee()));
		request.setAttribute("freeShippingMin", Integer.valueOf(configService.freeShippingMin()));
		request.setAttribute("warnings", intakeService.check(pickedIds));

		request.setAttribute("loadedCombo", loadedCombo);
		request.setAttribute("loadedTrack", loadedTrack);
		if (user) {
			request.setAttribute("myCombos", comboService.findMyCombos(memberId));
		}
		render(request, response, "/custom/index.jsp");
	}

	/* "1,4,7" → [1, 4, 7]. 숫자가 아닌 조각은 버린다. */
	private List<Integer> parseIds(String value) {
		List<Integer> ids = new ArrayList<Integer>();
		if (value == null) {
			return ids;
		}
		for (String part : value.split(",")) {
			int id = shop.util.WebUtil.toInt(part, 0);
			if (id > 0) {
				ids.add(Integer.valueOf(id));
			}
		}
		return ids;
	}

	private String join(Set<Integer> ids) {
		StringBuilder sb = new StringBuilder();
		for (Integer id : ids) {
			if (sb.length() > 0) {
				sb.append(",");
			}
			sb.append(id);
		}
		return sb.toString();
	}
}
