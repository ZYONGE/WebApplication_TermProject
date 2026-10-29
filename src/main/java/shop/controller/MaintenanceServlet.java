package shop.controller;

import java.io.IOException;
import java.sql.SQLException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import shop.service.ConfigService;

/*
  21번 점검 안내.

  ShopFilter 가 여기로 보낸다. 점검이 이미 풀렸는데 이 주소를 들고 있는
  경우를 위해 상태를 한 번 더 확인하고, 풀렸으면 메인으로 돌려보낸다.
*/
@WebServlet(name = "MaintenanceServlet", urlPatterns = { "/maintenance" })
public class MaintenanceServlet extends BaseServlet {

	private static final long serialVersionUID = 1L;

	private final ConfigService configService = new ConfigService();

	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		try {
			if (!configService.isMaintenance()) {
				redirect(request, response, "/");
				return;
			}
			request.setAttribute("pageTitle", "서비스 점검 중");
			render(request, response, "/maintenance.jsp");
		} catch (SQLException e) {
			throw wrap(e);
		}
	}
}
