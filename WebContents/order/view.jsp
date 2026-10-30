<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<c:set var="pageCss" value="order" />
<%@ include file="/common/header.jsp" %>

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
	<tr><th>결제수단</th><td><c:out value="${order.payment.method}" /></td></tr>
	<tr><th>결제상태</th>
		<td>
			<c:out value="${order.payment.payStatus}" />
			<c:if test="${order.payment.payStatus == '환불'}">
				(<fmt:formatDate value="${order.payment.refundedAt}" pattern="yyyy-MM-dd HH:mm" />)
			</c:if>
		</td>
	</tr>
	<tr><th>상품 합계</th><td><fmt:formatNumber value="${order.totalPrice}" pattern="#,##0" />원</td></tr>
	<tr><th>배송비</th><td><fmt:formatNumber value="${order.deliveryFee}" pattern="#,##0" />원</td></tr>
	<tr><th>결제금액</th><td><fmt:formatNumber value="${order.payAmount}" pattern="#,##0" />원</td></tr>
</table>

<h3>배송 조회</h3>
<c:choose>
	<c:when test="${empty order.delivery}">
		<p class="empty">배송 정보가 없습니다.</p>
	</c:when>
	<c:otherwise>
		<table class="formtable">
			<tr><th>배송상태</th><td><c:out value="${order.delivery.status}" /></td></tr>
			<tr><th>택배사</th><td><c:out value="${empty order.delivery.courier ? '-' : order.delivery.courier}" /></td></tr>
			<tr>
				<th>운송장번호</th>
				<%-- 표시만 한다. 외부 택배사 조회는 범위 외다 (CLAUDE.md § 5). --%>
				<td><c:out value="${empty order.delivery.trackingNo ? '-' : order.delivery.trackingNo}" /></td>
			</tr>
		</table>

		<%--
		  배송 타임라인 — delivery_history 가 유일한 근거다.
		  is_forced 는 고객에게 보여주지 않는다 (CRUD.md § 6-1).
		--%>
		<ol class="order_timeline">
			<c:forEach var="h" items="${order.delivery.history}">
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
		<tr><th class="col_name">상품</th><th class="col_price">단가</th><th class="col_qty">수량</th>
		    <th class="col_total">합계</th><th class="col_action">리뷰</th></tr>
	</thead>
	<tbody>
		<c:forEach var="item" items="${order.items}">
			<tr>
				<td class="col_name">
					<a href="${ctx}/product/view?productId=${item.productId}">
						<c:out value="${item.productName}" />
					</a>
				</td>
				<td class="col_price"><fmt:formatNumber value="${item.price}" pattern="#,##0" />원</td>
				<td class="col_qty">${item.quantity}</td>
				<td class="col_total"><fmt:formatNumber value="${item.lineTotal}" pattern="#,##0" />원</td>
				<td class="col_action">
					<%--
					  리뷰 작성은 강제가 아니다. 배송완료 + 미작성일 때만 버튼을 낸다
					  (CRUD.md § 6-3 — OrderItemDTO.reviewWritten).
					--%>
					<c:choose>
						<c:when test="${item.reviewWritten}">작성 완료</c:when>
						<c:when test="${not empty order.delivery and order.delivery.status == '배송완료'}">
							<button type="button" class="btn btn_small"
							        onclick="openReview(${item.itemId}, '<c:out value="${item.productName}" />')">리뷰 쓰기</button>
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
	<%-- 취소 가능 판정은 DTO 가 들고 있다 — 배송이 시작되기 전까지만 (ERD § 6.3). --%>
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

<script>
function openReview(itemId, productName) {
	document.getElementById("reviewItemId").value = itemId;
	document.getElementById("reviewProductName").textContent = productName;
	window.openPopup("reviewPopup");
}
</script>
<%@ include file="/common/footer.jsp" %>
