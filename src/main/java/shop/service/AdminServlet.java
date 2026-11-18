package shop.service;

import java.io.IOException;
import java.sql.SQLException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import shop.dao.AdminDAO;
import shop.dao.BoardDAO;
import shop.dao.ConfigDAO;
import shop.dao.DB;
import shop.dao.MemberDAO;
import shop.dao.OrderDAO;
import shop.model.Member;
import shop.model.Review;

/* 관리자 — 대시보드 · 주문 배송 · 회원 · 공지 · 문의 답변 · 리뷰 */
@WebServlet({ "/admin/dashboard", "/admin/orders", "/admin/members", "/admin/notices", "/admin/qna", "/admin/reviews" })
public class AdminServlet extends BaseServlet {

	private static final long serialVersionUID = 1L;

	@Override
	protected void get(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException, SQLException {
		String keyword = param(request, "keyword");
		int page = intParam(request, "page", 1);
		String path = request.getServletPath();
		request.setAttribute("pageUrl", path + "?");
		switch (path) {
		case "/admin/orders":
			request.setAttribute("pageData", OrderDAO.adminPage(keyword, param(request, "deliveryStatus"),
					param(request, "orderStatus"), page));
			request.setAttribute("flow", OrderDAO.FLOW);
			if (intParam(request, "orderId", 0) > 0) {
				request.setAttribute("detail", OrderDAO.detail(intParam(request, "orderId", 0), 0));
			}
			render(request, response, "admin/orders", "주문 · 배송 관리");
			break;
		case "/admin/members":
			request.setAttribute("pageData", MemberDAO.page(keyword, param(request, "role"), page, 10));
			render(request, response, "admin/members", "회원 관리");
			break;
		case "/admin/notices":
			request.setAttribute("pageData", BoardDAO.notices(keyword, page));
			if (intParam(request, "editId", 0) > 0) {
				request.setAttribute("editNotice", BoardDAO.notice(intParam(request, "editId", 0)));
			}
			render(request, response, "admin/notices", "공지 관리");
			break;
		case "/admin/qna":
			boolean onlyWaiting = !"all".equals(param(request, "filter"));
			request.setAttribute("onlyUnanswered", onlyWaiting);
			request.setAttribute("pageData", BoardDAO.adminQna(keyword, onlyWaiting, page));
			request.setAttribute("pageUrl", "/admin/qna?filter=" + (onlyWaiting ? "wait" : "all") + "&");
			render(request, response, "admin/qna", "문의 답변");
			break;
		case "/admin/reviews":
			request.setAttribute("pageData", BoardDAO.adminReviews(keyword, page));
			render(request, response, "admin/reviews", "리뷰 관리");
			break;
		default:
			request.setAttribute("summary", AdminDAO.summary());
			request.setAttribute("salesDaily", AdminDAO.salesDaily(7));
			request.setAttribute("deliveryCounts", AdminDAO.deliveryCounts());
			request.setAttribute("lowStock", AdminDAO.lowStock());
			request.setAttribute("lowStockThreshold", ConfigDAO.number("LOW_STOCK_THRESHOLD", 30));
			render(request, response, "admin/dashboard", "관리자 대시보드");
		}
	}

	@Override
	protected void post(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException, SQLException {
		int adminId = loginId(request);
		String path = request.getServletPath();
		String go = path;
		try {
			switch (path) {
			case "/admin/orders":
				go = path + returnQuery(request);
				if ("cancel".equals(param(request, "mode"))) {
					OrderServlet.cancel(intParam(request, "orderId", 0), adminId, true);
					flash(request, "주문을 취소했습니다. 재고가 복구되었습니다.");
				} else {
					nextStep(adminId, intParam(request, "deliveryId", 0), param(request, "courier"), param(request, "trackingNo"));
					flash(request, "배송 상태를 다음 단계로 넘겼습니다.");
				}
				break;
			case "/admin/members":
				leaveMember(adminId, intParam(request, "memberId", 0));
				flash(request, "탈퇴 처리했습니다.");
				break;
			case "/admin/notices":
				notice(request, adminId);
				break;
			case "/admin/qna":
				String answer = param(request, "answer");
				if (isEmpty(answer)) {
					throw new Fail("답변 내용을 입력하세요.");
				}
				if (answer.length() > 1000) {
					throw new Fail("답변은 1000자 이내로 입력하세요.");
				}
				int qnaId = intParam(request, "qnaId", 0);
				DB.begin();
				try {
					if (BoardDAO.answer(qnaId, answer) == 0) {
						throw new Fail("문의를 찾을 수 없습니다.");
					}
					AdminDAO.adminLog(adminId, "QNA_ANSWER", "qna", qnaId, "답변 등록");
					DB.commit();
				} finally {
					DB.end();
				}
				flash(request, "답변을 등록했습니다.");
				break;
			case "/admin/reviews":
				int reviewId = intParam(request, "reviewId", 0);
				Review review = BoardDAO.review(reviewId);
				if (review == null) {
					throw new Fail("리뷰를 찾을 수 없습니다.");
				}
				BoardDAO.deleteReview(reviewId, 0);
				AdminDAO.adminLog(adminId, "REVIEW_DELETE", "review", reviewId,
						"삭제: " + review.getProductName() + " / 작성자 " + review.getMemberName());
				flash(request, "리뷰를 삭제했습니다.");
				break;
			}
		} catch (Fail e) {
			flashError(request, e.getMessage());
		}
		redirect(request, response, go);
	}

	/* 목록 화면의 검색 조건을 유지해서 돌아간다 (열려 있던 주문 상세 orderId 는 뺀다) */
	private String returnQuery(HttpServletRequest request) {
		String query = request.getParameter("returnQuery");
		if (query == null || query.contains("\r") || query.contains("\n")) {
			return "";
		}
		StringBuilder kept = new StringBuilder();
		for (String pair : query.split("&")) {
			if (!pair.isEmpty() && !pair.startsWith("orderId=")) {
				kept.append(kept.length() == 0 ? "?" : "&").append(pair);
			}
		}
		return kept.toString();
	}

	/* 배송 다음 단계로 — 배송중으로 넘길 때는 택배사 · 운송장 번호가 필요하다 */
	private void nextStep(int adminId, int deliveryId, String courier, String trackingNo) throws SQLException {
		String from = OrderDAO.deliveryStatus(deliveryId);
		if (from == null) {
			throw new Fail("배송 정보를 찾을 수 없습니다.");
		}
		int step = OrderDAO.step(from);
		if (step < 0 || step == OrderDAO.FLOW.length - 1) {
			throw new Fail("이미 배송완료된 주문입니다.");
		}
		String to = OrderDAO.FLOW[step + 1];
		boolean shipping = "배송중".equals(to);
		if (shipping && (isEmpty(courier) || isEmpty(trackingNo))) {
			throw new Fail("택배사와 운송장 번호를 입력해야 배송중으로 넘어갑니다.");
		}
		DB.begin();
		try {
			if (OrderDAO.moveNext(deliveryId, from, to, courier, trackingNo) == 0) {
				throw new Fail("배송 상태가 이미 바뀌었습니다. 새로고침 후 다시 시도하세요.");
			}
			OrderDAO.addHistory(deliveryId, from, to, adminId, false);
			AdminDAO.adminLog(adminId, "DELIVERY_NEXT_STEP", "delivery", deliveryId, from + " -> " + to);
			if (shipping) {
				AdminDAO.adminLog(adminId, "DELIVERY_TRACKING_SET", "delivery", deliveryId, courier + " / " + trackingNo);
			}
			DB.commit();
		} finally {
			DB.end();
		}
	}

	private void leaveMember(int adminId, int memberId) throws SQLException {
		Member target = MemberDAO.byId(memberId);
		if (target == null) {
			throw new Fail("회원을 찾을 수 없습니다.");
		}
		if (!"USER".equals(target.getRole())) {
			throw new Fail("일반 회원만 탈퇴 처리할 수 있습니다.");
		}
		if ("LEFT".equals(target.getStatus())) {
			throw new Fail("이미 탈퇴한 회원입니다.");
		}
		DB.begin();
		try {
			if (MemberDAO.leave(memberId, true) == 0) {
				throw new Fail("탈퇴 처리하지 못했습니다.");
			}
			AdminDAO.adminLog(adminId, "MEMBER_LEAVE", "member", memberId, target.getLoginId() + ": ACTIVE -> LEFT");
			DB.commit();
		} finally {
			DB.end();
		}
	}

	private void notice(HttpServletRequest request, int adminId) throws SQLException {
		int noticeId = intParam(request, "noticeId", 0);
		String title = param(request, "title");
		if ("delete".equals(param(request, "mode"))) {
			BoardDAO.deleteNotice(noticeId);
			AdminDAO.adminLog(adminId, "NOTICE_DELETE", "notice", noticeId, "삭제");
			flash(request, "공지를 삭제했습니다.");
			return;
		}
		if (isEmpty(title) || title.length() > 200) {
			throw new Fail("제목을 200자 이내로 입력하세요.");
		}
		if (isEmpty(param(request, "content"))) {
			throw new Fail("내용을 입력하세요.");
		}
		if (BoardDAO.saveNotice(noticeId, title, param(request, "content")) == 0) {
			throw new Fail("공지를 찾을 수 없습니다.");
		}
		if (noticeId > 0) {
			AdminDAO.adminLog(adminId, "NOTICE_UPDATE", "notice", noticeId, "수정: " + title);
			flash(request, "공지를 수정했습니다.");
		} else {
			AdminDAO.adminLog(adminId, "NOTICE_CREATE", "notice", null, "등록: " + title);
			flash(request, "공지를 등록했습니다.");
		}
	}
}
