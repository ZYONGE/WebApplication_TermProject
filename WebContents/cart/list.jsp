<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<c:set var="pageCss" value="cart" />
<%@ include file="/common/header.jsp" %>

<h2>장바구니</h2>

<%--
  담아 둔 줄은 품절 · 숨김이어도 보여준다 (CRUD.md § 2 주의).
  주문 대상에서만 빼고 그 사실을 화면에 적는다 — 조용히 사라지면
  사용자는 자기가 담은 것이 왜 없는지 알 수 없다.
--%>
<c:if test="${blockedCount > 0}">
	<p class="toast toast_error">
		주문할 수 없는 상품이 ${blockedCount}건 있습니다. 합계에서 제외했습니다.
	</p>
</c:if>

<table class="listtable cart_table">
	<thead class="listtable_head">
		<tr>
			<th class="col_name">상품</th>
			<th class="col_price">단가</th>
			<th class="col_qty">수량</th>
			<th class="col_total">합계</th>
			<th class="col_state">상태</th>
			<th class="col_action">삭제</th>
		</tr>
	</thead>
	<tbody>
		<c:forEach var="item" items="${items}">
			<tr class="${item.orderable ? '' : 'soldout'}">
				<td class="col_name">
					<a href="${ctx}/product/view?productId=${item.productId}">
						<c:out value="${item.productName}" />
					</a>
				</td>
				<td class="col_price"><fmt:formatNumber value="${item.price}" pattern="#,##0" />원</td>
				<td class="col_qty">
					<form action="${ctx}/cart/update" method="post">
						<input type="hidden" name="cartId" value="${item.cartId}">
						<input type="number" name="quantity" value="${item.quantity}" min="1" max="99">
						<button type="submit" class="btn btn_small">변경</button>
					</form>
				</td>
				<%-- 주문 대상에서 빠진 줄은 합계에 들어가지 않는다 — 금액을 보이면 합산된 것으로 읽힌다 --%>
				<td class="col_total">
					<c:choose>
						<c:when test="${item.orderable}"><fmt:formatNumber value="${item.lineTotal}" pattern="#,##0" />원</c:when>
						<c:otherwise>- (합계 제외)</c:otherwise>
					</c:choose>
				</td>
				<td class="col_state">
					<c:choose>
						<c:when test="${item.status != '판매중'}">판매 중지</c:when>
						<c:when test="${item.stock <= 0}">품절</c:when>
						<c:when test="${item.stock < item.quantity}">재고 ${item.stock}개</c:when>
						<c:otherwise>주문 가능</c:otherwise>
					</c:choose>
				</td>
				<td class="col_action">
					<form action="${ctx}/cart/delete" method="post">
						<input type="hidden" name="cartId" value="${item.cartId}">
						<button type="submit" class="btn btn_danger btn_small"
						        data-confirm-message="장바구니에서 빼시겠습니까?">삭제</button>
					</form>
				</td>
			</tr>
		</c:forEach>
		<c:if test="${empty items}">
			<tr><td colspan="6" class="empty">장바구니가 비어 있습니다.</td></tr>
		</c:if>
	</tbody>
</table>

<table class="formtable cart_sum">
	<tr><th>상품 합계</th><td><fmt:formatNumber value="${totalPrice}" pattern="#,##0" />원</td></tr>
	<tr>
		<th>배송비</th>
		<td>
			<fmt:formatNumber value="${deliveryFee}" pattern="#,##0" />원
			<span class="field_help">
				<fmt:formatNumber value="${freeShippingMin}" pattern="#,##0" />원 이상 무료배송
			</span>
		</td>
	</tr>
	<tr><th>결제 예정 금액</th><td><fmt:formatNumber value="${payAmount}" pattern="#,##0" />원</td></tr>
</table>

<div class="btnstyle">
	<a class="btn btn_sub" href="${ctx}/product/list">계속 쇼핑</a>
	<c:if test="${totalPrice > 0}">
		<a class="btn btn_primary" href="${ctx}/order/form">주문하기</a>
	</c:if>
</div>
<%@ include file="/common/footer.jsp" %>
