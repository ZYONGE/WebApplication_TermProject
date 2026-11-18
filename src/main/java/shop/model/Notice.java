package shop.model;

import java.sql.Timestamp;

/* 공지 (notice) */
public class Notice {
	private int noticeId, viewCount;
	private String title, content;
	private Timestamp createdAt;

	public int getNoticeId() { return noticeId; }
	public int getViewCount() { return viewCount; }
	public String getTitle() { return title; }
	public String getContent() { return content; }
	public Timestamp getCreatedAt() { return createdAt; }
}
