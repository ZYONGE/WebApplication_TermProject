<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ include file="/common/header.jsp" %>

<ul class="my_nav">
	<li><a href="${ctx}/mypage/">마이페이지</a></li>
	<li><a href="${ctx}/mypage/edit">정보수정</a></li>
	<li><a href="${ctx}/mypage/address">배송지</a></li>
	<li><a href="${ctx}/mypage/wishlist">찜 목록</a></li>
	<li><a href="${ctx}/mypage/review">내 리뷰</a></li>
	<li><a href="${ctx}/mypage/qna">내 문의</a></li>
	<li><a href="${ctx}/order/list">주문 내역</a></li>
</ul>

<h2>내 리뷰</h2>

<table class="listtable">
	<thead class="listtable_head">
		<tr><th class="col_name">상품</th><th class="col_rating">별점</th>
		    <th class="col_content">내용</th><th class="col_date">작성일</th><th class="col_action"></th></tr>
	</thead>
	<tbody>
		<c:forEach var="r" items="${reviews}">
			<tr>
				<td class="col_name"><c:out value="${r.productName}" /></td>
				<td class="col_rating">${r.rating} / 5</td>
				<td class="col_content"><c:out value="${r.content}" /></td>
				<td class="col_date"><fmt:formatDate value="${r.createdAt}" pattern="yyyy-MM-dd" /></td>
				<td class="col_action">
					<button type="button" class="btn btn_small"
					        onclick="editReview(${r.reviewId}, ${r.rating}, '<c:out value="${r.productName}" />', this)">수정</button>
					<span hidden><c:out value="${r.content}" /></span>
					<form action="${ctx}/mypage/review" method="post">
						<input type="hidden" name="mode" value="delete">
						<input type="hidden" name="reviewId" value="${r.reviewId}">
						<button type="submit" class="btn btn_danger btn_small"
						        data-confirm-message="리뷰를 삭제하시겠습니까?">삭제</button>
					</form>
				</td>
			</tr>
		</c:forEach>
		<c:if test="${empty reviews}">
			<tr><td colspan="5" class="empty">작성한 리뷰가 없습니다.</td></tr>
		</c:if>
	</tbody>
</table>

<h3>리뷰를 쓸 수 있는 주문</h3>
<%--
  작성 조건은 배송완료 + 미작성이다. 리뷰는 강제가 아니므로 "안 쓴 건" 을
  목록으로만 보여주고 재촉하지 않는다 (CRUD.md § 6-3).
--%>
<table class="listtable">
	<thead class="listtable_head">
		<tr><th class="col_no">주문번호</th><th class="col_name">상품</th>
		    <th class="col_state">배송상태</th><th class="col_action"></th></tr>
	</thead>
	<tbody>
		<c:set var="writableCount" value="0" />
		<c:forEach var="order" items="${orders}">
			<c:if test="${not empty order.delivery and order.delivery.status == '배송완료'
			              and order.orderStatus == '정상'}">
				<c:forEach var="item" items="${order.items}">
					<c:if test="${not item.reviewWritten}">
						<c:set var="writableCount" value="${writableCount + 1}" />
						<tr>
							<td class="col_no"><c:out value="${order.orderNo}" /></td>
							<td class="col_name"><c:out value="${item.productName}" /></td>
							<td class="col_state">배송완료</td>
							<td class="col_action">
								<button type="button" class="btn btn_small"
								        onclick="writeReview(${item.itemId}, '<c:out value="${item.productName}" />')">리뷰 쓰기</button>
							</td>
						</tr>
					</c:if>
				</c:forEach>
			</c:if>
		</c:forEach>
		<c:if test="${writableCount == 0}">
			<tr><td colspan="4" class="empty">리뷰를 쓸 수 있는 주문이 없습니다.</td></tr>
		</c:if>
	</tbody>
</table>

<dialog id="reviewPopup" class="popup">
	<form class="popup_form" action="${ctx}/mypage/review" method="post" data-validate>
		<input type="hidden" name="mode" id="reviewMode" value="write">
		<input type="hidden" name="itemId" id="reviewItemId">
		<input type="hidden" name="reviewId" id="reviewId">

		<h3 class="popup_head" id="reviewPopupTitle">리뷰 작성</h3>
		<div class="popup_body">
			<p id="reviewProductName"></p>
			<label for="rating">별점</label>
			<select id="rating" name="rating">
				<option value="5">5 — 매우 만족</option>
				<option value="4">4 — 만족</option>
				<option value="3">3 — 보통</option>
				<option value="2">2 — 불만</option>
				<option value="1">1 — 매우 불만</option>
			</select>
			<label for="content">내용</label>
			<textarea id="content" name="content" rows="5" class="field" data-rule="required"></textarea>
		</div>
		<div class="popup_foot">
			<button type="submit" class="btn btn_primary">저장</button>
			<button type="button" class="btn btn_sub popup_close" data-popup-close="reviewPopup">닫기</button>
		</div>
	</form>
</dialog>

<script>
function writeReview(itemId, productName) {
	document.getElementById("reviewPopupTitle").textContent = "리뷰 작성";
	document.getElementById("reviewMode").value = "write";
	document.getElementById("reviewItemId").value = itemId;
	document.getElementById("reviewId").value = "";
	document.getElementById("reviewProductName").textContent = productName;
	document.getElementById("content").value = "";
	window.openPopup("reviewPopup");
}

function editReview(reviewId, rating, productName, button) {
	document.getElementById("reviewPopupTitle").textContent = "리뷰 수정";
	document.getElementById("reviewMode").value = "update";
	document.getElementById("reviewId").value = reviewId;
	document.getElementById("reviewItemId").value = "";
	document.getElementById("reviewProductName").textContent = productName;
	document.getElementById("rating").value = rating;
	/* 본문은 같은 칸의 hidden span 에서 읽는다 — 목록을 다시 조회하지 않는다. */
	var holder = button.parentNode.querySelector("span[hidden]");
	document.getElementById("content").value = holder ? holder.textContent.trim() : "";
	window.openPopup("reviewPopup");
}
</script>
<%@ include file="/common/footer.jsp" %>
