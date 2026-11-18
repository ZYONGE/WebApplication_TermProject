<%@ include file="../common/header.jsp" %>
<%@ include file="../common/mynav.jsp" %>
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
					<button type="button" class="btn btn_small" data-popup-open="reviewPopup" data-fill-review-popup-title="리뷰 수정" data-fill-review-mode="update"
					        data-fill-review-id="${r.reviewId}" data-fill-review-item-id="" data-fill-review-product-name="<c:out value='${r.productName}' />"
					        data-fill-rating="${r.rating}" data-fill-content="<c:out value='${r.content}' />">수정</button>
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
<table class="listtable">
	<thead class="listtable_head">
		<tr><th class="col_no">주문번호</th><th class="col_name">상품</th>
		    <th class="col_state">배송상태</th><th class="col_action"></th></tr>
	</thead>
	<tbody>
		<c:forEach var="row" items="${writable}">
			<tr>
				<td class="col_no"><c:out value="${row.orderNo}" /></td>
				<td class="col_name"><c:out value="${row.item.productName}" /></td>
				<td class="col_state">배송완료</td>
				<td class="col_action">
					<button type="button" class="btn btn_small" data-popup-open="reviewPopup" data-fill-review-popup-title="리뷰 작성" data-fill-review-mode="write"
					        data-fill-review-item-id="${row.item.itemId}" data-fill-review-id="" data-fill-review-product-name="<c:out value='${row.item.productName}' />" data-fill-content="">리뷰 쓰기</button>
				</td>
			</tr>
		</c:forEach>
		<c:if test="${empty writable}">
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
<%@ include file="../common/footer.jsp" %>
