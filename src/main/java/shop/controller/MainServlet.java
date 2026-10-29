package shop.controller;

import java.io.IOException;
import java.sql.SQLException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import shop.service.NoticeService;
import shop.service.ProductService;

/* 1번 메인 — 공지 + 신상품 (CRUD.md § 2) */
/*
  "" 는 컨텍스트 루트 정확 일치 매핑이다 (Servlet 4.0).
  이것으로 welcome-file 없이 / 요청이 바로 이 서블릿에 온다.
*/
@WebServlet(name = "MainServlet", urlPatterns = { "", "/main" })
public class MainServlet extends BaseServlet {

	private static final long serialVersionUID = 1L;

	private final ProductService productService = new ProductService();
	private final NoticeService noticeService = new NoticeService();

	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		try {
			request.setAttribute("pageTitle", "SHOP");
			pageCss(request, "main");
			request.setAttribute("notices", noticeService.findLatest(3));
			request.setAttribute("newProducts", productService.findLatest(8));
			render(request, response, "/main.jsp");
		} catch (SQLException e) {
			throw wrap(e);
		}
	}
}
