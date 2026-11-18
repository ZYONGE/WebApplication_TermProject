package shop.service;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import shop.dao.BoardDAO;
import shop.dao.CartDAO;
import shop.dao.MemberDAO;
import shop.dao.OrderDAO;
import shop.model.Address;
import shop.model.Item;
import shop.model.Member;
import shop.model.Order;
import shop.model.Qna;

/* 마이페이지 — 내 정보 · 배송지 · 찜 · 내 리뷰 · 내 문의 */
@WebServlet({ "/mypage", "/mypage/", "/mypage/edit", "/mypage/password", "/mypage/leave", "/mypage/address",
		"/mypage/wishlist", "/mypage/review", "/mypage/qna" })
public class MypageServlet extends BaseServlet {

	private static final long serialVersionUID = 1L;

	@Override
	protected void get(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException, SQLException {
		int memberId = loginId(request);
		switch (request.getServletPath()) {
		case "/mypage/edit":
			request.setAttribute("member", MemberDAO.byId(memberId));
			render(request, response, "mypage/edit", "정보 수정");
			break;
		case "/mypage/address":
			request.setAttribute("addresses", MemberDAO.addresses(memberId));
			render(request, response, "mypage/address", "배송지 관리");
			break;
		case "/mypage/wishlist":
			request.setAttribute("items", CartDAO.wishes(memberId));
			render(request, response, "mypage/wishlist", "찜 목록");
			break;
		case "/mypage/review":
			/* 리뷰를 쓸 수 있는 주문 상품 = 배송완료된 정상 주문 중 아직 리뷰가 없는 것 (최근 주문 20건에서) */
			List<Map<String, Object>> writable = new ArrayList<Map<String, Object>>();
			for (Order o : OrderDAO.myOrders(memberId, 1, 20).getList()) {
				for (Item item : o.getItems()) {
					if ("배송완료".equals(o.getDeliveryStatus()) && "정상".equals(o.getOrderStatus()) && !item.isReviewWritten()) {
						Map<String, Object> row = new HashMap<String, Object>();
						row.put("orderNo", o.getOrderNo());
						row.put("item", item);
						writable.add(row);
					}
				}
			}
			request.setAttribute("reviews", BoardDAO.myReviews(memberId));
			request.setAttribute("writable", writable);
			render(request, response, "mypage/review", "내 리뷰");
			break;
		case "/mypage/qna":
			request.setAttribute("qnaList", BoardDAO.myQna(memberId));
			render(request, response, "mypage/qna", "내 문의");
			break;
		default:
			List<Address> addresses = MemberDAO.addresses(memberId);
			request.setAttribute("member", MemberDAO.byId(memberId));
			request.setAttribute("recentOrders", OrderDAO.myOrders(memberId, 1, 5).getList());
			request.setAttribute("wishCount", CartDAO.countWishes(memberId));
			/* 기본 배송지가 맨 앞에 온다 */
			request.setAttribute("mainAddr", addresses.isEmpty() ? null : addresses.get(0));
			request.setAttribute("addressCount", addresses.size());
			request.setAttribute("reviewCount", BoardDAO.myReviews(memberId).size());
			request.setAttribute("qnaCount", BoardDAO.myQna(memberId).size());
			render(request, response, "mypage/index", "마이페이지");
		}
	}

	@Override
	protected void post(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException, SQLException {
		int memberId = loginId(request);
		String path = request.getServletPath();
		String go = path.equals("/mypage/password") || path.equals("/mypage/leave") ? "/mypage/edit" : path;
		try {
			switch (path) {
			case "/mypage/edit":
				MemberServlet.checkProfile(param(request, "name"), param(request, "email"), param(request, "phone"));
				if (MemberDAO.exists("email", param(request, "email"), memberId)) {
					throw new Fail("다른 회원이 사용 중인 이메일입니다.");
				}
				MemberDAO.update(memberId, param(request, "name"), param(request, "email"), param(request, "phone"));
				request.getSession().setAttribute("loginMember", MemberDAO.byId(memberId));
				flash(request, "회원 정보를 수정했습니다.");
				break;
			case "/mypage/password":
				if (!MemberDAO.byId(memberId).getPassword().equals(request.getParameter("currentPassword"))) {
					throw new Fail("현재 비밀번호가 올바르지 않습니다.");
				}
				MemberServlet.checkPassword(request.getParameter("newPassword"), request.getParameter("newPasswordConfirm"));
				MemberDAO.updatePassword(memberId, request.getParameter("newPassword"));
				flash(request, "비밀번호를 변경했습니다.");
				break;
			case "/mypage/leave":
				Member me = MemberDAO.byId(memberId);
				if (!me.getPassword().equals(request.getParameter("password"))) {
					throw new Fail("비밀번호가 올바르지 않습니다.");
				}
				if ("DEV".equals(me.getRole())) {
					throw new Fail("DEV 계정은 탈퇴할 수 없습니다.");
				}
				MemberDAO.leave(memberId, false);
				request.getSession().invalidate();
				go = "/";
				break;
			case "/mypage/address":
				address(request, memberId);
				break;
			case "/mypage/review":
				review(request, memberId);
				back(request, response, "/mypage/review");
				return;
			case "/mypage/qna":
				qna(request, memberId);
				back(request, response, "/mypage/qna");
				return;
			default:
				go = "/mypage/";
			}
		} catch (Fail e) {
			flashError(request, e.getMessage());
			if (path.equals("/mypage/review") || path.equals("/mypage/qna")) {
				back(request, response, path);
				return;
			}
		}
		redirect(request, response, go);
	}

	private void address(HttpServletRequest request, int memberId) throws SQLException {
		int addrId = intParam(request, "addrId", 0);
		String mode = param(request, "mode", "save");
		if (mode.equals("delete")) {
			Address target = MemberDAO.address(addrId, memberId);
			if (target == null) {
				throw new Fail("배송지를 찾을 수 없습니다.");
			}
			MemberDAO.deleteAddress(target);
			flash(request, "배송지를 삭제했습니다.");
		} else if (mode.equals("default")) {
			if (!MemberDAO.setDefault(addrId, memberId)) {
				throw new Fail("배송지를 찾을 수 없습니다.");
			}
			flash(request, "기본 배송지를 변경했습니다.");
		} else {
			String[][] required = { { "receiver", "받는 사람을" }, { "receiverPhone", "연락처를" },
					{ "zipcode", "우편번호를" }, { "addr1", "주소를" } };
			for (String[] r : required) {
				if (isEmpty(param(request, r[0]))) {
					throw new Fail(r[1] + " 입력하세요.");
				}
			}
			/* 첫 배송지는 자동으로 기본 배송지 */
			boolean asDefault = "Y".equals(param(request, "isDefault")) || MemberDAO.addresses(memberId).isEmpty();
			if (!MemberDAO.saveAddress(addrId, memberId, param(request, "addrName"), param(request, "receiver"),
					param(request, "receiverPhone"), param(request, "zipcode"), param(request, "addr1"),
					param(request, "addr2"), asDefault)) {
				throw new Fail("배송지를 찾을 수 없습니다.");
			}
			flash(request, "배송지를 저장했습니다.");
		}
	}

	private void review(HttpServletRequest request, int memberId) throws SQLException {
		String mode = param(request, "mode", "write");
		int rating = intParam(request, "rating", 0);
		String content = param(request, "content");
		if (!mode.equals("delete")) {
			if (rating < 1 || rating > 5) {
				throw new Fail("별점은 1~5점으로 선택하세요.");
			}
			if (isEmpty(content)) {
				throw new Fail("리뷰 내용을 입력하세요.");
			}
			if (content.length() > 1000) {
				throw new Fail("리뷰는 1000자 이내로 입력하세요.");
			}
		}
		if (mode.equals("update")) {
			if (BoardDAO.updateReview(intParam(request, "reviewId", 0), memberId, rating, content) == 0) {
				throw new Fail("본인이 쓴 리뷰만 수정할 수 있습니다.");
			}
			flash(request, "리뷰를 수정했습니다.");
		} else if (mode.equals("delete")) {
			if (BoardDAO.deleteReview(intParam(request, "reviewId", 0), memberId) == 0) {
				throw new Fail("본인이 쓴 리뷰만 삭제할 수 있습니다.");
			}
			flash(request, "리뷰를 삭제했습니다.");
		} else {
			/* 내 주문 중에서 이 주문 상품(itemId)을 찾는다 */
			int itemId = intParam(request, "itemId", 0);
			Order found = null;
			Item target = null;
			for (Order o : OrderDAO.myOrders(memberId, 1, 100).getList()) {
				for (Item item : o.getItems()) {
					if (item.getItemId() == itemId) {
						found = o;
						target = item;
					}
				}
			}
			if (target == null) {
				throw new Fail("본인이 주문한 상품만 리뷰를 쓸 수 있습니다.");
			}
			if (!"배송완료".equals(found.getDeliveryStatus())) {
				throw new Fail("배송완료된 주문에만 리뷰를 쓸 수 있습니다.");
			}
			if (BoardDAO.hasReview(itemId)) {
				throw new Fail("이미 리뷰를 작성한 주문 건입니다.");
			}
			BoardDAO.addReview(memberId, target.getProductId(), itemId, rating, content);
			flash(request, "리뷰를 등록했습니다.");
		}
	}

	private void qna(HttpServletRequest request, int memberId) throws SQLException {
		String mode = param(request, "mode", "write");
		int qnaId = intParam(request, "qnaId", 0);
		String question = param(request, "question");
		boolean secret = "Y".equals(param(request, "isSecret"));
		if (!mode.equals("delete")) {
			if (isEmpty(question)) {
				throw new Fail("문의 내용을 입력하세요.");
			}
			if (question.length() > 1000) {
				throw new Fail("문의는 1000자 이내로 입력하세요.");
			}
		}
		if (mode.equals("update")) {
			Qna qna = BoardDAO.qna(qnaId);
			if (qna == null || qna.getMemberId() != memberId) {
				throw new Fail("본인이 쓴 문의만 수정할 수 있습니다.");
			}
			if (qna.isAnswered()) {
				throw new Fail("답변이 등록된 문의는 수정할 수 없습니다.");
			}
			BoardDAO.updateQna(qnaId, memberId, question, secret);
			flash(request, "문의를 수정했습니다.");
		} else if (mode.equals("delete")) {
			if (BoardDAO.deleteQna(qnaId, memberId) == 0) {
				throw new Fail("본인이 쓴 문의만 삭제할 수 있습니다.");
			}
			flash(request, "문의를 삭제했습니다.");
		} else {
			BoardDAO.addQna(memberId, intParam(request, "productId", 0), question, secret);
			flash(request, "문의를 등록했습니다.");
		}
	}
}
