<%@ include file="../common/header.jsp" %>
<h2>장바구니</h2>
<p class="field_help">수량은 섭취 일수입니다. 1포 = 하루 섭취분이라 N일분은 N포입니다. 한 줄에 최대 ${maxDays}일까지 담을 수 있습니다.</p>
<c:if test="${blockedCount > 0}">
  <p class="toast toast_error">주문할 수 없는 상품이 ${blockedCount}건 있습니다. 합계에서 제외했습니다.</p>
</c:if>
<%@ include file="../common/warnbox.jsp" %>
<c:if test="${mixedDays}">
  <p class="cart_notice">섭취 일수가 서로 다른 상품이 있습니다. 같은 날짜에 함께 다 드시려면 일수를 맞춰 주세요.</p>
</c:if>
<table class="listtable cart_table">
  <thead class="listtable_head">
    <tr><th class="col_name">상품</th><th class="col_price">1포 가격</th><th class="col_qty">섭취 일수</th>
        <th class="col_total">합계</th><th class="col_state">상태</th><th class="col_action">삭제</th></tr>
  </thead>
  <tbody>
    <c:forEach var="item" items="${items}">
      <tr class="${item.orderable ? '' : 'soldout'}">
        <td class="col_name">
          <span class="prodcard_badge">${item.typeLabel}</span>
          <a href="${ctx}/product/view?productId=${item.productId}"><c:out value="${item.productName}" /></a>
        </td>
        <td class="col_price"><fmt:formatNumber value="${item.price}" pattern="#,##0" />원</td>
        <td class="col_qty">
          <form action="${ctx}/cart/update" method="post">
            <input type="hidden" name="cartId" value="${item.cartId}">
            <input type="number" name="quantity" value="${item.quantity}" min="1" max="${maxDays}">일
            <button type="submit" class="btn btn_small">변경</button>
            <span class="field_help">${item.quantity}포</span>
          </form>
        </td>
        <td class="col_total">
          <c:if test="${item.orderable}"><fmt:formatNumber value="${item.lineTotal}" pattern="#,##0" />원</c:if>
          <c:if test="${not item.orderable}">- (합계 제외)</c:if>
        </td>
        <td class="col_state">${item.cartState}</td>
        <td class="col_action">
          <form action="${ctx}/cart/delete" method="post">
            <input type="hidden" name="cartId" value="${item.cartId}">
            <button type="submit" class="btn btn_danger btn_small" data-confirm-message="장바구니에서 빼시겠습니까?">삭제</button>
          </form>
        </td>
      </tr>
    </c:forEach>
    <c:if test="${empty items}"><tr><td colspan="6" class="empty">장바구니가 비어 있습니다.</td></tr></c:if>
  </tbody>
</table>
<table class="formtable cart_sum">
  <tr><th>상품 합계</th><td><fmt:formatNumber value="${totalPrice}" pattern="#,##0" />원</td></tr>
  <tr>
    <th>배송비</th>
    <td><fmt:formatNumber value="${deliveryFee}" pattern="#,##0" />원
      <span class="field_help"><fmt:formatNumber value="${freeShippingMin}" pattern="#,##0" />원 이상 무료배송</span></td>
  </tr>
  <tr><th>결제 예정 금액</th><td><fmt:formatNumber value="${payAmount}" pattern="#,##0" />원</td></tr>
</table>
<div class="btnstyle">
  <a class="btn btn_sub" href="${ctx}/product/list">계속 쇼핑</a>
  <a class="btn btn_sub" href="${ctx}/custom">조합 맞추기</a>
  <c:if test="${totalPrice > 0}"><a class="btn btn_primary" href="${ctx}/order/form">주문하기</a></c:if>
</div>
<%@ include file="../common/footer.jsp" %>
