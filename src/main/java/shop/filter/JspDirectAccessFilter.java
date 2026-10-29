package shop.filter;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/*
  .jsp 직접 접근 차단.

  왜 필요한가
    CLAUDE.md § 7 이 JSP 위치를 WebContents/{member,product,...}/*.jsp 로 고정했다.
    그 경로는 웹 루트 아래라 브라우저가 /product/view.jsp 를 그대로 열 수 있고,
    그러면 Servlet 을 건너뛰어 모델 없이 렌더되거나 권한 검사를 지나친다.

  어떻게 막는가
    이 필터는 <dispatcher>REQUEST</dispatcher> 로만 매핑한다. Servlet 이
    RequestDispatcher.forward() 로 넘기는 것은 FORWARD 디스패치라 이 필터를 타지 않는다.
    즉 "브라우저가 직접 친 .jsp" 만 걸러진다.

  ★ 이 판단은 샌드박스에서 내린 것이다. 대안은 뷰를 /WEB-INF/views/ 로 옮기는
    표준 방식인데, 그러면 확정된 디렉터리 구조를 바꿔야 한다. 구조를 지키면서
    같은 보호를 얻는 쪽을 골랐다 — 트랙 문서 § 8 에 올려 둔다.
*/
public class JspDirectAccessFilter implements Filter {

	public void init(FilterConfig config) throws ServletException {
	}

	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
			throws IOException, ServletException {
		HttpServletRequest req = (HttpServletRequest) request;
		HttpServletResponse res = (HttpServletResponse) response;

		String uri = req.getRequestURI();
		if (uri != null && uri.endsWith(".jsp")) {
			/* 메인 진입은 welcome-file 로도 들어오므로 예외로 둔다. */
			String context = req.getContextPath();
			if (!uri.equals(context + "/index.jsp")) {
				res.sendRedirect(context + "/");
				return;
			}
		}
		chain.doFilter(request, response);
	}

	public void destroy() {
	}
}
