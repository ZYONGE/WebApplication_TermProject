<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<c:set var="pageCss" value="order" />
<%@ include file="/common/header.jsp" %>

<h2>주문이 완료되었습니다</h2>

<table class="formtable">
	<tr><th>주문번호</th><td><c:out value="${order.orderNo}" /></td></tr>
	<tr><th>주문일시</th><td><fmt:formatDate value="${order.orderedAt}" pattern="yyyy-MM-dd HH:mm" /></td></tr>
	<tr><th>결제수단</th><td><c:out value="${order.payment.method}" /></td></tr>
	<tr><th>결제금액</th><td><fmt:formatNumber value="${order.payAmount}" pattern="#,##0" />원</td></tr>
	<tr><th>배송상태</th><td><c:out value="${order.delivery.status}" /></td></tr>
	<tr>
		<th>배송지</th>
		<td>
			[<c:out value="${order.zipcode}" />]
			<c:out value="${order.addr1}" /> <c:out value="${order.addr2}" />
			/ <c:out value="${order.receiver}" /> (<c:out value="${order.receiverPhone}" />)
		</td>
	</tr>
</table>

<table class="listtable">
	<thead class="listtable_head">
		<tr><th class="col_name">상품</th><th class="col_price">단가</th>
		    <th class="col_qty">수량</th><th class="col_total">합계</th></tr>
	</thead>
	<tbody>
		<c:forEach var="item" items="${order.items}">
			<tr>
				<td class="col_name"><c:out value="${item.productName}" /></td>
				<td class="col_price"><fmt:formatNumber value="${item.price}" pattern="#,##0" />원</td>
				<td class="col_qty">${item.quantity}</td>
				<td class="col_total"><fmt:formatNumber value="${item.lineTotal}" pattern="#,##0" />원</td>
			</tr>
		</c:forEach>
	</tbody>
</table>

<div class="btnstyle">
	<a class="btn btn_primary" href="${ctx}/order/view?orderId=${order.orderId}">주문 상세</a>
	<a class="btn btn_sub" href="${ctx}/product/list">계속 쇼핑</a>
</div>
<%@ include file="/common/footer.jsp" %>
