package shop.service;

import java.io.IOException;
import java.sql.SQLException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import shop.dao.BoardDAO;
import shop.dao.ConfigDAO;
import shop.dao.ProductDAO;
import shop.model.Notice;

/* 메인 · 점검 안내 · 공지사항 ("" 는 사이트 첫 주소) */
@WebServlet({ "", "/main", "/maintenance", "/notice/list", "/notice/view" })
public class MainServlet extends BaseServlet {

	private static final long serialVersionUID = 1L;

	@Override
	protected void get(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException, SQLException {
		switch (request.getServletPath()) {
		case "/maintenance":
			if (!ConfigDAO.isMaintenance()) {
				redirect(request, response, "/");
				return;
			}
			render(request, response, "main/maintenance", "서비스 점검 중");
			break;
		case "/notice/list":
			request.setAttribute("pageData", BoardDAO.notices(param(request, "keyword"), intParam(request, "page", 1)));
			request.setAttribute("pageUrl", "/notice/list?");
			render(request, response, "notice/list", "공지사항");
			break;
		case "/notice/view":
			int noticeId = intParam(request, "noticeId", 0);
			BoardDAO.addNoticeView(noticeId);
			Notice notice = BoardDAO.notice(noticeId);
			if (notice == null) {
				flashError(request, "공지를 찾을 수 없습니다.");
				redirect(request, response, "/notice/list");
				return;
			}
			request.setAttribute("notice", notice);
			render(request, response, "notice/view", notice.getTitle());
			break;
		default:
			request.setAttribute("notices", BoardDAO.latestNotices(3));
			request.setAttribute("tracks", ProductDAO.latest("TRACK", 4));
			request.setAttribute("newProducts", ProductDAO.latest("SINGLE", 8));
			render(request, response, "main/index", "윤라댄");
		}
	}
}
