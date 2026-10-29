package shop.controller;

import java.io.IOException;
import java.sql.SQLException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import shop.dto.NoticeDTO;
import shop.service.NoticeService;
import shop.util.Paging;

/* 19 · 20번 공지목록 · 공지상세 (CRUD.md § 2) */
@WebServlet(name = "NoticeServlet", urlPatterns = { "/notice/list", "/notice/view" })
public class NoticeServlet extends BaseServlet {

	private static final long serialVersionUID = 1L;

	private final NoticeService noticeService = new NoticeService();

	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		String action = action(request);
		pageCss(request, "main");
		try {
			if ("view".equals(action)) {
				int noticeId = intParam(request, "noticeId", 0);
				NoticeDTO notice = noticeService.findDetail(noticeId);
				if (notice == null) {
					flashError(request, "공지를 찾을 수 없습니다.");
					redirect(request, response, "/notice/list");
					return;
				}
				request.setAttribute("pageTitle", notice.getTitle());
				request.setAttribute("notice", notice);
				render(request, response, "/notice/view.jsp");
			} else {
				String keyword = param(request, "keyword");
				request.setAttribute("pageTitle", "공지사항");
				request.setAttribute("pageData",
						noticeService.findPage(keyword, intParam(request, "page", 1),
								Paging.DEFAULT_SIZE));
				request.setAttribute("pageUrl", "/notice/list?");
				render(request, response, "/notice/list.jsp");
			}
		} catch (SQLException e) {
			throw wrap(e);
		}
	}
}
