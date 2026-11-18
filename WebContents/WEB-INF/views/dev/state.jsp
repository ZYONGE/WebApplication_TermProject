<%@ include file="../common/header.jsp" %>
<%@ include file="../common/devnav.jsp" %>
<h2>dev · 상태 컨트롤러</h2>
<h3>점검 모드 · 전역 설정</h3>
<form action="${ctx}/dev/state" method="post">
	<input type="hidden" name="mode" value="shop">
	<button type="submit" class="btn btn_primary">점검 모드 토글</button>
</form>
<table class="listtable">
	<thead class="listtable_head"><tr><th>키</th><th>현재 값</th><th>변경</th></tr></thead>
	<tbody>
		<c:forEach var="cfg" items="${configs}">
			<tr>
				<td><c:out value="${cfg.configKey}" /><br><span class="field_help"><c:out value="${cfg.description}" /></span></td>
				<td><strong><c:out value="${cfg.configValue}" /></strong></td>
				<td>
					<form action="${ctx}/dev/state" method="post">
						<input type="hidden" name="mode" value="config">
						<input type="hidden" name="configKey" value="${cfg.configKey}">
						<input type="text" name="configValue" value="<c:out value='${cfg.configValue}' />">
						<button type="submit" class="btn btn_small">변경</button>
					</form>
				</td>
			</tr>
		</c:forEach>
	</tbody>
</table>
<h3>재고 강제 지정</h3>
<form class="formtable_form" action="${ctx}/dev/state" method="post">
	<input type="hidden" name="mode" value="stock">
	<select name="productId">
		<c:forEach var="p" items="${products}">
			<option value="${p.productId}">
				[${p.productId}] <c:out value="${p.name}" /> (현재 ${p.stock}포 · <c:out value="${p.status}" />)
			</option>
		</c:forEach>
	</select>
	<input type="number" name="stock" value="0" min="0">
	<button type="submit" class="btn">재고 설정</button>
</form>
<h3>주문별 강제 조작</h3>
<form class="admin_filter" action="${ctx}/dev/state" method="get">
	<input type="text" name="keyword" value="<c:out value='${param.keyword}' />" placeholder="주문번호 · 아이디">
	<button type="submit" class="btn">검색</button>
</form>
<table class="listtable">
	<thead class="listtable_head">
		<tr><th>주문번호</th><th>회원</th><th>주문상태</th><th>결제상태</th><th>배송상태</th><th>강제 변경</th></tr>
	</thead>
	<tbody>
		<c:forEach var="o" items="${pageData.list}">
			<tr>
				<td><a href="${ctx}/dev/state?orderId=${o.orderId}"><c:out value="${o.orderNo}" /></a></td>
				<td><c:out value="${o.loginId}" /></td>
				<td><c:out value="${o.orderStatus}" /></td>
				<td><c:out value="${empty o.payStatus ? '-' : o.payStatus}" /></td>
				<td><c:out value="${empty o.deliveryStatus ? '-' : o.deliveryStatus}" /></td>
				<td>
					<c:if test="${not empty o.deliveryId}">
						<form action="${ctx}/dev/state" method="post">
							<input type="hidden" name="mode" value="delivery">
							<input type="hidden" name="deliveryId" value="${o.deliveryId}">
							<select name="status">
								<c:forEach var="s" items="${flow}">
									<option value="${s}" ${o.deliveryStatus == s ? 'selected' : ''}>${s}</option>
								</c:forEach>
							</select>
							<input type="text" name="courier" placeholder="택배사" size="8">
							<input type="text" name="trackingNo" placeholder="운송장" size="10">
							<button type="submit" class="btn btn_small">배송 강제</button>
						</form>
					</c:if>
					<form action="${ctx}/dev/state" method="post">
						<input type="hidden" name="mode" value="order">
						<input type="hidden" name="orderId" value="${o.orderId}">
						<select name="status">
							<option value="정상" ${o.orderStatus == '정상' ? 'selected' : ''}>정상</option>
							<option value="취소" ${o.orderStatus == '취소' ? 'selected' : ''}>취소</option>
						</select>
						<label><input type="checkbox" name="restoreStock" value="Y" checked> 재고 반영</label>
						<button type="submit" class="btn btn_small">주문 강제</button>
					</form>
					<form action="${ctx}/dev/state" method="post">
						<input type="hidden" name="mode" value="pay">
						<input type="hidden" name="orderId" value="${o.orderId}">
						<select name="status">
							<option value="완료">완료</option>
							<option value="환불">환불</option>
						</select>
						<button type="submit" class="btn btn_small">결제 강제</button>
					</form>
				</td>
			</tr>
		</c:forEach>
		<c:if test="${empty pageData.list}"><tr><td colspan="6" class="empty">주문이 없습니다.</td></tr></c:if>
	</tbody>
</table>
<%@ include file="../common/paging.jsp" %>
<c:if test="${not empty detail}">
<h3>배송 이력 — <c:out value="${detail.orderNo}" /></h3>
<table class="listtable">
	<thead class="listtable_head">
		<tr><th>시각</th><th>이전</th><th>이후</th><th>강제</th><th>변경자</th></tr>
	</thead>
	<tbody>
		<c:forEach var="h" items="${detail.history}">
			<tr class="${h.isForced == 'Y' ? 'on' : ''}">
				<td><fmt:formatDate value="${h.changedAt}" pattern="yyyy-MM-dd HH:mm:ss" /></td>
				<td><c:out value="${empty h.prevStatus ? '(최초)' : h.prevStatus}" /></td>
				<td><c:out value="${h.newStatus}" /></td>
				<td>${h.isForced}</td>
				<td><c:out value="${empty h.changedByName ? '시스템' : h.changedByName}" /></td>
			</tr>
		</c:forEach>
	</tbody>
</table>
</c:if>
<%@ include file="../common/footer.jsp" %>
