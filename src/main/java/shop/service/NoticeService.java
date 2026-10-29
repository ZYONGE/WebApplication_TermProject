package shop.service;

import java.sql.SQLException;
import java.util.List;

import shop.dao.NoticeDAO;
import shop.dto.NoticeDTO;
import shop.dto.PageDTO;

/* 공지사항 — 고객 조회 쪽. 등록 · 수정 · 삭제는 AdminService 가 맡는다. */
public class NoticeService {

	private final NoticeDAO noticeDAO = new NoticeDAO();

	public List<NoticeDTO> findLatest(int limit) throws SQLException {
		return noticeDAO.findLatest(limit);
	}

	public PageDTO<NoticeDTO> findPage(String keyword, int page, int size) throws SQLException {
		return noticeDAO.findPage(keyword, page, size);
	}

	/*
	  상세 조회 — 조회수를 1 올린다.

	  새로고침마다 올라간다. 중복 방지(세션 · 쿠키)는 넣지 않았다 — 공지 조회수는
	  정확성이 요구되는 값이 아니고, 넣으면 그 자체가 또 하나의 상태가 된다.
	*/
	public NoticeDTO findDetail(int noticeId) throws SQLException {
		NoticeDTO notice = noticeDAO.findById(noticeId);
		if (notice == null) {
			return null;
		}
		noticeDAO.increaseViewCount(noticeId);
		notice.setViewCount(notice.getViewCount() + 1);
		return notice;
	}
}
