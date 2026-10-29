package shop.controller;

import java.io.IOException;
import java.sql.SQLException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import shop.dao.AdminLogDAO;
import shop.dao.DevDAO;
import shop.service.DevService;
import shop.util.Paging;

/*
  36번 dev 감사로그 조회 (CRUD.md § 6-2)

  이 화면이 생기면서 admin_log.detail 이 처음으로 쓰인다.
  login-as 가 만든 감사 구멍도 여기서 메워진다 — detail 의 impersonated_by 를
  함께 보여주면 실제 행위자를 되찾을 수 있다.

  정렬은 created_at DESC 고정이다. 감사 로그를 최신순 말고 다르게 볼 이유가 없다.
*/
@WebServlet(name = "DevLogServlet", urlPatterns = { "/dev/log" })
public class DevLogServlet extends BaseServlet {

	private static final long serialVersionUID = 1L;

	private final DevService devService = new DevService();

	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		try {
			request.setAttribute("pageTitle", "dev · 감사 로그");
			pageCss(request, "admin");
			request.setAttribute("actions", AdminLogDAO.ACTIONS);
			request.setAttribute("tables", DevDAO.ALLOWED_TABLES);
			request.setAttribute("pageData", devService.findLogs(
					param(request, "mode"), param(request, "action"),
					param(request, "targetTable"), param(request, "fromDate"),
					param(request, "toDate"),
					intParam(request, "page", 1), Paging.DEFAULT_SIZE));

			/* 필터를 유지한 채 페이지만 넘기도록 조건을 링크에 붙여 둔다. */
			StringBuilder url = new StringBuilder("/dev/log?");
			appendParam(url, "mode", param(request, "mode"));
			appendParam(url, "action", param(request, "action"));
			appendParam(url, "targetTable", param(request, "targetTable"));
			appendParam(url, "fromDate", param(request, "fromDate"));
			appendParam(url, "toDate", param(request, "toDate"));
			request.setAttribute("pageUrl", url.toString());

			render(request, response, "/dev/log.jsp");
		} catch (SQLException e) {
			throw wrap(e);
		}
	}

	private void appendParam(StringBuilder url, String name, String value) throws IOException {
		if (value != null && value.length() > 0) {
			url.append(name).append("=")
			   .append(java.net.URLEncoder.encode(value, "UTF-8")).append("&");
		}
	}
}
