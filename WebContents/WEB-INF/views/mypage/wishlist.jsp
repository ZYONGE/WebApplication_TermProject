<%@ include file="../common/header.jsp" %>
<%@ include file="../common/mynav.jsp" %>
<h2>찜 목록</h2>
<table class="listtable">
	<thead class="listtable_head">
		<tr><th class="col_name">상품</th><th class="col_price">1포 가격</th>
		    <th class="col_state">상태</th><th class="col_date">찜한 날</th><th class="col_action"></th></tr>
	</thead>
	<tbody>
		<c:forEach var="w" items="${items}">
			<tr class="${w.stateLabel == '판매중' ? '' : 'soldout'}">
				<td class="col_name">
					<span class="prodcard_badge">${w.typeLabel}</span>
					<a href="${ctx}/product/view?productId=${w.productId}"><c:out value="${w.productName}" /></a>
				</td>
				<td class="col_price"><fmt:formatNumber value="${w.price}" pattern="#,##0" />원</td>
				<td class="col_state"><c:out value="${w.stateLabel}" /></td>
				<td class="col_date"><fmt:formatDate value="${w.createdAt}" pattern="yyyy-MM-dd" /></td>
				<td class="col_action">
					<form action="${ctx}/wish/remove" method="post">
						<input type="hidden" name="productId" value="${w.productId}">
						<input type="hidden" name="back" value="/mypage/wishlist">
						<button type="submit" class="btn btn_small">찜 해제</button>
					</form>
				</td>
			</tr>
		</c:forEach>
		<c:if test="${empty items}">
			<tr><td colspan="5" class="empty">찜한 상품이 없습니다.</td></tr>
		</c:if>
	</tbody>
</table>
<%@ include file="../common/footer.jsp" %>
