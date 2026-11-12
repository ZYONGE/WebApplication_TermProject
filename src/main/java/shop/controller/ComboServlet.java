package shop.controller;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import shop.dto.ComboDTO;
import shop.service.ComboService;
import shop.service.ServiceException;

/*
  40번 조합 저장 · 41번 내 조합 (CRUD.md · ERD § 7.1)
      GET  /mypage/combo         내 조합 목록 (구성 · 현재 가격 · 재고 표시)
      POST /mypage/combo/save    조합 탭에서 이름 + 일수 + 고른 낱개 저장
      POST /mypage/combo/delete  삭제

  /mypage/* 라 UserFilter 를 그대로 탄다 — USER 만 들어온다.
  ★ 서비스가 언제나 member_id 를 함께 건다.
*/
@WebServlet(name = "ComboServlet", urlPatterns = { "/mypage/combo", "/mypage/combo/save", "/mypage/combo/delete" })
public class ComboServlet extends BaseServlet {

	private static final long serialVersionUID = 1L;

	private final ComboService comboService = new ComboService();

	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		int memberId = loginMemberId(request);
		try {
			/* 목록에서 구성 이름 · 현재 합계를 함께 보여준다. 조합은 회원당 몇 건이라 건마다 읽는다. */
			List<ComboDTO> combos = new ArrayList<ComboDTO>();
			for (ComboDTO c : comboService.findMyCombos(memberId)) {
				combos.add(comboService.findMyCombo(c.getComboId(), memberId));
			}
			request.setAttribute("pageTitle", "내 조합");
			pageCss(request, "mypage");
			request.setAttribute("combos", combos);
			render(request, response, "/mypage/combo.jsp");
		} catch (SQLException e) {
			throw wrap(e);
		}
	}

	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		String action = action(request);
		int memberId = loginMemberId(request);
		try {
			if ("save".equals(action)) {
				List<Integer> ids = CartServlet.intParams(request, "productId");
				int days = intParam(request, "days", 0);
				try {
					int comboId = comboService.save(memberId, param(request, "comboName"), days, ids,
							"Y".equals(param(request, "overwrite")));
					/* 저장 안내 한 줄만 — 조합 탭의 "불러왔습니다" 배너는 saved=1 이면 내지 않는다 */
					flash(request, "조합 '" + shop.util.StringUtil.trim(param(request, "comboName")) + "' 을(를) 저장했습니다.");
					redirect(request, response, "/custom?combo=" + comboId + "&saved=1");
				} catch (ServiceException e) {
					/* 고르던 상태를 잃지 않게 쿼리로 되돌린다 */
					flashError(request, e.getMessage());
					redirect(request, response, "/custom?pick=" + join(ids) + "&days=" + days);
				}
				return;
			}
			if ("delete".equals(action)) {
				try {
					comboService.delete(intParam(request, "comboId", 0), memberId);
					flash(request, "조합을 삭제했습니다.");
				} catch (ServiceException e) {
					flashError(request, e.getMessage());
				}
			}
		} catch (SQLException e) {
			throw wrap(e);
		}
		redirect(request, response, "/mypage/combo");
	}

	private String join(List<Integer> ids) {
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
