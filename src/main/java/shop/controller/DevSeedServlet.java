package shop.controller;

import java.io.IOException;
import java.sql.SQLException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import shop.dao.DevDAO;
import shop.service.DevService;
import shop.service.ServiceException;

/*
  33번 dev 데이터 관리 (CRUD.md § 3)

  ★ member 를 통째로 비우지 않는다. DEV 계정이 사라지면 /dev 재진입 경로가 없어지고,
    ADMIN 은 DEV 권한을 줄 수 없어서 복구 수단이 사라진다 (ERD § 6.7).
    전체 초기화는 role = 'USER' 인 회원만 지운다.
*/
@WebServlet(name = "DevSeedServlet", urlPatterns = { "/dev/seed" })
public class DevSeedServlet extends BaseServlet {

	private static final long serialVersionUID = 1L;

	private final DevService devService = new DevService();

	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		request.setAttribute("pageTitle", "dev · 데이터 관리");
		pageCss(request, "admin");
		request.setAttribute("tables", DevDAO.ALLOWED_TABLES);
		render(request, response, "/dev/seed.jsp");
	}

	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		int devId = loginMemberId(request);
		String mode = param(request, "mode", "");
		try {
			if ("truncateAll".equals(mode)) {
				devService.truncateAll(devId);
				flash(request, "전체 초기화했습니다. (ADMIN · DEV 계정은 남았습니다)");
			} else if ("truncateTable".equals(mode)) {
				String table = param(request, "table");
				devService.truncateTable(devId, table);
				flash(request, table + " 테이블을 비웠습니다.");
			} else if ("generate".equals(mode)) {
				int count = devService.generateProducts(devId, intParam(request, "count", 10));
				flash(request, "더미 상품 " + count + "건을 생성했습니다.");
			}
		} catch (ServiceException e) {
			flashError(request, e.getMessage());
		} catch (SQLException e) {
			throw wrap(e);
		}
		redirect(request, response, "/dev/seed");
	}
}
