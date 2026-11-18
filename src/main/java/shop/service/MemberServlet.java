package shop.service;

import java.io.IOException;
import java.net.URLEncoder;
import java.sql.SQLException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import shop.dao.MemberDAO;
import shop.model.Member;

/* 회원가입 · 로그인 · 로그아웃 · 아이디 중복확인 */
@WebServlet({ "/member/join", "/member/login", "/member/logout", "/member/idcheck" })
public class MemberServlet extends BaseServlet {

	private static final long serialVersionUID = 1L;

	@Override
	protected void get(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException, SQLException {
		switch (request.getServletPath()) {
		case "/member/join":
			render(request, response, "member/join", "회원가입");
			break;
		case "/member/login":
			request.setAttribute("returnUrl", param(request, "returnUrl"));
			request.setAttribute("loginId", param(request, "loginId"));
			render(request, response, "member/login", "로그인");
			break;
		case "/member/logout":
			request.getSession().invalidate();
			flash(request, "로그아웃했습니다.");
			redirect(request, response, "/");
			break;
		default:
			/* 아이디 중복확인 — 화면의 자바스크립트가 OK / NO 글자를 받는다 */
			String id = param(request, "loginId");
			boolean ok = id != null && id.length() >= 4 && MemberDAO.byLoginId(id) == null;
			response.setContentType("text/plain; charset=UTF-8");
			response.getWriter().write(ok ? "OK" : "NO");
		}
	}

	@Override
	protected void post(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException, SQLException {
		if (request.getServletPath().equals("/member/join")) {
			join(request, response);
		} else if (request.getServletPath().equals("/member/login")) {
			login(request, response);
		} else {
			redirect(request, response, "/");
		}
	}

	private void join(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException, SQLException {
		String loginId = param(request, "loginId");
		try {
			if (isEmpty(loginId)) {
				throw new Fail("아이디를 입력하세요.");
			}
			if (loginId.length() < 4 || loginId.length() > 30) {
				throw new Fail("아이디는 4~30자로 입력하세요.");
			}
			if (!loginId.matches("[A-Za-z0-9_]+")) {
				throw new Fail("아이디는 영문 · 숫자 · 밑줄만 쓸 수 있습니다.");
			}
			checkPassword(request.getParameter("password"), request.getParameter("passwordConfirm"));
			checkProfile(param(request, "name"), param(request, "email"), param(request, "phone"));
			if (MemberDAO.exists("login_id", loginId, 0)) {
				throw new Fail("이미 사용 중인 아이디입니다.");
			}
			if (MemberDAO.exists("email", param(request, "email"), 0)) {
				throw new Fail("이미 가입된 이메일입니다.");
			}
			MemberDAO.insert(loginId, request.getParameter("password"), param(request, "name"),
					param(request, "email"), param(request, "phone"));
			flash(request, "회원가입이 완료되었습니다. 로그인해 주세요.");
			redirect(request, response, "/member/login?loginId=" + URLEncoder.encode(loginId, "UTF-8"));
		} catch (Fail e) {
			request.setAttribute("errorMessage", e.getMessage());
			render(request, response, "member/join", "회원가입");
		}
	}

	private void login(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException, SQLException {
		String loginId = param(request, "loginId");
		String password = request.getParameter("password");
		String returnUrl = param(request, "returnUrl");
		try {
			if (isEmpty(loginId) || isEmpty(password)) {
				throw new Fail("아이디와 비밀번호를 입력하세요.");
			}
			Member member = MemberDAO.byLoginId(loginId);
			if (member == null || !password.equals(member.getPassword())) {
				throw new Fail("아이디 또는 비밀번호가 올바르지 않습니다.");
			}
			if ("LEFT".equals(member.getStatus())) {
				throw new Fail("탈퇴한 계정입니다.");
			}
			request.getSession().invalidate();
			request.getSession().setAttribute("loginMember", member);

			/* 돌아갈 주소 — 같은 사이트 주소이고, 그 역할이 들어갈 수 있는 곳일 때만 */
			String ctx = request.getContextPath();
			String role = member.getRole();
			boolean safe = returnUrl != null && returnUrl.startsWith(ctx + "/");
			if (safe && ("USER".equals(role)
					|| returnUrl.startsWith(ctx + ("ADMIN".equals(role) ? "/admin/" : "/dev/")))) {
				response.sendRedirect(returnUrl);
			} else {
				redirect(request, response, homeOf(role));
			}
		} catch (Fail e) {
			request.setAttribute("errorMessage", e.getMessage());
			request.setAttribute("loginId", loginId);
			request.setAttribute("returnUrl", returnUrl);
			render(request, response, "member/login", "로그인");
		}
	}

	/* 비밀번호 규칙 — 8자 이상, 확인 칸과 같아야 한다 */
	static void checkPassword(String password, String confirm) {
		if (isEmpty(password)) {
			throw new Fail("비밀번호를 입력하세요.");
		}
		if (password.length() < 8 || password.length() > 100) {
			throw new Fail("비밀번호는 8자 이상으로 입력하세요.");
		}
		if (confirm != null && !password.equals(confirm)) {
			throw new Fail("비밀번호 확인이 일치하지 않습니다.");
		}
	}

	/* 이름 · 이메일 · 연락처 규칙 (가입 · 정보수정 공용) */
	static void checkProfile(String name, String email, String phone) {
		if (isEmpty(name) || name.length() > 30) {
			throw new Fail("이름을 30자 이내로 입력하세요.");
		}
		if (isEmpty(email) || email.length() > 100) {
			throw new Fail("이메일을 입력하세요.");
		}
		if (!email.matches("[^@\\s]+@[^@\\s]+\\.[A-Za-z]{2,}")) {
			throw new Fail("이메일 형식이 올바르지 않습니다.");
		}
		if (isEmpty(phone)) {
			throw new Fail("연락처를 입력하세요.");
		}
		if (!phone.matches("0\\d{1,2}-?\\d{3,4}-?\\d{4}")) {
			throw new Fail("연락처 형식이 올바르지 않습니다. (예: 010-1234-5678)");
		}
	}
}
