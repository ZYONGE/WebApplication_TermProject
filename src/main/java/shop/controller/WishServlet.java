package shop.controller;

import java.io.IOException;
import java.sql.SQLException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import shop.service.MypageService;

/*
  찜 토글 — 상품상세와 찜목록이 함께 쓴다.

  토글이라 add / remove 가 따로 없다 (CRUD.md § 4 wishlist 행).
  화면 없이 상태만 바꾸고 보낸 쪽으로 돌려보낸다.
*/
@WebServlet(name = "WishServlet", urlPatterns = { "/wish/toggle", "/wish/remove" })
public class WishServlet extends BaseServlet {

	private static final long serialVersionUID = 1L;

	private final MypageService mypageService = new MypageService();

	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		int memberId = loginMemberId(request);
		int productId = intParam(request, "productId", 0);
		String action = action(request);

		try {
			if ("remove".equals(action)) {
				mypageService.removeWish(memberId, productId);
				flash(request, "찜을 해제했습니다.");
			} else {
				boolean wished = mypageService.toggleWish(memberId, productId);
				flash(request, wished ? "찜했습니다." : "찜을 해제했습니다.");
			}
		} catch (SQLException e) {
			throw wrap(e);
		}

		String back = param(request, "back");
		redirect(request, response,
				back != null && back.startsWith("/") ? back : "/mypage/wishlist");
	}
}
