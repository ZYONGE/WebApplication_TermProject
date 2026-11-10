package shop.service;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import shop.dao.ConflictDAO;
import shop.dao.ProductDAO;
import shop.dao.TrackItemDAO;
import shop.dto.CartItemDTO;
import shop.dto.ConflictDTO;
import shop.dto.IntakeWarningDTO;
import shop.dto.ProductDTO;

/*
  동시 섭취 경고 — 조합 탭 · 상품상세 · 장바구니 · 주문서가 함께 쓴다 (ERD § 7.1 경고 계산).

      상품 묶음
        → 트랙은 track_item 으로 구성 낱개로 펼친다 (어느 트랙에서 왔는지 기억)
        → 같은 낱개가 두 번 이상 나오면  DUPLICATE  "중복 섭취"
        → 서로 다른 낱개 쌍마다 supplement_conflict 조회 → CONFLICT  "함께 드시지 마세요"

  ★ 경고는 안내만 한다. 구매를 막지 않는다 — 여기서 예외를 던지지 않는다.
  ★ 일반 정보이며 의학적 조언이 아니다. 고지 문구는 화면(warnbox)이 함께 띄운다.
*/
public class IntakeService {

	private final ProductDAO productDAO = new ProductDAO();
	private final TrackItemDAO trackItemDAO = new TrackItemDAO();
	private final ConflictDAO conflictDAO = new ConflictDAO();

	/* 펼친 낱개 한 개 — 어떤 낱개가 어느 트랙을 거쳐 들어왔는가 (직접 골랐으면 via = null) */
	private static class Entry {
		final int id;
		final String name;
		final String via;

		Entry(int id, String name, String via) {
			this.id = id;
			this.name = name;
			this.via = via;
		}
	}

	/* 장바구니 · 주문서 — 주문 대상 줄만 본다. 실제로 함께 먹게 될 묶음이기 때문이다. */
	public List<IntakeWarningDTO> checkCart(List<CartItemDTO> items) throws SQLException {
		List<Integer> ids = new ArrayList<Integer>();
		for (CartItemDTO item : items) {
			if (item.isOrderable()) {
				ids.add(Integer.valueOf(item.getProductId()));
			}
		}
		return check(ids);
	}

	/* 상품 id 묶음 → 경고 목록. 순서는 금지 조합 먼저, 중복 섭취 나중. */
	public List<IntakeWarningDTO> check(Collection<Integer> productIds) throws SQLException {
		List<IntakeWarningDTO> warnings = new ArrayList<IntakeWarningDTO>();
		if (productIds == null || productIds.isEmpty()) {
			return warnings;
		}

		/* 1. 트랙을 구성품으로 펼친다 */
		List<Entry> entries = expand(new LinkedHashSet<Integer>(productIds));

		/* 낱개 id → 처음 나온 자리 (경고 문구에 "어느 트랙에서" 를 붙이려고) */
		Map<Integer, List<Entry>> byId = new LinkedHashMap<Integer, List<Entry>>();
		for (Entry e : entries) {
			Integer key = Integer.valueOf(e.id);
			if (!byId.containsKey(key)) {
				byId.put(key, new ArrayList<Entry>());
			}
			byId.get(key).add(e);
		}

		/* 2. 금지 조합 — 펼친 낱개 집합 안의 쌍 */
		for (ConflictDTO c : conflictDAO.findAmong(byId.keySet())) {
			Entry a = byId.get(Integer.valueOf(c.getProductA())).get(0);
			Entry b = byId.get(Integer.valueOf(c.getProductB())).get(0);
			warnings.add(warning(IntakeWarningDTO.CONFLICT, a, b, c.getMessage()));
		}

		/* 3. 중복 섭취 — 같은 낱개가 두 번 이상 (낱개 + 그 낱개가 든 트랙, 트랙 + 트랙) */
		for (List<Entry> same : byId.values()) {
			if (same.size() < 2) {
				continue;
			}
			Entry a = same.get(0);
			Entry b = same.get(1);
			warnings.add(warning(IntakeWarningDTO.DUPLICATE, a, b,
					"'" + a.name + "' 이(가) " + same.size() + "번 들어갑니다. "
					+ "같은 영양소를 겹쳐 드시게 됩니다."));
		}
		return warnings;
	}

	/*
	  상품상세 — 이 상품과 함께 드시면 안 되는 낱개 목록.
	  트랙이면 구성품 전체 기준이다. 트랙 구성품끼리는 금지 조합이 없으므로(ERD § 8-14)
	  나오는 쌍은 전부 "트랙 밖의 낱개" 와의 쌍이다.
	*/
	public List<ConflictDTO> findConflictsFor(ProductDTO product) throws SQLException {
		Set<Integer> ids = new LinkedHashSet<Integer>();
		if (product.isTrack()) {
			for (ProductDTO item : product.getTrackItems()) {
				ids.add(Integer.valueOf(item.getProductId()));
			}
		} else {
			ids.add(Integer.valueOf(product.getProductId()));
		}
		List<ConflictDTO> result = new ArrayList<ConflictDTO>();
		for (ConflictDTO c : conflictDAO.findInvolving(ids)) {
			/* 구성품끼리의 쌍은 원칙상 없지만, 데이터가 비틀렸을 때 상대가 자기 자신으로 보이지 않게 거른다 */
			if (!(ids.contains(Integer.valueOf(c.getProductA())) && ids.contains(Integer.valueOf(c.getProductB())))) {
				result.add(c);
			}
		}
		return result;
	}

	/* 조합 탭이 화면에 심어 두는 전체 금지 조합 — JS 가 고르는 즉시 다시 계산한다 */
	public List<ConflictDTO> findAllConflicts() throws SQLException {
		return conflictDAO.findAll();
	}

	private List<Entry> expand(Set<Integer> ids) throws SQLException {
		Map<Integer, ProductDTO> products = productDAO.findAnyByIds(ids);

		List<Integer> trackIds = new ArrayList<Integer>();
		for (ProductDTO p : products.values()) {
			if (p.isTrack()) {
				trackIds.add(Integer.valueOf(p.getProductId()));
			}
		}
		Map<Integer, List<ProductDTO>> trackItems = trackItemDAO.findItemsByTracks(trackIds);

		List<Entry> entries = new ArrayList<Entry>();
		for (Integer id : ids) {
			ProductDTO p = products.get(id);
			if (p == null) {
				continue;
			}
			if (p.isTrack()) {
				for (ProductDTO item : trackItems.get(id)) {
					entries.add(new Entry(item.getProductId(), item.getName(), p.getName()));
				}
			} else {
				entries.add(new Entry(p.getProductId(), p.getName(), null));
			}
		}
		return entries;
	}

	private IntakeWarningDTO warning(String type, Entry a, Entry b, String message) {
		IntakeWarningDTO w = new IntakeWarningDTO();
		w.setType(type);
		w.setProductA(a.id);
		w.setProductB(b.id);
		w.setNameA(a.name);
		w.setNameB(b.name);
		w.setViaA(a.via);
		w.setViaB(b.via);
		w.setMessage(message);
		return w;
	}
}
