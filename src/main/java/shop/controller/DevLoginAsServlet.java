package shop.controller;

import java.io.IOException;
import java.sql.SQLException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import shop.dto.MemberDTO;
import shop.filter.DevFilter;
import shop.service.DevService;
import shop.service.ServiceException;
import shop.util.WebUtil;

/*
  34번 dev 계정 전환 (CRUD.md § 3)

  ★ 감사 구멍을 의식적으로 메운다 (ERD § 8 쟁점 7).
    전환 후의 조작은 admin_log.member_id 에 "전환된 회원" 으로 찍혀 실제 행위자가
    사라진다. 그래서 전환 시점 로그의 detail 에 impersonated_by 를 남기고,
    /dev/log 에서 그 값을 보여 준다.

  ★ 원래 DEV 계정을 세션에 따로 보관한다(devOrigin). 그래야 USER 로 전환한 뒤에도
    DevFilter 를 통과해 /dev 로 돌아올 수 있다.
*/
@WebServlet(name = "DevLoginAsServlet", urlPatterns = { "/dev/loginAs" })
public class DevLoginAsServlet extends BaseServlet {

	private static final long serialVersionUID = 1L;

	private final DevService devService = new DevService();

	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		if ("restore".equals(param(request, "action"))) {
			restore(request, response);
			return;
		}
		try {
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
		HttpSession session = request.getSession();
		MemberDTO current = loginMember(request);

		try {
			MemberDTO target = devService.loginAs(originId(request, current),
					intParam(request, "memberId", 0));

			/* 원래 DEV 계정은 한 번만 보관한다 — 전환을 여러 번 해도 원점은 하나다. */
			if (session.getAttribute(DevFilter.SESSION_DEV_ORIGIN) == null && current != null) {
				session.setAttribute(DevFilter.SESSION_DEV_ORIGIN, current);
			}
			session.setAttribute(WebUtil.SESSION_MEMBER, target);
			flash(request, target.getLoginId() + " 계정으로 전환했습니다.");
			redirect(request, response, "/");
		} catch (ServiceException e) {
			flashError(request, e.getMessage());
			redirect(request, response, "/dev/loginAs");
		} catch (SQLException e) {
			throw wrap(e);
		}
	}

	/* 전환을 풀고 원래 DEV 계정으로 돌아간다. */
	private void restore(HttpServletRequest request, HttpServletResponse response)
			throws IOException {
		HttpSession session = request.getSession(false);
		if (session != null) {
			Object origin = session.getAttribute(DevFilter.SESSION_DEV_ORIGIN);
			if (origin instanceof MemberDTO) {
				session.setAttribute(WebUtil.SESSION_MEMBER, origin);
				session.removeAttribute(DevFilter.SESSION_DEV_ORIGIN);
				flash(request, "원래 계정으로 돌아왔습니다.");
			}
		}
		redirect(request, response, "/dev/");
	}

	/* 로그를 남길 주체는 "전환된 계정" 이 아니라 "원래 DEV 계정" 이다. */
	private int originId(HttpServletRequest request, MemberDTO current) {
		HttpSession session = request.getSession(false);
		if (session != null) {
			Object origin = session.getAttribute(DevFilter.SESSION_DEV_ORIGIN);
			if (origin instanceof MemberDTO) {
				return ((MemberDTO) origin).getMemberId();
			}
		}
		return current == null ? 0 : current.getMemberId();
	}
}
