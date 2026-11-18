package shop.dao;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import shop.model.Notice;
import shop.model.Page;
import shop.model.Qna;
import shop.model.Review;

/* 게시판 — 공지 · 리뷰 · 상품 문의 (notice, review, qna) */
public class BoardDAO {

	/* ---------- 공지 ---------- */

	public static List<Notice> latestNotices(int limit) throws SQLException {
		return DB.list(Notice.class, "SELECT * FROM notice ORDER BY notice_id DESC LIMIT ?", limit);
	}

	public static Page<Notice> notices(String keyword, int page) throws SQLException {
		if (keyword != null && keyword.length() > 0) {
			return DB.page(Notice.class, "SELECT *", "FROM notice WHERE title LIKE ?", "ORDER BY notice_id DESC",
					page, 10, ProductDAO.like(keyword));
		}
		return DB.page(Notice.class, "SELECT *", "FROM notice", "ORDER BY notice_id DESC", page, 10);
	}

	public static Notice notice(int noticeId) throws SQLException {
		return DB.one(Notice.class, "SELECT * FROM notice WHERE notice_id = ?", noticeId);
	}

	public static void addNoticeView(int noticeId) throws SQLException {
		DB.update("UPDATE notice SET view_count = view_count + 1 WHERE notice_id = ?", noticeId);
	}

	/* 등록(noticeId 0) 또는 수정 — 바뀐 행 수 */
	public static int saveNotice(int noticeId, String title, String content) throws SQLException {
		if (noticeId > 0) {
			return DB.update("UPDATE notice SET title = ?, content = ? WHERE notice_id = ?", title, content, noticeId);
		}
		return DB.update("INSERT INTO notice (title, content) VALUES (?, ?)", title, content);
	}

	public static void deleteNotice(int noticeId) throws SQLException {
		DB.update("DELETE FROM notice WHERE notice_id = ?", noticeId);
	}

	/* ---------- 리뷰 ---------- */

	private static final String REVIEW = "SELECT r.*, m.name AS member_name, p.name AS product_name";
	private static final String REVIEW_FROM = "FROM review r JOIN member m ON m.member_id = r.member_id"
		+ " JOIN product p ON p.product_id = r.product_id";

	/* 상품 화면의 리뷰 — 작성자 이름은 가린다 (홍길동 → 홍*동) */
	public static List<Review> productReviews(int productId) throws SQLException {
		List<Review> list = DB.list(Review.class, REVIEW + " " + REVIEW_FROM
				+ " WHERE r.product_id = ? ORDER BY r.created_at DESC", productId);
		for (Review r : list) {
			r.setMemberName(mask(r.getMemberName()));
		}
		return list;
	}

	public static List<Review> myReviews(int memberId) throws SQLException {
		return DB.list(Review.class, REVIEW + " " + REVIEW_FROM + " WHERE r.member_id = ? ORDER BY r.created_at DESC", memberId);
	}

	public static Review review(int reviewId) throws SQLException {
		return DB.one(Review.class, REVIEW + " " + REVIEW_FROM + " WHERE r.review_id = ?", reviewId);
	}

	public static boolean hasReview(int itemId) throws SQLException {
		return DB.number("SELECT COUNT(*) FROM review WHERE item_id = ?", itemId) > 0;
	}

	public static void addReview(int memberId, int productId, int itemId, int rating, String content) throws SQLException {
		DB.update("INSERT INTO review (member_id, product_id, item_id, rating, content) VALUES (?, ?, ?, ?, ?)",
				memberId, productId, itemId, rating, content);
	}

	public static int updateReview(int reviewId, int memberId, int rating, String content) throws SQLException {
		return DB.update("UPDATE review SET rating = ?, content = ? WHERE review_id = ? AND member_id = ?",
				rating, content, reviewId, memberId);
	}

	/* 삭제 — memberId 가 0 이면 관리자 삭제(누구 것이든) */
	public static int deleteReview(int reviewId, int memberId) throws SQLException {
		return memberId > 0
				? DB.update("DELETE FROM review WHERE review_id = ? AND member_id = ?", reviewId, memberId)
				: DB.update("DELETE FROM review WHERE review_id = ?", reviewId);
	}

	public static Page<Review> adminReviews(String keyword, int page) throws SQLException {
		if (keyword != null && keyword.length() > 0) {
			String like = ProductDAO.like(keyword);
			return DB.page(Review.class, REVIEW, REVIEW_FROM + " WHERE (p.name LIKE ? OR m.login_id LIKE ? OR r.content LIKE ?)",
					"ORDER BY r.created_at DESC", page, 10, like, like, like);
		}
		return DB.page(Review.class, REVIEW, REVIEW_FROM, "ORDER BY r.created_at DESC", page, 10);
	}

	/* ---------- 상품 문의 ---------- */

	private static final String QNA = "SELECT q.*, m.name AS member_name, p.name AS product_name";
	private static final String QNA_FROM = "FROM qna q JOIN member m ON m.member_id = q.member_id"
		+ " JOIN product p ON p.product_id = q.product_id";

	/* 상품 화면의 문의 — 작성자 이름은 가리고, 남의 비밀글은 내용을 지운다 */
	public static List<Qna> productQna(int productId, int viewerId, boolean admin) throws SQLException {
		List<Qna> list = DB.list(Qna.class, QNA + " " + QNA_FROM + " WHERE q.product_id = ? ORDER BY q.created_at DESC", productId);
		for (Qna q : list) {
			q.setMemberName(mask(q.getMemberName()));
			boolean mine = viewerId > 0 && viewerId == q.getMemberId();
			if (q.isSecretPost() && !mine && !admin) {
				q.hide();
			}
		}
		return list;
	}

	public static List<Qna> myQna(int memberId) throws SQLException {
		return DB.list(Qna.class, QNA + " " + QNA_FROM + " WHERE q.member_id = ? ORDER BY q.created_at DESC", memberId);
	}

	public static Qna qna(int qnaId) throws SQLException {
		return DB.one(Qna.class, QNA + " " + QNA_FROM + " WHERE q.qna_id = ?", qnaId);
	}

	public static void addQna(int memberId, int productId, String question, boolean secret) throws SQLException {
		DB.update("INSERT INTO qna (member_id, product_id, question, is_secret) VALUES (?, ?, ?, ?)",
				memberId, productId, question, secret ? "Y" : "N");
	}

	public static void updateQna(int qnaId, int memberId, String question, boolean secret) throws SQLException {
		DB.update("UPDATE qna SET question = ?, is_secret = ? WHERE qna_id = ? AND member_id = ?",
				question, secret ? "Y" : "N", qnaId, memberId);
	}

	public static int deleteQna(int qnaId, int memberId) throws SQLException {
		return DB.update("DELETE FROM qna WHERE qna_id = ? AND member_id = ?", qnaId, memberId);
	}

	public static int answer(int qnaId, String answer) throws SQLException {
		return DB.update("UPDATE qna SET answer = ?, answered_at = NOW() WHERE qna_id = ?", answer, qnaId);
	}

	/* 문의 목록 (관리자) — 미답변이 먼저, 오래된 것부터 */
	public static Page<Qna> adminQna(String keyword, boolean onlyWaiting, int page) throws SQLException {
		StringBuilder where = new StringBuilder(QNA_FROM + " WHERE 1 = 1");
		List<Object> args = new ArrayList<Object>();
		if (onlyWaiting) {
			where.append(" AND q.answered_at IS NULL");
		}
		if (keyword != null && keyword.length() > 0) {
			where.append(" AND (p.name LIKE ? OR m.login_id LIKE ? OR q.question LIKE ?)");
			String like = ProductDAO.like(keyword);
			args.add(like);
			args.add(like);
			args.add(like);
		}
		return DB.page(Qna.class, QNA, where.toString(), "ORDER BY (q.answered_at IS NOT NULL), q.created_at", page, 10, args);
	}

	/* 이름 가리기 — 홍길동 → 홍*동, 김철 → 김* */
	public static String mask(String name) {
		if (name == null || name.trim().isEmpty()) {
			return "";
		}
		if (name.length() == 1) {
			return "*";
		}
		if (name.length() == 2) {
			return name.charAt(0) + "*";
		}
		return name.charAt(0) + "*".repeat(name.length() - 2) + name.charAt(name.length() - 1);
	}
}
