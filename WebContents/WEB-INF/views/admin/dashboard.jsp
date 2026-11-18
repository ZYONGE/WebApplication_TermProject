<%@ include file="../common/header.jsp" %>
<%@ include file="../common/adminnav.jsp" %>
<h2>대시보드</h2>
<table class="formtable">
	<tr><th>활동 회원</th><td>${summary.memberCount}명</td></tr>
	<tr><th>판매중 상품</th><td>${summary.productCount}개<c:if test="${summary.soldoutCount > 0}"> <span class="field_help">재고 0 으로 고객 화면에서 숨겨진 상품 ${summary.soldoutCount}개 별도</span></c:if></td></tr>
	<tr><th>정상 주문</th><td>${summary.orderCount}건</td></tr>
	<tr><th>오늘 주문</th><td>${summary.todayOrderCount}건 / <fmt:formatNumber value="${summary.todayAmount}" pattern="#,##0" />원</td></tr>
	<tr><th>미답변 문의</th><td><a href="${ctx}/admin/qna">${summary.qnaWaiting}건</a></td></tr>
</table>
<h3>배송 상태별 건수</h3>
<table class="listtable">
	<thead class="listtable_head"><tr><th>상태</th><th>건수</th></tr></thead>
	<tbody>
		<c:forEach var="d" items="${deliveryCounts}">
			<tr>
				<td><c:out value="${d.deliveryStatus}" /></td>
				<td><a href="${ctx}/admin/orders?deliveryStatus=${d.deliveryStatus}&orderStatus=정상">${d.orderCount}건</a></td>
			</tr>
		</c:forEach>
	</tbody>
</table>
<h3>최근 7일 매출</h3>
<table class="listtable">
	<thead class="listtable_head">
		<tr><th>일자</th><th>주문수</th><th>상품금액</th><th>배송비</th><th>합계</th></tr>
	</thead>
	<tbody>
		<c:forEach var="s" items="${salesDaily}">
			<tr>
				<td><fmt:formatDate value="${s.salesDate}" pattern="yyyy-MM-dd" /></td>
				<td>${s.orderCount}</td>
				<td><fmt:formatNumber value="${s.goodsAmount}" pattern="#,##0" /></td>
				<td><fmt:formatNumber value="${s.deliveryAmount}" pattern="#,##0" /></td>
				<td><fmt:formatNumber value="${s.totalAmount}" pattern="#,##0" /></td>
			</tr>
		</c:forEach>
		<c:if test="${empty salesDaily}"><tr><td colspan="5" class="empty">매출 데이터가 없습니다.</td></tr></c:if>
	</tbody>
</table>
<h3>재고 부족 (기준 ${lowStockThreshold}포 이하)</h3>
<table class="listtable">
	<thead class="listtable_head">
		<tr><th>상품</th><th>카테고리</th><th>재고</th><th>상태</th></tr>
	</thead>
	<tbody>
		<c:forEach var="p" items="${lowStock}">
			<tr class="${p.stock == 0 ? 'soldout' : ''}">
				<td><a href="${ctx}/admin/products?editId=${p.productId}"><c:out value="${p.name}" /></a></td>
				<td><c:out value="${p.categoryName}" /></td>
				<td>${p.stock}포<c:if test="${p.stock == 0}"> (고객 화면에서 숨겨짐)</c:if></td>
				<td><c:out value="${p.status}" /></td>
			</tr>
		</c:forEach>
		<c:if test="${empty lowStock}"><tr><td colspan="4" class="empty">재고 부족 상품이 없습니다.</td></tr></c:if>
	</tbody>
</table>
<%@ include file="../common/footer.jsp" %>
