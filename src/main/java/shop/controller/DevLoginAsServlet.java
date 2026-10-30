package shop.controller;

import java.io.IOException;
import java.sql.SQLException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import shop.dto.MemberDTO;
import shop.service.DevService;
import shop.service.ServiceException;
import shop.util.RoleUtil;
import shop.util.WebUtil;

/*
  34번 dev 계정 전환 (ERD § 5.4)

      GET  /dev/loginAs          전환 대상 목록
      POST /dev/loginAs          전환 — 세션의 회원 · 역할을 대상 계정 값으로 교체
      GET  /dev/loginAs/return   복귀 — 원래 DEV 로 되돌린다

  ★ 전환하면 세션은 완전히 대상 계정이 된다. 그래서 전환 중에는 /dev 도 막힌다(DevFilter).
    원래 DEV 의 id 는 impersonatorId 로만 남기고, 복귀 경로만 그 값으로 판정한다.
  ★ 전환 중에 남는 admin_log 에는 impersonated_by 가 자동으로 붙는다(AdminLogDAO).
*/
@WebServlet(name = "DevLoginAsServlet", urlPatterns = { "/dev/loginAs", "/dev/loginAs/return" })
public class DevLoginAsServlet extends BaseServlet {

	private static final long serialVersionUID = 1L;

	private final DevService devService = new DevService();

	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		try {
			if ("return".equals(action(request))) {
				returnToDev(request, response);
				return;
			}
			request.setAttribute("pageTitle", "dev · 계정 전환");
			pageCss(request, "admin");
			request.setAttribute("members", devService.findAllMembers());
			render(request, response, "/dev/loginAs.jsp");
		} catch (SQLException e) {
			throw wrap(e);
		}
	}

	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		MemberDTO dev = loginMember(request);
		try {
			MemberDTO target = devService.loginAs(dev.getMemberId(), intParam(request, "memberId", 0));

			HttpSession session = request.getSession();
			session.setAttribute(RoleUtil.SESSION_IMPERSONATOR, Integer.valueOf(dev.getMemberId()));
			session.setAttribute(WebUtil.SESSION_MEMBER, target);
			flash(request, target.getLoginId() + " 계정으로 전환했습니다. 복귀는 상단 '원래 계정으로'.");
			redirect(request, response, RoleUtil.homeOf(target.getRole()));
		} catch (ServiceException e) {
			flashError(request, e.getMessage());
			redirect(request, response, "/dev/loginAs");
		} catch (SQLException e) {
			throw wrap(e);
		}
	}

	/* 복귀 — LOGIN_AS_RETURN 은 impersonatorId 의 DEV 로 기록된다. */
	private void returnToDev(HttpServletRequest request, HttpServletResponse response)
			throws IOException, SQLException {
		int devId = RoleUtil.impersonatorId(request);
		MemberDTO from = loginMember(request);
		MemberDTO dev = devService.returnFromLoginAs(devId, from == null ? 0 : from.getMemberId());

		HttpSession session = request.getSession();
		session.removeAttribute(RoleUtil.SESSION_IMPERSONATOR);
		session.setAttribute(WebUtil.SESSION_MEMBER, dev);
		flash(request, "원래 계정으로 돌아왔습니다.");
		redirect(request, response, "/dev/");
	}
}
