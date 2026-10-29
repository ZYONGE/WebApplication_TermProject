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
import shop.util.Paging;
import shop.util.StringUtil;

/*
  31번 dev 데이터 브라우저 (CRUD.md § 3)

  ★ 17개 테이블 전부를 R 하는 유일한 화면이다. 테이블 이름은 ? 로 바인딩되지 않으므로
    허용 목록(DevDAO.ALLOWED_TABLES)을 통과한 이름만 SQL 에 들어간다 (ERD § 6.5).
    정렬 컬럼도 같다 — 실제 컬럼 목록에 있는 이름만 ORDER BY 에 붙는다.
*/
@WebServlet(name = "DevDataServlet", urlPatterns = { "/dev/data" })
public class DevDataServlet extends BaseServlet {

	private static final long serialVersionUID = 1L;

	private final DevService devService = new DevService();

	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		String table = param(request, "table");
		request.setAttribute("pageTitle", "dev · 데이터 브라우저");
		pageCss(request, "admin");
		request.setAttribute("tables", DevDAO.ALLOWED_TABLES);

		try {
			if (!StringUtil.isEmpty(table)) {
				request.setAttribute("result", devService.browse(table,
						param(request, "sort"), "desc".equals(param(request, "dir")),
						intParam(request, "page", 1), Paging.normalizeSize(
								intParam(request, "size", 20))));
			}
			render(request, response, "/dev/data.jsp");
		} catch (ServiceException e) {
			request.setAttribute("errorMessage", e.getMessage());
			render(request, response, "/dev/data.jsp");
		} catch (SQLException e) {
			throw wrap(e);
		}
	}
}
