<%@ include file="../common/header.jsp" %>
<%@ include file="../common/devnav.jsp" %>
<h2>dev · 감사 로그</h2>
<form class="admin_filter" action="${ctx}/dev/log" method="get">
	<select name="mode">
		<option value="">전체 모드</option>
		<option value="ADMIN" ${param.mode == 'ADMIN' ? 'selected' : ''}>ADMIN</option>
		<option value="DEV" ${param.mode == 'DEV' ? 'selected' : ''}>DEV</option>
	</select>
	<select name="action">
		<option value="">전체 동작</option>
		<c:forEach var="a" items="${actions}">
			<option value="${a}" ${param.action == a ? 'selected' : ''}>${a}</option>
		</c:forEach>
	</select>
	<select name="targetTable">
		<option value="">전체 대상</option>
		<c:forEach var="t" items="${tables}">
			<option value="${t}" ${param.targetTable == t ? 'selected' : ''}>${t}</option>
		</c:forEach>
	</select>
	<input type="date" name="fromDate" value="<c:out value='${param.fromDate}' />">
	<input type="date" name="toDate" value="<c:out value='${param.toDate}' />">
	<button type="submit" class="btn">조회</button>
</form>
<table class="listtable">
	<thead class="listtable_head">
		<tr><th>번호</th><th>시각</th><th>행위자</th><th>모드</th>
		    <th>동작</th><th>대상</th><th>상세</th></tr>
	</thead>
	<tbody>
		<c:forEach var="l" items="${pageData.list}">
			<tr>
				<td>${l.logId}</td>
				<td><fmt:formatDate value="${l.createdAt}" pattern="yyyy-MM-dd HH:mm:ss" /></td>
				<td><c:out value="${l.memberName}" /> (${l.memberId})</td>
				<td><c:out value="${l.mode}" /></td>
				<td><c:out value="${l.action}" /></td>
				<td>
					<c:out value="${l.targetTable}" />
					<c:if test="${not empty l.targetId}">#${l.targetId}</c:if>
				</td>
				<td><c:out value="${l.detail}" /></td>
			</tr>
		</c:forEach>
		<c:if test="${empty pageData.list}"><tr><td colspan="7" class="empty">조회된 로그가 없습니다.</td></tr></c:if>
	</tbody>
</table>
<%@ include file="../common/paging.jsp" %>
<%@ include file="../common/footer.jsp" %>
