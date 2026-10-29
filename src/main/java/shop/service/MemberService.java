package shop.service;

import java.sql.SQLException;

import shop.dao.MemberDAO;
import shop.dto.MemberDTO;
import shop.dto.PageDTO;
import shop.util.StringUtil;

/*
  회원 — 가입 · 로그인 · 정보수정 · 탈퇴.

  ★ 검증 규칙은 WebContents/js/validate.js 와 같아야 한다.
    클라이언트 검증은 거들 뿐이고, 판정의 근거는 항상 이쪽이다.
    규칙을 고치면 두 곳을 같이 고친다 (CLAUDE.md § 4.1 JS 분담).

  ★ 비밀번호를 평문으로 다룬다. 샌드박스 범위에서 해시를 넣지 않은 이유는
    data.sql 의 더미 계정이 평문이고 /dev 계정 전환이 그 전제 위에 서 있기 때문이다.
    실제 제출물에서는 해시가 필요하다 — 트랙 문서 § 8 에 올려 두었다.
*/
public class MemberService {

	/* validate.js 와 공유하는 규칙 */
	public static final int LOGIN_ID_MIN = 4;
	public static final int LOGIN_ID_MAX = 30;
	public static final int PASSWORD_MIN = 8;
	public static final int PASSWORD_MAX = 100;

	private final MemberDAO memberDAO = new MemberDAO();

	/* ---------------- 로그인 ---------------- */

	public MemberDTO login(String loginId, String password) throws SQLException {
		if (StringUtil.isEmpty(loginId) || StringUtil.isEmpty(password)) {
			throw new ServiceException("아이디와 비밀번호를 입력하세요.");
		}
		MemberDTO member = memberDAO.findByLoginId(loginId.trim());
		if (member == null || !password.equals(member.getPassword())) {
			/* 아이디 없음과 비밀번호 불일치를 구분해 알리지 않는다. */
			throw new ServiceException("아이디 또는 비밀번호가 올바르지 않습니다.");
		}
		if ("LEFT".equals(member.getStatus())) {
			throw new ServiceException("탈퇴한 계정입니다.");
		}
		return member;
	}

	/* ---------------- 가입 ---------------- */

	public int join(MemberDTO member, String passwordConfirm) throws SQLException {
		validateLoginId(member.getLoginId());
		validatePassword(member.getPassword(), passwordConfirm);
		validateName(member.getName());
		validateEmail(member.getEmail());
		validatePhone(member.getPhone());

		if (memberDAO.existsLoginId(member.getLoginId())) {
			throw new ServiceException("이미 사용 중인 아이디입니다.");
		}
		if (memberDAO.existsEmail(member.getEmail())) {
			throw new ServiceException("이미 가입된 이메일입니다.");
		}
		return memberDAO.insert(member);
	}

	public boolean isLoginIdAvailable(String loginId) throws SQLException {
		if (StringUtil.isEmpty(loginId) || loginId.trim().length() < LOGIN_ID_MIN) {
			return false;
		}
		return !memberDAO.existsLoginId(loginId.trim());
	}

	/* ---------------- 정보 수정 · 탈퇴 ---------------- */

	public void updateProfile(MemberDTO member) throws SQLException {
		validateName(member.getName());
		validateEmail(member.getEmail());
		validatePhone(member.getPhone());
		if (memberDAO.existsEmailExcept(member.getEmail(), member.getMemberId())) {
			throw new ServiceException("다른 회원이 사용 중인 이메일입니다.");
		}
		memberDAO.update(member);
	}

	public void changePassword(int memberId, String current, String next, String confirm)
			throws SQLException {
		MemberDTO member = memberDAO.findById(memberId);
		if (member == null || !member.getPassword().equals(current)) {
			throw new ServiceException("현재 비밀번호가 올바르지 않습니다.");
		}
		validatePassword(next, confirm);
		memberDAO.updatePassword(memberId, next);
	}

	/* 탈퇴 — 소프트 삭제. 주문 · 리뷰가 FK 로 묶여 물리 삭제가 불가능하다. */
	public void leave(int memberId, String password) throws SQLException {
		MemberDTO member = memberDAO.findById(memberId);
		if (member == null || !member.getPassword().equals(password)) {
			throw new ServiceException("비밀번호가 올바르지 않습니다.");
		}
		if ("DEV".equals(member.getRole())) {
			throw new ServiceException("DEV 계정은 탈퇴할 수 없습니다.");
		}
		memberDAO.leave(memberId);
	}

	public MemberDTO findById(int memberId) throws SQLException {
		return memberDAO.findById(memberId);
	}

	public PageDTO<MemberDTO> findPage(String keyword, String role, int page, int size)
			throws SQLException {
		return memberDAO.findPage(keyword, role, page, size);
	}

	/* ---------------- 검증 ---------------- */

	public void validateLoginId(String loginId) {
		if (StringUtil.isEmpty(loginId)) {
			throw new ServiceException("아이디를 입력하세요.");
		}
		String value = loginId.trim();
		if (value.length() < LOGIN_ID_MIN || value.length() > LOGIN_ID_MAX) {
			throw new ServiceException("아이디는 " + LOGIN_ID_MIN + "~" + LOGIN_ID_MAX
					+ "자로 입력하세요.");
		}
		if (!value.matches("[A-Za-z0-9_]+")) {
			throw new ServiceException("아이디는 영문 · 숫자 · 밑줄만 쓸 수 있습니다.");
		}
	}

	public void validatePassword(String password, String confirm) {
		if (StringUtil.isEmpty(password)) {
			throw new ServiceException("비밀번호를 입력하세요.");
		}
		if (password.length() < PASSWORD_MIN || password.length() > PASSWORD_MAX) {
			throw new ServiceException("비밀번호는 " + PASSWORD_MIN + "자 이상으로 입력하세요.");
		}
		if (confirm != null && !password.equals(confirm)) {
			throw new ServiceException("비밀번호 확인이 일치하지 않습니다.");
		}
	}

	public void validateName(String name) {
		if (StringUtil.isEmpty(name) || name.trim().length() > 30) {
			throw new ServiceException("이름을 30자 이내로 입력하세요.");
		}
	}

	public void validateEmail(String email) {
		if (StringUtil.isEmpty(email) || email.trim().length() > 100) {
			throw new ServiceException("이메일을 입력하세요.");
		}
		if (!email.trim().matches("[^@\\s]+@[^@\\s]+\\.[A-Za-z]{2,}")) {
			throw new ServiceException("이메일 형식이 올바르지 않습니다.");
		}
	}

	public void validatePhone(String phone) {
		if (StringUtil.isEmpty(phone)) {
			throw new ServiceException("연락처를 입력하세요.");
		}
		if (!phone.trim().matches("0\\d{1,2}-?\\d{3,4}-?\\d{4}")) {
			throw new ServiceException("연락처 형식이 올바르지 않습니다. (예: 010-1234-5678)");
		}
	}
}
