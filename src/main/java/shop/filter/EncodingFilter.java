package shop.filter;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

/*
  요청 · 응답 인코딩을 UTF-8 로 고정한다.

  체인에서 가장 먼저 와야 한다. 다른 필터나 서블릿이 getParameter() 를 한 번이라도
  호출하면 그 시점에 인코딩이 확정돼 버려, 뒤늦게 바꿔도 한글이 이미 깨져 있다.
*/
public class EncodingFilter implements Filter {

	private String encoding = "UTF-8";

	public void init(FilterConfig config) throws ServletException {
		String value = config.getInitParameter("encoding");
		if (value != null && value.length() > 0) {
			this.encoding = value;
		}
	}

	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
			throws IOException, ServletException {
		request.setCharacterEncoding(encoding);
		response.setCharacterEncoding(encoding);
		chain.doFilter(request, response);
	}

	public void destroy() {
	}
}
