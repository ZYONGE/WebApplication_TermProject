package shop.controller;

import java.io.IOException;
import java.sql.SQLException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import shop.service.ConfigService;

/* 29번 dev 라우팅 허브 (CRUD.md § 3) — 나머지 dev 화면으로 가는 입구다. */
@WebServlet(name = "DevHubServlet", urlPatterns = { "/dev", "/dev/" })
public class DevHubServlet extends BaseServlet {

	private static final long serialVersionUID = 1L;

	private final ConfigService configService = new ConfigService();

	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		try {
			request.setAttribute("pageTitle", "dev");
			pageCss(request, "admin");
			request.setAttribute("configs", configService.findAll());
			render(request, response, "/dev/index.jsp");
		} catch (SQLException e) {
			throw wrap(e);
		}
	}
}
