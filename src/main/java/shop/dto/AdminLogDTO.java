package shop.dto;

import java.sql.Timestamp;

/* 관리자·dev 조작 기록 */
public class AdminLogDTO {

	private int logId;
	private int memberId;
	private String memberName;
	private String mode;
	private String action;
	private String targetTable;
	private Integer targetId;
	private String detail;
	private Timestamp createdAt;


	public int getLogId() {
		return logId;
	}

	public void setLogId(int logId) {
		this.logId = logId;
	}

	public int getMemberId() {
		return memberId;
	}

	public void setMemberId(int memberId) {
		this.memberId = memberId;
	}

	public String getMemberName() {
		return memberName;
	}

	public void setMemberName(String memberName) {
		this.memberName = memberName;
	}

	public String getMode() {
		return mode;
	}

	public void setMode(String mode) {
		this.mode = mode;
	}

	public String getAction() {
		return action;
	}

	public void setAction(String action) {
		this.action = action;
	}

	public String getTargetTable() {
		return targetTable;
	}

	public void setTargetTable(String targetTable) {
		this.targetTable = targetTable;
	}

	public Integer getTargetId() {
		return targetId;
	}

	public void setTargetId(Integer targetId) {
		this.targetId = targetId;
	}

	public String getDetail() {
		return detail;
	}

	public void setDetail(String detail) {
		this.detail = detail;
	}

	public Timestamp getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(Timestamp createdAt) {
		this.createdAt = createdAt;
	}
}
