package shop.controller;

import java.io.IOException;
import java.sql.SQLException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import shop.service.DevService;

/*
  32번 dev 설계 보기 (CRUD.md § 3)

  업무 테이블을 전혀 읽지 않는다. information_schema 만 조회한다.
  DATABASE() 를 쓰므로 스키마명을 받을 필요가 없고, 인젝션 표면도 생기지 않는다
  (ERD § 6.6).

  ★ Mermaid 렌더링 탭은 넣지 않았다. mermaid.min.js 가 저장소에 없고
    CDN 로드는 금지돼 있다 (CLAUDE.md § 5 · ERD § 8 쟁점 8).
    대신 실제 스키마 조회 탭만 구현했다 — 트랙 문서 § 7 로 파일 반입을 요청한다.
*/
@WebServlet(name = "DevErdServlet", urlPatterns = { "/dev/erd" })
public class DevErdServlet extends BaseServlet {

	private static final long serialVersionUID = 1L;

	private final DevService devService = new DevService();

	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		try {
			request.setAttribute("pageTitle", "dev · 설계 보기");
			pageCss(request, "admin");
			request.setAttribute("columns", devService.findSchemaColumns());
			request.setAttribute("constraints", devService.findSchemaConstraints());
			request.setAttribute("checks", devService.findSchemaChecks());
			render(request, response, "/dev/erd.jsp");
		} catch (SQLException e) {
			throw wrap(e);
		}
	}
}
