<%@ include file="../common/header.jsp" %>
<%@ include file="../common/mynav.jsp" %>
<h2>주문 내역</h2>
<table class="listtable">
	<thead class="listtable_head">
		<tr>
			<th class="col_no">주문번호</th>
			<th class="col_date">주문일</th>
			<th class="col_name">상품</th>
			<th class="col_total">결제금액</th>
			<th class="col_state">상태</th>
			<th class="col_action"></th>
		</tr>
	</thead>
	<tbody>
		<c:forEach var="order" items="${pageData.list}">
			<tr class="${order.orderStatus == '취소' ? 'disabled' : ''}">
				<td class="col_no"><c:out value="${order.orderNo}" /></td>
				<td class="col_date"><fmt:formatDate value="${order.orderedAt}" pattern="yyyy-MM-dd" /></td>
				<td class="col_name"><c:out value="${order.itemSummary}" /></td>
				<td class="col_total"><fmt:formatNumber value="${order.payAmount}" pattern="#,##0" />원</td>
				<td class="col_state"><c:out value="${order.displayStatus}" /></td>
				<td class="col_action">
					<a class="btn btn_small" href="${ctx}/order/view?orderId=${order.orderId}">상세</a>
				</td>
			</tr>
		</c:forEach>
		<c:if test="${empty pageData.list}">
			<tr><td colspan="6" class="empty">주문 내역이 없습니다.</td></tr>
		</c:if>
	</tbody>
</table>
<%@ include file="../common/paging.jsp" %>
<%@ include file="../common/footer.jsp" %>
