package shop.dao;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import shop.model.Address;
import shop.model.Member;
import shop.model.Page;

/* 회원 · 배송지 (member, address) */
public class MemberDAO {

	public static Member byLoginId(String loginId) throws SQLException {
		return DB.one(Member.class, "SELECT * FROM member WHERE login_id = ?", loginId);
	}

	public static Member byId(int memberId) throws SQLException {
		return DB.one(Member.class, "SELECT * FROM member WHERE member_id = ?", memberId);
	}

	public static boolean exists(String column, String value, int exceptId) throws SQLException {
		return DB.number("SELECT COUNT(*) FROM member WHERE " + column + " = ? AND member_id <> ?", value, exceptId) > 0;
	}

	public static void insert(String loginId, String password, String name, String email, String phone)
			throws SQLException {
		DB.update("INSERT INTO member (login_id, password, name, email, phone, role, status)"
				+ " VALUES (?, ?, ?, ?, ?, 'USER', 'ACTIVE')", loginId, password, name, email, phone);
	}

	public static void update(int memberId, String name, String email, String phone) throws SQLException {
		DB.update("UPDATE member SET name = ?, email = ?, phone = ? WHERE member_id = ?", name, email, phone, memberId);
	}

	public static void updatePassword(int memberId, String password) throws SQLException {
		DB.update("UPDATE member SET password = ? WHERE member_id = ?", password, memberId);
	}

	/* 탈퇴 — onlyUser 면 일반 회원만 (관리자 처리) */
	public static int leave(int memberId, boolean onlyUser) throws SQLException {
		return DB.update("UPDATE member SET status = 'LEFT' WHERE member_id = ? AND status = 'ACTIVE'"
				+ (onlyUser ? " AND role = 'USER'" : ""), memberId);
	}

	/* 회원 목록 (관리자) — 아이디 · 이름 · 이메일 검색, 역할 필터 */
	public static Page<Member> page(String keyword, String role, int page, int size) throws SQLException {
		StringBuilder where = new StringBuilder("FROM member WHERE 1 = 1");
		List<Object> args = new ArrayList<Object>();
		if (keyword != null && keyword.length() > 0) {
			where.append(" AND (login_id LIKE ? OR name LIKE ? OR email LIKE ?)");
			String like = ProductDAO.like(keyword);
			args.add(like);
			args.add(like);
			args.add(like);
		}
		if ("USER".equals(role) || "ADMIN".equals(role) || "DEV".equals(role)) {
			where.append(" AND role = ?");
			args.add(role);
		}
		return DB.page(Member.class, "SELECT *", where.toString(), "ORDER BY member_id DESC", page, size, args);
	}

	/* ---------- 배송지 ---------- */

	/* 기본 배송지가 맨 위, 나머지는 최근 것부터 */
	public static List<Address> addresses(int memberId) throws SQLException {
		return DB.list(Address.class, "SELECT * FROM address WHERE member_id = ?"
				+ " ORDER BY (is_default = 'Y') DESC, addr_id DESC", memberId);
	}

	public static Address defaultAddress(int memberId) throws SQLException {
		return DB.one(Address.class, "SELECT * FROM address WHERE member_id = ? AND is_default = 'Y' LIMIT 1", memberId);
	}

	public static Address address(int addrId, int memberId) throws SQLException {
		return DB.one(Address.class, "SELECT * FROM address WHERE addr_id = ? AND member_id = ?", addrId, memberId);
	}

	/* 저장 — 기본으로 지정하면 다른 배송지의 기본 표시를 먼저 지운다. 고칠 배송지가 없으면 false */
	public static boolean saveAddress(int addrId, int memberId, String addrName, String receiver, String receiverPhone,
			String zipcode, String addr1, String addr2, boolean asDefault) throws SQLException {
		DB.begin();
		try {
			if (asDefault) {
				DB.update("UPDATE address SET is_default = 'N' WHERE member_id = ?", memberId);
			}
			String flag = asDefault ? "Y" : "N";
			int n;
			if (addrId > 0) {
				n = DB.update("UPDATE address SET addr_name = ?, receiver = ?, receiver_phone = ?, zipcode = ?,"
						+ " addr1 = ?, addr2 = ?, is_default = ? WHERE addr_id = ? AND member_id = ?",
						addrName, receiver, receiverPhone, zipcode, addr1, addr2, flag, addrId, memberId);
			} else {
				n = DB.update("INSERT INTO address (member_id, addr_name, receiver, receiver_phone, zipcode, addr1, addr2,"
						+ " is_default) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
						memberId, addrName, receiver, receiverPhone, zipcode, addr1, addr2, flag);
			}
			if (n > 0) {
				DB.commit();
			}
			return n > 0;
		} finally {
			DB.end();
		}
	}

	/* 기본 배송지 바꾸기 — 대상이 없으면 false (아무것도 바꾸지 않음) */
	public static boolean setDefault(int addrId, int memberId) throws SQLException {
		DB.begin();
		try {
			DB.update("UPDATE address SET is_default = 'N' WHERE member_id = ?", memberId);
			if (DB.update("UPDATE address SET is_default = 'Y' WHERE addr_id = ? AND member_id = ?", addrId, memberId) == 0) {
				return false;
			}
			DB.commit();
			return true;
		} finally {
			DB.end();
		}
	}

	/* 삭제 — 기본 배송지를 지웠으면 가장 최근 배송지를 기본으로 */
	public static void deleteAddress(Address target) throws SQLException {
		DB.begin();
		try {
			DB.update("DELETE FROM address WHERE addr_id = ? AND member_id = ?", target.getAddrId(), target.getMemberId());
			if ("Y".equals(target.getIsDefault())) {
				DB.update("UPDATE address SET is_default = 'Y' WHERE member_id = ? ORDER BY addr_id DESC LIMIT 1",
						target.getMemberId());
			}
			DB.commit();
		} finally {
			DB.end();
		}
	}
}
