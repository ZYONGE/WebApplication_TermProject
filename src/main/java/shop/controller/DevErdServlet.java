package shop.controller;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.sql.SQLException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import shop.service.DevService;
import shop.util.MermaidErd;

/*
  32번 dev 설계 보기

      GET /dev/erd               ERD 그림 + 실제 스키마 조회(information_schema)
      GET /dev/erd/diagram.svg   README 의 mermaid ERD 를 SVG 이미지로

  ★ ERD 그림은 mermaid.js 없이 서버에서 그린다(MermaidErd). CDN 로드는 금지돼 있고
    라이브러리 파일도 저장소에 없다. 결과가 SVG 파일이라 <img> 로 붙고 새 탭으로 열어 저장할 수 있다.
  ★ 원본은 README.md 의 ```mermaid 블록이다. README 는 웹앱(WebContents) 밖에 있어서
    Eclipse 배포에서는 읽히지 않는다 — 그때는 같은 내용의 사본(shop/util/erd.mmd)을 쓰고,
    화면에 어느 쪽을 썼는지 표시한다.
*/
@WebServlet(name = "DevErdServlet", urlPatterns = { "/dev/erd", "/dev/erd/diagram.svg" })
public class DevErdServlet extends BaseServlet {

	private static final long serialVersionUID = 1L;

	private static final String FALLBACK = "/shop/util/erd.mmd";

	private final DevService devService = new DevService();

	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		if (request.getServletPath().endsWith(".svg")) {
			diagram(response);
			return;
		}
		try {
			request.setAttribute("pageTitle", "dev · 설계 보기");
			pageCss(request, "admin");

			String[] source = loadSource();
			request.setAttribute("erdOrigin", source[1]);
			if (source[0] != null) {
				MermaidErd erd = MermaidErd.parse(source[0]);
				request.setAttribute("erdEntityCount", Integer.valueOf(erd.entityCount()));
				request.setAttribute("erdRelationCount", Integer.valueOf(erd.relationCount()));
				/* 페이지에는 화면 폭에 맞춘 SVG 를 직접 넣는다. 원본 크기는 diagram.svg 로 연다. */
				request.setAttribute("erdSvg", erd.toSvg(true));
			}

			request.setAttribute("columns", devService.findSchemaColumns());
			request.setAttribute("constraints", devService.findSchemaConstraints());
			request.setAttribute("checks", devService.findSchemaChecks());
			render(request, response, "/dev/erd.jsp");
		} catch (SQLException e) {
			throw wrap(e);
		}
	}

	private void diagram(HttpServletResponse response) throws IOException {
		String[] source = loadSource();
		if (source[0] == null) {
			response.sendError(HttpServletResponse.SC_NOT_FOUND, "ERD 원본(mermaid)을 찾지 못했습니다.");
			return;
		}
		byte[] svg = MermaidErd.parse(source[0]).toSvg().getBytes(StandardCharsets.UTF_8);
		response.setContentType("image/svg+xml; charset=UTF-8");
		/* README 를 고치면 다음 요청에 바로 반영되도록 캐시하지 않는다 */
		response.setHeader("Cache-Control", "no-cache");
		response.setContentLength(svg.length);
		response.getOutputStream().write(svg);
	}

	/* { mermaid 원문, 원본 설명 }. 둘 다 못 찾으면 원문은 null. */
	private String[] loadSource() throws IOException {
		String webRoot = getServletContext().getRealPath("/");
		if (webRoot != null) {
			File readme = new File(new File(webRoot).getParentFile(), "README.md");
			if (readme.isFile()) {
				String block = MermaidErd.extractBlock(
						new String(Files.readAllBytes(readme.toPath()), StandardCharsets.UTF_8));
				if (block != null) {
					return new String[] { block, "README.md" };
				}
			}
		}

		InputStream in = getClass().getResourceAsStream(FALLBACK);
		if (in == null) {
			return new String[] { null, "없음" };
		}
		try {
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			byte[] buf = new byte[4096];
			int n;
			while ((n = in.read(buf)) > 0) {
				out.write(buf, 0, n);
			}
			return new String[] { new String(out.toByteArray(), StandardCharsets.UTF_8),
					"사본 shop/util/erd.mmd (README 를 읽을 수 없는 배포 환경)" };
		} finally {
			in.close();
		}
	}
}
