package shop.controller;

import java.io.IOException;
import java.sql.SQLException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import shop.dto.MemberDTO;
import shop.service.MemberService;
import shop.service.ServiceException;
import shop.util.StringUtil;

/* 2 · 3번 회원가입 · 로그인 (CRUD.md § 2) */
@WebServlet(name = "MemberServlet", urlPatterns = { "/member/join", "/member/login", "/member/logout", "/member/idcheck" })
public class MemberServlet extends BaseServlet {

	private static final long serialVersionUID = 1L;

	private final MemberService memberService = new MemberService();

	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		String action = action(request);
		pageCss(request, "member");

		if ("join".equals(action)) {
			request.setAttribute("pageTitle", "회원가입");
			render(request, response, "/member/join.jsp");
		} else if ("login".equals(action)) {
			request.setAttribute("pageTitle", "로그인");
			request.setAttribute("returnUrl", param(request, "returnUrl"));
			render(request, response, "/member/login.jsp");
		} else if ("logout".equals(action)) {
			logout(request, response);
		} else if ("idcheck".equals(action)) {
			idCheck(request, response);
		} else {
			redirect(request, response, "/");
		}
	}

	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		String action = action(request);
		if ("join".equals(action)) {
			join(request, response);
		} else if ("login".equals(action)) {
			login(request, response);
		} else {
			redirect(request, response, "/");
		}
	}

	private void join(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		MemberDTO member = new MemberDTO();
		member.setLoginId(param(request, "loginId"));
		member.setPassword(request.getParameter("password"));
		member.setName(param(request, "name"));
		member.setEmail(param(request, "email"));
		member.setPhone(param(request, "phone"));

		try {
			memberService.join(member, request.getParameter("passwordConfirm"));
			flash(request, "회원가입이 완료되었습니다. 로그인해 주세요.");
			redirect(request, response, "/member/login");
		} catch (ServiceException e) {
			/* 거절은 입력값을 살려 둔 채 같은 화면으로 돌려보낸다. */
			request.setAttribute("errorMessage", e.getMessage());
			request.setAttribute("form", member);
			request.setAttribute("pageTitle", "회원가입");
			pageCss(request, "member");
			render(request, response, "/member/join.jsp");
		} catch (SQLException e) {
			throw wrap(e);
		}
	}

	private void login(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		String loginId = param(request, "loginId");
		String returnUrl = param(request, "returnUrl");

		try {
			MemberDTO member = memberService.login(loginId, request.getParameter("password"));

			/* 세션 고정 공격을 막기 위해 로그인 시점에 세션을 새로 만든다. */
			HttpSession old = request.getSession(false);
			if (old != null) {
				old.invalidate();
			}
			setLoginMember(request, member);

			if (!StringUtil.isEmpty(returnUrl) && returnUrl.startsWith(request.getContextPath())) {
				/* 열린 리다이렉트를 막는다 — 컨텍스트 안의 경로만 허용한다. */
				response.sendRedirect(returnUrl);
			} else {
				redirect(request, response, "/");
			}
		} catch (ServiceException e) {
			request.setAttribute("errorMessage", e.getMessage());
			request.setAttribute("loginId", loginId);
			request.setAttribute("returnUrl", returnUrl);
			request.setAttribute("pageTitle", "로그인");
			pageCss(request, "member");
			render(request, response, "/member/login.jsp");
		} catch (SQLException e) {
			throw wrap(e);
		}
	}

	private void logout(HttpServletRequest request, HttpServletResponse response)
			throws IOException {
		HttpSession session = request.getSession(false);
		if (session != null) {
			session.invalidate();
		}
		redirect(request, response, "/");
	}

	/* 아이디 중복 확인 팝업이 부르는 응답 — 화면 없이 글자만 돌려준다. */
	private void idCheck(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		try {
			boolean available = memberService.isLoginIdAvailable(param(request, "loginId"));
			response.setContentType("text/plain; charset=UTF-8");
			response.getWriter().write(available ? "OK" : "NO");
		} catch (SQLException e) {
			throw wrap(e);
		}
	}
}
