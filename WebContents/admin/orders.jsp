<%@ include file="../common/header.jsp" %>
<%@ include file="../common/adminnav.jsp" %>
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
				<td><c:out value="${empty o.payStatus ? '-' : o.payStatus}" /></td>
				<td><c:out value="${o.orderStatus}" /></td>
				<td><c:out value="${o.displayStatus}" /></td>
				<td>
					<c:if test="${not empty o.deliveryId and o.orderStatus == '정상'}">
						<c:if test="${o.deliveryStatus == '결제완료' or o.deliveryStatus == '배송중'}">
							<form action="${ctx}/admin/orders" method="post">
								<input type="hidden" name="returnQuery" value="<c:out value='${pageContext.request.queryString}' />">
								<input type="hidden" name="mode" value="next">
								<input type="hidden" name="deliveryId" value="${o.deliveryId}">
								<button type="submit" class="btn btn_small">${o.deliveryStatus == '결제완료' ? '상품준비로' : '배송완료로'}</button>
							</form>
						</c:if>
						<c:if test="${o.deliveryStatus == '상품준비'}">
							<button type="button" class="btn btn_small" data-popup-open="shipPopup" data-fill-ship-delivery-id="${o.deliveryId}" data-fill-ship-order-no="주문번호 <c:out value='${o.orderNo}' />">배송중으로</button>
						</c:if>
						<c:if test="${o.deliveryStatus == '배송완료'}">완료</c:if>
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
<%@ include file="../common/paging.jsp" %>
<c:if test="${not empty detail}">
<h3>주문 상세 — <c:out value="${detail.orderNo}" /></h3>
<table class="formtable">
	<tr><th>받는 사람</th><td><c:out value="${detail.receiver}" /> (<c:out value="${detail.receiverPhone}" />)</td></tr>
	<tr><th>배송지</th><td>[<c:out value="${detail.zipcode}" />] <c:out value="${detail.addr1}" /> <c:out value="${detail.addr2}" /></td></tr>
	<tr><th>요청사항</th><td><c:out value="${detail.deliveryMemo}" /></td></tr>
	<tr><th>운송장</th><td><c:out value="${detail.courier}" /> <c:out value="${detail.trackingNo}" /></td></tr>
</table>
<table class="listtable">
	<thead class="listtable_head"><tr><th>상품</th><th>1포 가격</th><th>섭취 일수</th><th>합계</th></tr></thead>
	<tbody>
		<c:forEach var="item" items="${detail.items}">
			<tr>
				<td>
					<c:out value="${item.productName}" />
					<c:if test="${not empty item.trackSnapshot}"><span class="field_help">구성: <c:out value="${item.trackSnapshot}" /></span></c:if>
				</td>
				<td><fmt:formatNumber value="${item.price}" pattern="#,##0" /></td>
				<td>${item.quantity}일분</td>
				<td><fmt:formatNumber value="${item.lineTotal}" pattern="#,##0" /></td>
			</tr>
		</c:forEach>
	</tbody>
</table>
<h4>배송 이력</h4>
<ol>
	<c:forEach var="h" items="${detail.history}">
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
			<label for="courier">택배사</label>
			<input type="text" id="courier" name="courier" class="field" data-rule="required">
			<label for="trackingNo">운송장 번호</label>
			<input type="text" id="trackingNo" name="trackingNo" class="field" data-rule="required">
			<span class="field_help">입력한 택배사 · 운송장 번호가 고객의 배송조회에 표시됩니다.</span>
		</div>
		<div class="popup_foot">
			<button type="submit" class="btn btn_primary">배송중으로 변경</button>
			<button type="button" class="btn btn_sub popup_close" data-popup-close="shipPopup">닫기</button>
		</div>
	</form>
</dialog>
<%@ include file="../common/footer.jsp" %>
