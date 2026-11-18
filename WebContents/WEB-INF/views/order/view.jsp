<%@ include file="../common/header.jsp" %>
<c:set var="myTab" value="order" />
<%@ include file="../common/mynav.jsp" %>
<h2>주문 상세</h2>
<table class="formtable">
	<tr><th>주문번호</th><td><c:out value="${order.orderNo}" /></td></tr>
	<tr><th>주문일시</th><td><fmt:formatDate value="${order.orderedAt}" pattern="yyyy-MM-dd HH:mm" /></td></tr>
	<tr><th>주문상태</th>
		<td>
			<c:out value="${order.orderStatus}" />
			<c:if test="${order.orderStatus == '취소'}">
				(<fmt:formatDate value="${order.canceledAt}" pattern="yyyy-MM-dd HH:mm" />)
			</c:if>
		</td>
	</tr>
	<tr>
		<th>받는 사람</th>
		<td><c:out value="${order.receiver}" /> (<c:out value="${order.receiverPhone}" />)</td>
	</tr>
	<tr>
		<th>배송지</th>
		<td>[<c:out value="${order.zipcode}" />] <c:out value="${order.addr1}" /> <c:out value="${order.addr2}" /></td>
	</tr>
	<tr><th>요청사항</th><td><c:out value="${order.deliveryMemo}" /></td></tr>
</table>
<h3>결제</h3>
<table class="formtable">
	<tr><th>결제수단</th><td><c:out value="${order.payMethod}" /></td></tr>
	<tr><th>결제상태</th>
		<td>
			<c:out value="${order.payStatus}" />
			<c:if test="${order.payStatus == '환불'}">
				(<fmt:formatDate value="${order.refundedAt}" pattern="yyyy-MM-dd HH:mm" />)
			</c:if>
		</td>
	</tr>
	<tr><th>상품 합계</th><td><fmt:formatNumber value="${order.totalPrice}" pattern="#,##0" />원</td></tr>
	<tr><th>배송비</th><td><fmt:formatNumber value="${order.deliveryFee}" pattern="#,##0" />원</td></tr>
	<tr><th>결제금액</th><td><fmt:formatNumber value="${order.payAmount}" pattern="#,##0" />원</td></tr>
</table>
<h3>배송 조회</h3>
<c:choose>
	<c:when test="${empty order.deliveryId}">
		<p class="empty">배송 정보가 없습니다.</p>
	</c:when>
	<c:otherwise>
		<table class="formtable">
			<tr><th>배송상태</th><td><c:out value="${order.displayStatus}" /></td></tr>
			<tr><th>택배사</th><td><c:out value="${empty order.courier ? '-' : order.courier}" /></td></tr>
			<tr>
				<th>운송장번호</th>
				<td><c:out value="${empty order.trackingNo ? '-' : order.trackingNo}" /></td>
			</tr>
		</table>
		<ol class="order_timeline">
			<c:forEach var="h" items="${order.history}">
				<li>
					<fmt:formatDate value="${h.changedAt}" pattern="yyyy-MM-dd HH:mm" />
					— <c:out value="${h.newStatus}" />
				</li>
			</c:forEach>
		</ol>
	</c:otherwise>
</c:choose>
<h3>주문 상품</h3>
<table class="listtable">
	<thead class="listtable_head">
		<tr><th class="col_name">상품</th><th class="col_price">1포 가격</th><th class="col_qty">섭취 일수</th>
		    <th class="col_total">합계</th><th class="col_action">리뷰</th></tr>
	</thead>
	<tbody>
		<c:forEach var="item" items="${order.items}">
			<tr>
				<td class="col_name">
					<a href="${ctx}/product/view?productId=${item.productId}">
						<c:out value="${item.productName}" />
					</a>
					<c:if test="${not empty item.trackSnapshot}"><span class="field_help">구성: <c:out value="${item.trackSnapshot}" /></span></c:if>
				</td>
				<td class="col_price"><fmt:formatNumber value="${item.price}" pattern="#,##0" />원</td>
				<td class="col_qty">${item.quantity}일분 (${item.quantity}포)</td>
				<td class="col_total"><fmt:formatNumber value="${item.lineTotal}" pattern="#,##0" />원</td>
				<td class="col_action">
					<c:choose>
						<c:when test="${item.reviewWritten}">작성 완료</c:when>
						<c:when test="${order.deliveryStatus == '배송완료'}">
							<button type="button" class="btn btn_small" data-popup-open="reviewPopup" data-fill-review-item-id="${item.itemId}" data-fill-review-product-name="<c:out value='${item.productName}' />">리뷰 쓰기</button>
						</c:when>
						<c:otherwise>-</c:otherwise>
					</c:choose>
				</td>
			</tr>
		</c:forEach>
	</tbody>
</table>
<div class="btnstyle">
	<a class="btn btn_sub" href="${ctx}/order/list">주문 내역</a>
	<c:if test="${order.cancelable}">
		<form action="${ctx}/order/cancel" method="post">
			<input type="hidden" name="orderId" value="${order.orderId}">
			<button type="submit" class="btn btn_danger"
			        data-confirm-message="주문을 취소하시겠습니까? 재고가 복구됩니다.">주문 취소</button>
		</form>
	</c:if>
</div>
<dialog id="reviewPopup" class="popup">
	<form class="popup_form" action="${ctx}/mypage/review" method="post" data-validate>
		<input type="hidden" name="mode" value="write">
		<input type="hidden" name="itemId" id="reviewItemId">
		<input type="hidden" name="back" value="/order/view?orderId=${order.orderId}">
		<h3 class="popup_head">리뷰 작성</h3>
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
			<button type="submit" class="btn btn_primary">등록</button>
			<button type="button" class="btn btn_sub popup_close" data-popup-close="reviewPopup">닫기</button>
		</div>
	</form>
</dialog>
<%@ include file="../common/footer.jsp" %>
