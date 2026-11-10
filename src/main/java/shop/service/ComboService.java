package shop.service;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import shop.dao.ComboDAO;
import shop.dao.ProductDAO;
import shop.dto.ComboDTO;
import shop.dto.ProductDTO;
import shop.util.DBUtil;
import shop.util.StringUtil;

/*
  저장한 조합 (내 조합) — 40번 조합 저장 · 41번 내 조합 (CRUD.md · ERD § 7.1).

  ★ 언제나 member_id = 세션 회원을 함께 건다. 남의 조합 번호로는 열리지도 지워지지도 않는다.
  ★ 낱개만 담는다 (ERD § 8-11). 구성은 1개 이상.
  ★ 같은 이름이 이미 있으면 덮어쓸지 묻는다 — overwrite 가 true 일 때만 덮어쓴다.
*/
public class ComboService {

	public static final int MAX_NAME = 50;

	private final ComboDAO comboDAO = new ComboDAO();
	private final ProductDAO productDAO = new ProductDAO();
	private final ConfigService configService = new ConfigService();

	public List<ComboDTO> findMyCombos(int memberId) throws SQLException {
		return comboDAO.findByMember(memberId);
	}

	/* 구성까지 채운 한 건. 남의 조합이면 null */
	public ComboDTO findMyCombo(int comboId, int memberId) throws SQLException {
		ComboDTO combo = comboDAO.findByIdForMember(comboId, memberId);
		if (combo != null) {
			combo.setItems(comboDAO.findItems(comboId));
		}
		return combo;
	}

	/*
	  저장 — 한 트랜잭션 (ERD § 7.1 조합 저장).
	      combo INSERT (또는 같은 이름이면 UPDATE) → combo_item 전부 DELETE → 다시 INSERT

	  돌려주는 값: combo_id
	*/
	public int save(int memberId, String name, int days, List<Integer> productIds, boolean overwrite)
			throws SQLException {
		String trimmed = StringUtil.trim(name);
		if (StringUtil.isEmpty(trimmed)) {
			throw new ServiceException("조합 이름을 입력하세요.");
		}
		if (trimmed.length() > MAX_NAME) {
			throw new ServiceException("조합 이름은 " + MAX_NAME + "자 이내로 입력하세요.");
		}
		int max = configService.maxIntakeDays();
		if (days < 1 || days > max) {
			throw new ServiceException("섭취 일수는 1~" + max + "일로 입력하세요.");
		}

		Set<Integer> ids = new LinkedHashSet<Integer>(productIds);
		if (ids.isEmpty()) {
			throw new ServiceException("저장할 영양제를 하나 이상 고르세요.");
		}
		Map<Integer, ProductDTO> products = productDAO.findAnyByIds(ids);
		for (Integer id : ids) {
			ProductDTO p = products.get(id);
			if (p == null || p.isTrack()) {
				throw new ServiceException("조합에는 낱개 영양제만 저장할 수 있습니다.");
			}
		}

		Connection conn = null;
		try {
			conn = DBUtil.beginTransaction();
			int comboId = comboDAO.findIdByName(conn, memberId, trimmed);
			if (comboId > 0) {
				if (!overwrite) {
					throw new ServiceException("'" + trimmed + "' 이름의 조합이 이미 있습니다. 덮어쓰려면 확인을 눌러 다시 저장하세요.");
				}
				comboDAO.update(conn, comboId, memberId, days);
				comboDAO.deleteItems(conn, comboId);
			} else {
				comboId = comboDAO.insert(conn, memberId, trimmed, days);
			}
			int order = 1;
			for (Integer id : ids) {
				comboDAO.insertItem(conn, comboId, id.intValue(), order++);
			}
			DBUtil.commit(conn);
			return comboId;
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

	public void delete(int comboId, int memberId) throws SQLException {
		if (comboDAO.delete(comboId, memberId) == 0) {
			throw new ServiceException("조합을 찾을 수 없습니다.");
		}
	}
}
