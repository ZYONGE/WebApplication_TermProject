package shop.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import shop.dto.AddressDTO;
import shop.util.DBUtil;

/*
  address — 회원 배송지.

  모든 문장에 member_id 조건이 붙는다. addr_id 만으로 수정 · 삭제하면
  남의 주소를 건드릴 수 있다. (schema.sql address 주석)
*/
public class AddressDAO {

	private static final String COLUMNS =
		"addr_id, member_id, addr_name, receiver, receiver_phone, zipcode, addr1, addr2, is_default";

	public List<AddressDTO> findByMember(int memberId) throws SQLException {
		String sql = "SELECT " + COLUMNS + " FROM address WHERE member_id = ?"
				   + " ORDER BY is_default DESC, addr_id DESC";
		List<AddressDTO> list = new ArrayList<AddressDTO>();
		Connection conn = null;
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			conn = DBUtil.getConnection();
			pstmt = conn.prepareStatement(sql);
			pstmt.setInt(1, memberId);
			rs = pstmt.executeQuery();
			while (rs.next()) {
				list.add(map(rs));
			}
			return list;
		} finally {
			DBUtil.close(rs, pstmt, conn);
		}
	}

	/* 주문서 자동입력에 쓰는 기본 배송지 */
	public AddressDTO findDefault(int memberId) throws SQLException {
		String sql = "SELECT " + COLUMNS + " FROM address"
				   + " WHERE member_id = ? AND is_default = 'Y' LIMIT 1";
		Connection conn = null;
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			conn = DBUtil.getConnection();
			pstmt = conn.prepareStatement(sql);
			pstmt.setInt(1, memberId);
			rs = pstmt.executeQuery();
			return rs.next() ? map(rs) : null;
		} finally {
			DBUtil.close(rs, pstmt, conn);
		}
	}

	public AddressDTO findById(int addrId, int memberId) throws SQLException {
		String sql = "SELECT " + COLUMNS + " FROM address WHERE addr_id = ? AND member_id = ?";
		Connection conn = null;
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			conn = DBUtil.getConnection();
			pstmt = conn.prepareStatement(sql);
			pstmt.setInt(1, addrId);
			pstmt.setInt(2, memberId);
			rs = pstmt.executeQuery();
			return rs.next() ? map(rs) : null;
		} finally {
			DBUtil.close(rs, pstmt, conn);
		}
	}

	public int insert(Connection conn, AddressDTO addr) throws SQLException {
		String sql = "INSERT INTO address"
				   + " (member_id, addr_name, receiver, receiver_phone, zipcode, addr1, addr2, is_default)"
				   + " VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
		PreparedStatement pstmt = null;
		try {
			pstmt = conn.prepareStatement(sql);
			pstmt.setInt(1, addr.getMemberId());
			pstmt.setString(2, addr.getAddrName());
			pstmt.setString(3, addr.getReceiver());
			pstmt.setString(4, addr.getReceiverPhone());
			pstmt.setString(5, addr.getZipcode());
			pstmt.setString(6, addr.getAddr1());
			pstmt.setString(7, addr.getAddr2());
			pstmt.setString(8, addr.getIsDefault());
			return pstmt.executeUpdate();
		} finally {
			DBUtil.close(pstmt);
		}
	}

	public int update(Connection conn, AddressDTO addr) throws SQLException {
		String sql = "UPDATE address SET addr_name = ?, receiver = ?, receiver_phone = ?,"
				   + " zipcode = ?, addr1 = ?, addr2 = ?, is_default = ?"
				   + " WHERE addr_id = ? AND member_id = ?";
		PreparedStatement pstmt = null;
		try {
			pstmt = conn.prepareStatement(sql);
			pstmt.setString(1, addr.getAddrName());
			pstmt.setString(2, addr.getReceiver());
			pstmt.setString(3, addr.getReceiverPhone());
			pstmt.setString(4, addr.getZipcode());
			pstmt.setString(5, addr.getAddr1());
			pstmt.setString(6, addr.getAddr2());
			pstmt.setString(7, addr.getIsDefault());
			pstmt.setInt(8, addr.getAddrId());
			pstmt.setInt(9, addr.getMemberId());
			return pstmt.executeUpdate();
		} finally {
			DBUtil.close(pstmt);
		}
	}

	public int delete(int addrId, int memberId) throws SQLException {
		String sql = "DELETE FROM address WHERE addr_id = ? AND member_id = ?";
		Connection conn = null;
		PreparedStatement pstmt = null;
		try {
			conn = DBUtil.getConnection();
			pstmt = conn.prepareStatement(sql);
			pstmt.setInt(1, addrId);
			pstmt.setInt(2, memberId);
			return pstmt.executeUpdate();
		} finally {
			DBUtil.close(pstmt, conn);
		}
	}

	/* 기본 배송지는 회원당 하나다 — 먼저 전부 내리고 하나만 올린다. */
	public int clearDefault(Connection conn, int memberId) throws SQLException {
		String sql = "UPDATE address SET is_default = 'N' WHERE member_id = ?";
		PreparedStatement pstmt = null;
		try {
			pstmt = conn.prepareStatement(sql);
			pstmt.setInt(1, memberId);
			return pstmt.executeUpdate();
		} finally {
			DBUtil.close(pstmt);
		}
	}

	public int markDefault(Connection conn, int addrId, int memberId) throws SQLException {
		String sql = "UPDATE address SET is_default = 'Y' WHERE addr_id = ? AND member_id = ?";
		PreparedStatement pstmt = null;
		try {
			pstmt = conn.prepareStatement(sql);
			pstmt.setInt(1, addrId);
			pstmt.setInt(2, memberId);
			return pstmt.executeUpdate();
		} finally {
			DBUtil.close(pstmt);
		}
	}

	public int countByMember(int memberId) throws SQLException {
		String sql = "SELECT COUNT(*) FROM address WHERE member_id = ?";
		Connection conn = null;
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			conn = DBUtil.getConnection();
			pstmt = conn.prepareStatement(sql);
			pstmt.setInt(1, memberId);
			rs = pstmt.executeQuery();
			return rs.next() ? rs.getInt(1) : 0;
		} finally {
			DBUtil.close(rs, pstmt, conn);
		}
	}

	private AddressDTO map(ResultSet rs) throws SQLException {
		AddressDTO dto = new AddressDTO();
		dto.setAddrId(rs.getInt("addr_id"));
		dto.setMemberId(rs.getInt("member_id"));
		dto.setAddrName(rs.getString("addr_name"));
		dto.setReceiver(rs.getString("receiver"));
		dto.setReceiverPhone(rs.getString("receiver_phone"));
		dto.setZipcode(rs.getString("zipcode"));
		dto.setAddr1(rs.getString("addr1"));
		dto.setAddr2(rs.getString("addr2"));
		dto.setIsDefault(rs.getString("is_default"));
		return dto;
	}
}
