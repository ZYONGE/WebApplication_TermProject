package shop.controller;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/*
  LLM 기능의 JSON 엔드포인트. js/llm.js 가 상품 화면을 그린 뒤 비동기로 부른다.
  응답이 최대 180초라 페이지 렌더 안에서 부르지 않는다. (docs/tracks/LLM.md § 5)

    GET /llm/summary?type=desc&productId=    F1 상품 설명 요약
    GET /llm/summary?type=review&productId=  F2 리뷰 요약
    GET /llm/related?keyword=                F3 연관 검색

  /llm/* 와일드카드는 쓰지 않는다 (BaseServlet.action 주석 참고).
  구현 전에는 501 과 안내 JSON 을 돌려준다 — js/llm.js 는 실패로 보고 자리를 숨긴다.
*/
@WebServlet(name = "LlmServlet", urlPatterns = { "/llm/summary", "/llm/related" })
public class LlmServlet extends BaseServlet {

	private static final long serialVersionUID = 1L;

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		response.setStatus(HttpServletResponse.SC_NOT_IMPLEMENTED);
		response.setContentType("application/json; charset=UTF-8");
		response.getWriter().write("{\"ok\":false,\"message\":\"준비 중인 기능입니다.\"}");
	}
}
