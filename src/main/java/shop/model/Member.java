package shop.model;

import java.sql.Timestamp;

/* 회원 (member) — 역할은 USER · ADMIN · DEV, 상태는 ACTIVE · LEFT */
public class Member {
	private int memberId;
	private String loginId, password, name, email, phone, role, status;
	private Timestamp createdAt;

	public int getMemberId() { return memberId; }
	public String getLoginId() { return loginId; }
	public String getPassword() { return password; }
	public String getName() { return name; }
	public String getEmail() { return email; }
	public String getPhone() { return phone; }
	public String getRole() { return role; }
	public String getStatus() { return status; }
	public Timestamp getCreatedAt() { return createdAt; }

	public String getRoleLabel() {
		return "ADMIN".equals(role) ? "관리자" : "DEV".equals(role) ? "개발자" : "회원";
	}

	public String getStatusLabel() {
		return "LEFT".equals(status) ? "탈퇴" : "활동";
	}
}
