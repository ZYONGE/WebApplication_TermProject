<%@ include file="../common/header.jsp" %>
<%@ include file="../common/mynav.jsp" %>
<h2>마이페이지</h2>
<table class="formtable">
	<tr><th>아이디</th><td><c:out value="${member.loginId}" /></td></tr>
	<tr><th>이름</th><td><c:out value="${member.name}" /></td></tr>
	<tr><th>이메일</th><td><c:out value="${member.email}" /></td></tr>
	<tr><th>연락처</th><td><c:out value="${member.phone}" /></td></tr>
	<tr>
		<th>주소</th>
		<td>
			<c:if test="${not empty mainAddr}">
					[<c:out value="${mainAddr.zipcode}" />] <c:out value="${mainAddr.addr1}" /> <c:out value="${mainAddr.addr2}" />
					<span class="field_help">
						<c:out value="${mainAddr.addrName}" /><c:if test="${mainAddr.isDefault == 'Y'}"> (기본 배송지)</c:if>
						· 받는 사람 <c:out value="${mainAddr.receiver}" />
						· 등록된 배송지 ${addressCount}곳 <a href="${ctx}/mypage/address">관리</a>
					</span>
			</c:if>
			<c:if test="${empty mainAddr}">
					등록된 주소가 없습니다. <a href="${ctx}/mypage/address">배송지 등록</a></c:if>
		</td>
	</tr>
	<tr><th>가입일</th><td><fmt:formatDate value="${member.createdAt}" pattern="yyyy-MM-dd" /></td></tr>
	<tr><th>찜한 상품</th><td>${wishCount}개</td></tr>
</table>
<h3>최근 주문</h3>
<table class="listtable">
	<thead class="listtable_head">
		<tr><th class="col_no">주문번호</th><th class="col_date">주문일</th>
		    <th class="col_total">결제금액</th><th class="col_state">배송상태</th></tr>
	</thead>
	<tbody>
		<c:forEach var="order" items="${recentOrders}">
			<tr>
				<td class="col_no">
					<a href="${ctx}/order/view?orderId=${order.orderId}"><c:out value="${order.orderNo}" /></a>
				</td>
				<td class="col_date"><fmt:formatDate value="${order.orderedAt}" pattern="yyyy-MM-dd" /></td>
				<td class="col_total"><fmt:formatNumber value="${order.payAmount}" pattern="#,##0" />원</td>
				<td class="col_state"><c:out value="${order.displayStatus}" /></td>
			</tr>
		</c:forEach>
		<c:if test="${empty recentOrders}">
			<tr><td colspan="4" class="empty">주문 내역이 없습니다.</td></tr>
		</c:if>
	</tbody>
</table>
<h3>내가 쓴 글</h3>
<p>리뷰 ${reviewCount}건 / 문의 ${qnaCount}건</p>
<%@ include file="../common/footer.jsp" %>
