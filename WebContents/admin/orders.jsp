<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<c:set var="pageCss" value="admin" />
<%@ include file="/common/header.jsp" %>

<ul class="admin_nav">
	<li><a href="${ctx}/admin/dashboard">대시보드</a></li>
	<li><a href="${ctx}/admin/products">상품</a></li>
	<li><a href="${ctx}/admin/categories">카테고리</a></li>
	<li><a href="${ctx}/admin/orders">주문·배송</a></li>
	<li><a href="${ctx}/admin/members">회원</a></li>
	<li><a href="${ctx}/admin/qna">문의답변</a></li>
	<li><a href="${ctx}/admin/reviews">리뷰</a></li>
	<li><a href="${ctx}/admin/notices">공지</a></li>
</ul>

<h2>주문 · 배송 관리</h2>

<form class="admin_filter" action="${ctx}/admin/orders" method="get">
	<input type="text" name="keyword" value="<c:out value='${param.keyword}' />"
	       placeholder="주문번호 · 아이디 · 받는사람">
	<select name="deliveryStatus">
		<option value="">전체 배송상태</option>
		<c:forEach var="s" items="${flow}">
			<option value="${s}" ${param.deliveryStatus == s ? 'selected' : ''}>${s}</option>
		</c:forEach>
	</select>
	<select name="orderStatus">
		<option value="">전체 주문상태</option>
		<option value="정상" ${param.orderStatus == '정상' ? 'selected' : ''}>정상</option>
		<option value="취소" ${param.orderStatus == '취소' ? 'selected' : ''}>취소</option>
	</select>
	<button type="submit" class="btn">검색</button>
</form>

<table class="listtable">
	<thead class="listtable_head">
		<tr><th>주문번호</th><th>주문일</th><th>회원</th><th>상품</th><th>결제금액</th>
		    <th>결제</th><th>주문</th><th>배송</th><th>처리</th></tr>
	</thead>
	<tbody>
		<c:forEach var="o" items="${pageData.list}">
			<tr class="${o.orderStatus == '취소' ? 'disabled' : ''}">
				<td><a href="${ctx}/admin/orders?orderId=${o.orderId}"><c:out value="${o.orderNo}" /></a></td>
				<td><fmt:formatDate value="${o.orderedAt}" pattern="yyyy-MM-dd HH:mm" /></td>
				<td><c:out value="${o.memberName}" /> (<c:out value="${o.loginId}" />)</td>
				<td><c:out value="${o.itemSummary}" /></td>
				<td><fmt:formatNumber value="${o.payAmount}" pattern="#,##0" />원</td>
				<td><c:out value="${empty o.payment ? '-' : o.payment.payStatus}" /></td>
				<td><c:out value="${o.orderStatus}" /></td>
				<td><c:out value="${empty o.delivery ? '-' : o.delivery.status}" /></td>
				<td>
					<%--
					  다음 단계 버튼. 배송중으로 넘어갈 때만 운송장 입력이 필요하므로
					  그 경우에는 팝업을 띄운다 (ERD § 6.2).
					--%>
					<c:if test="${not empty o.delivery and o.orderStatus == '정상'}">
						<c:choose>
							<c:when test="${o.delivery.status == '결제완료'}">
								<form action="${ctx}/admin/orders" method="post">
									<input type="hidden" name="returnQuery" value="<c:out value='${pageContext.request.queryString}' />">
									<input type="hidden" name="mode" value="next">
									<input type="hidden" name="deliveryId" value="${o.delivery.deliveryId}">
									<button type="submit" class="btn btn_small">상품준비로</button>
								</form>
							</c:when>
							<c:when test="${o.delivery.status == '상품준비'}">
								<button type="button" class="btn btn_small"
								        onclick="openShip(${o.delivery.deliveryId}, '<c:out value="${o.orderNo}" />')">배송중으로</button>
							</c:when>
							<c:when test="${o.delivery.status == '배송중'}">
								<form action="${ctx}/admin/orders" method="post">
									<input type="hidden" name="returnQuery" value="<c:out value='${pageContext.request.queryString}' />">
									<input type="hidden" name="mode" value="next">
									<input type="hidden" name="deliveryId" value="${o.delivery.deliveryId}">
									<button type="submit" class="btn btn_small">배송완료로</button>
								</form>
							</c:when>
							<c:otherwise>완료</c:otherwise>
						</c:choose>

						<c:if test="${o.cancelable}">
							<form action="${ctx}/admin/orders" method="post">
								<input type="hidden" name="returnQuery" value="<c:out value='${pageContext.request.queryString}' />">
									<input type="hidden" name="mode" value="cancel">
								<input type="hidden" name="orderId" value="${o.orderId}">
								<button type="submit" class="btn btn_danger btn_small"
								        data-confirm-message="주문을 취소하시겠습니까? 재고가 복구됩니다.">취소</button>
							</form>
						</c:if>
					</c:if>
				</td>
			</tr>
		</c:forEach>
		<c:if test="${empty pageData.list}"><tr><td colspan="9" class="empty">주문이 없습니다.</td></tr></c:if>
	</tbody>
</table>

<%@ include file="/common/paging.jsp" %>

<c:if test="${not empty detail}">
<h3>주문 상세 — <c:out value="${detail.orderNo}" /></h3>
<table class="formtable">
	<tr><th>받는 사람</th><td><c:out value="${detail.receiver}" /> (<c:out value="${detail.receiverPhone}" />)</td></tr>
	<tr><th>배송지</th><td>[<c:out value="${detail.zipcode}" />] <c:out value="${detail.addr1}" /> <c:out value="${detail.addr2}" /></td></tr>
	<tr><th>요청사항</th><td><c:out value="${detail.deliveryMemo}" /></td></tr>
	<tr><th>운송장</th><td><c:out value="${detail.delivery.courier}" /> <c:out value="${detail.delivery.trackingNo}" /></td></tr>
</table>
<table class="listtable">
	<thead class="listtable_head"><tr><th>상품</th><th>단가</th><th>수량</th><th>합계</th></tr></thead>
	<tbody>
		<c:forEach var="item" items="${detail.items}">
			<tr>
				<td><c:out value="${item.productName}" /></td>
				<td><fmt:formatNumber value="${item.price}" pattern="#,##0" /></td>
				<td>${item.quantity}</td>
				<td><fmt:formatNumber value="${item.lineTotal}" pattern="#,##0" /></td>
			</tr>
		</c:forEach>
	</tbody>
</table>
<h4>배송 이력</h4>
<ol>
	<c:forEach var="h" items="${detail.delivery.history}">
		<li>
			<fmt:formatDate value="${h.changedAt}" pattern="yyyy-MM-dd HH:mm" />
			<c:out value="${empty h.prevStatus ? '(최초)' : h.prevStatus}" /> &rarr; <c:out value="${h.newStatus}" />
			<c:if test="${h.isForced == 'Y'}"> [강제]</c:if>
			<c:if test="${not empty h.changedByName}"> / <c:out value="${h.changedByName}" /></c:if>
		</li>
	</c:forEach>
</ol>
</c:if>

<dialog id="shipPopup" class="popup">
	<form class="popup_form" action="${ctx}/admin/orders" method="post" data-validate>
		<input type="hidden" name="returnQuery" value="<c:out value='${pageContext.request.queryString}' />">
									<input type="hidden" name="mode" value="next">
		<input type="hidden" name="deliveryId" id="shipDeliveryId">

		<h3 class="popup_head">배송중으로 — 운송장 입력</h3>
		<div class="popup_body">
			<p id="shipOrderNo"></p>
			<%-- 운송장 없이 배송중으로 넘어가면 chk_delivery_tracking CHECK 에 걸린다 --%>
			<label for="courier">택배사</label>
			<input type="text" id="courier" name="courier" class="field" data-rule="required">
			<label for="trackingNo">운송장 번호</label>
			<input type="text" id="trackingNo" name="trackingNo" class="field" data-rule="required">
			<span class="field_help">외부 택배사 조회는 하지 않습니다. 화면 표시용입니다.</span>
		</div>
		<div class="popup_foot">
			<button type="submit" class="btn btn_primary">배송중으로 변경</button>
			<button type="button" class="btn btn_sub popup_close" data-popup-close="shipPopup">닫기</button>
		</div>
	</form>
</dialog>

<script>
function openShip(deliveryId, orderNo) {
	document.getElementById("shipDeliveryId").value = deliveryId;
	document.getElementById("shipOrderNo").textContent = "주문번호 " + orderNo;
	window.openPopup("shipPopup");
}
</script>
<%@ include file="/common/footer.jsp" %>
