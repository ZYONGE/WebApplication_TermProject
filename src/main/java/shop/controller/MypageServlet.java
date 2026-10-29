package shop.controller;

import java.io.IOException;
import java.sql.SQLException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import shop.dto.AddressDTO;
import shop.dto.MemberDTO;
import shop.service.MemberService;
import shop.service.MypageService;
import shop.service.OrderService;
import shop.service.ServiceException;
import shop.util.Paging;

/*
  13~18번 마이페이지 (CRUD.md § 2)
      /mypage/          요약
      /mypage/edit      정보수정 · 탈퇴
      /mypage/address   배송지 관리
      /mypage/wishlist  찜 목록
      /mypage/review    내 리뷰
      /mypage/qna       내 문의

  ★ 모든 수정 · 삭제는 서비스 계층에서 member_id 를 조건에 넣는다.
    화면이 넘긴 id 만 믿고 처리하면 남의 글을 건드릴 수 있다.
*/
@WebServlet(name = "MypageServlet", urlPatterns = { "/mypage", "/mypage/", "/mypage/edit", "/mypage/password", "/mypage/leave", "/mypage/address", "/mypage/wishlist", "/mypage/review", "/mypage/qna" })
public class MypageServlet extends BaseServlet {

	private static final long serialVersionUID = 1L;

	private final MypageService mypageService = new MypageService();
	private final MemberService memberService = new MemberService();
	private final OrderService orderService = new OrderService();

	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		String action = action(request);
		int memberId = loginMemberId(request);
		pageCss(request, "mypage");

		try {
			if ("edit".equals(action)) {
				request.setAttribute("pageTitle", "정보 수정");
				request.setAttribute("member", memberService.findById(memberId));
				render(request, response, "/mypage/edit.jsp");
			} else if ("address".equals(action)) {
				request.setAttribute("pageTitle", "배송지 관리");
				request.setAttribute("addresses", mypageService.findAddresses(memberId));
				render(request, response, "/mypage/address.jsp");
			} else if ("wishlist".equals(action)) {
				request.setAttribute("pageTitle", "찜 목록");
				request.setAttribute("items", mypageService.findWishlist(memberId));
				render(request, response, "/mypage/wishlist.jsp");
			} else if ("review".equals(action)) {
				request.setAttribute("pageTitle", "내 리뷰");
				request.setAttribute("reviews", mypageService.findMyReviews(memberId));
				/* 아직 리뷰를 쓰지 않은 배송완료 건을 함께 보여준다 (CRUD.md § 6-3). */
				request.setAttribute("orders",
						orderService.findMyOrders(memberId, 1, 20).getList());
				render(request, response, "/mypage/review.jsp");
			} else if ("qna".equals(action)) {
				request.setAttribute("pageTitle", "내 문의");
				request.setAttribute("qnaList", mypageService.findMyQna(memberId));
				render(request, response, "/mypage/qna.jsp");
			} else {
				index(request, response, memberId);
			}
		} catch (SQLException e) {
			throw wrap(e);
		}
	}

	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		String action = action(request);
		int memberId = loginMemberId(request);

		try {
			if ("edit".equals(action)) {
				edit(request, response, memberId);
			} else if ("password".equals(action)) {
				password(request, response, memberId);
			} else if ("leave".equals(action)) {
				leave(request, response, memberId);
			} else if ("address".equals(action)) {
				address(request, response, memberId);
			} else if ("review".equals(action)) {
				review(request, response, memberId);
			} else if ("qna".equals(action)) {
				qna(request, response, memberId);
			} else {
				redirect(request, response, "/mypage/");
			}
		} catch (SQLException e) {
			throw wrap(e);
		}
	}

	/* 13번 마이페이지 요약 */
	private void index(HttpServletRequest request, HttpServletResponse response, int memberId)
			throws ServletException, IOException, SQLException {
		request.setAttribute("pageTitle", "마이페이지");
		request.setAttribute("member", memberService.findById(memberId));
		request.setAttribute("recentOrders",
				orderService.findMyOrders(memberId, 1, 5).getList());
		request.setAttribute("wishCount", Integer.valueOf(mypageService.countWishlist(memberId)));
		request.setAttribute("reviews", mypageService.findMyReviews(memberId));
		request.setAttribute("qnaList", mypageService.findMyQna(memberId));
		render(request, response, "/mypage/index.jsp");
	}

	/* 14번 정보수정 */
	private void edit(HttpServletRequest request, HttpServletResponse response, int memberId)
			throws ServletException, IOException, SQLException {
		MemberDTO member = new MemberDTO();
		member.setMemberId(memberId);
		member.setName(param(request, "name"));
		member.setEmail(param(request, "email"));
		member.setPhone(param(request, "phone"));
		try {
			memberService.updateProfile(member);
			/* 세션의 회원 정보도 함께 갱신한다 — 헤더 이름이 옛날 값으로 남지 않게. */
			setLoginMember(request, memberService.findById(memberId));
			flash(request, "회원 정보를 수정했습니다.");
		} catch (ServiceException e) {
			flashError(request, e.getMessage());
		}
		redirect(request, response, "/mypage/edit");
	}

	private void password(HttpServletRequest request, HttpServletResponse response, int memberId)
			throws ServletException, IOException, SQLException {
		try {
			memberService.changePassword(memberId,
					request.getParameter("currentPassword"),
					request.getParameter("newPassword"),
					request.getParameter("newPasswordConfirm"));
			flash(request, "비밀번호를 변경했습니다.");
		} catch (ServiceException e) {
			flashError(request, e.getMessage());
		}
		redirect(request, response, "/mypage/edit");
	}

	/* 탈퇴 — 소프트 삭제 후 세션을 끊는다. */
	private void leave(HttpServletRequest request, HttpServletResponse response, int memberId)
			throws ServletException, IOException, SQLException {
		try {
			memberService.leave(memberId, request.getParameter("password"));
			request.getSession().invalidate();
			redirect(request, response, "/");
		} catch (ServiceException e) {
			flashError(request, e.getMessage());
			redirect(request, response, "/mypage/edit");
		}
	}

	/* 15번 배송지 관리 — 등록 · 수정 · 삭제 · 기본 지정 */
	private void address(HttpServletRequest request, HttpServletResponse response, int memberId)
			throws ServletException, IOException, SQLException {
		String mode = param(request, "mode", "save");
		try {
			if ("delete".equals(mode)) {
				mypageService.deleteAddress(intParam(request, "addrId", 0), memberId);
				flash(request, "배송지를 삭제했습니다.");
			} else if ("default".equals(mode)) {
				mypageService.setDefaultAddress(intParam(request, "addrId", 0), memberId);
				flash(request, "기본 배송지를 변경했습니다.");
			} else {
				AddressDTO addr = new AddressDTO();
				addr.setAddrId(intParam(request, "addrId", 0));
				addr.setMemberId(memberId);
				addr.setAddrName(param(request, "addrName"));
				addr.setReceiver(param(request, "receiver"));
				addr.setReceiverPhone(param(request, "receiverPhone"));
				addr.setZipcode(param(request, "zipcode"));
				addr.setAddr1(param(request, "addr1"));
				addr.setAddr2(param(request, "addr2"));
				mypageService.saveAddress(addr, "Y".equals(param(request, "isDefault")));
				flash(request, "배송지를 저장했습니다.");
			}
		} catch (ServiceException e) {
			flashError(request, e.getMessage());
		}
		redirect(request, response, "/mypage/address");
	}

	/* 17번 내 리뷰 — 작성 · 수정 · 삭제 */
	private void review(HttpServletRequest request, HttpServletResponse response, int memberId)
			throws ServletException, IOException, SQLException {
		String mode = param(request, "mode", "write");
		try {
			if ("update".equals(mode)) {
				mypageService.updateReview(intParam(request, "reviewId", 0), memberId,
						intParam(request, "rating", 0), param(request, "content"));
				flash(request, "리뷰를 수정했습니다.");
			} else if ("delete".equals(mode)) {
				mypageService.deleteReview(intParam(request, "reviewId", 0), memberId);
				flash(request, "리뷰를 삭제했습니다.");
			} else {
				mypageService.writeReview(memberId, intParam(request, "itemId", 0),
						intParam(request, "rating", 0), param(request, "content"));
				flash(request, "리뷰를 등록했습니다.");
			}
		} catch (ServiceException e) {
			flashError(request, e.getMessage());
		}
		String back = param(request, "back");
		redirect(request, response,
				back != null && back.startsWith("/") ? back : "/mypage/review");
	}

	/* 18번 내 문의 — 작성 · 수정 · 삭제 */
	private void qna(HttpServletRequest request, HttpServletResponse response, int memberId)
			throws ServletException, IOException, SQLException {
		String mode = param(request, "mode", "write");
		boolean secret = "Y".equals(param(request, "isSecret"));
		try {
			if ("update".equals(mode)) {
				mypageService.updateQna(intParam(request, "qnaId", 0), memberId,
						param(request, "question"), secret);
				flash(request, "문의를 수정했습니다.");
			} else if ("delete".equals(mode)) {
				mypageService.deleteQna(intParam(request, "qnaId", 0), memberId);
				flash(request, "문의를 삭제했습니다.");
			} else {
				mypageService.writeQna(memberId, intParam(request, "productId", 0),
						param(request, "question"), secret);
				flash(request, "문의를 등록했습니다.");
			}
		} catch (ServiceException e) {
			flashError(request, e.getMessage());
		}
		String back = param(request, "back");
		redirect(request, response, back != null && back.startsWith("/") ? back : "/mypage/qna");
	}
}
