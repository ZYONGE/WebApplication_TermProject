<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ include file="/common/header.jsp" %>

<ul class="admin_nav">
	<li><a href="${ctx}/dev/">dev 허브</a></li>
	<li><a href="${ctx}/dev/state">상태 컨트롤러</a></li>
	<li><a href="${ctx}/dev/data">데이터 브라우저</a></li>
	<li><a href="${ctx}/dev/erd">설계 보기</a></li>
	<li><a href="${ctx}/dev/seed">데이터 관리</a></li>
	<li><a href="${ctx}/dev/loginAs">계정 전환</a></li>
	<li><a href="${ctx}/dev/log">감사 로그</a></li>
	<li><a href="${ctx}/admin/dashboard">관리자로</a></li>
</ul>

<h2>dev · 감사 로그</h2>

<%--
  CRUD.md § 6-2 로 추가된 화면이다. 이 화면이 생기면서 admin_log.detail 이
  처음으로 쓰인다 — 그 전에는 범용 테이블 덤프뿐이라 필터도 추적도 불가능했다.
  정렬은 created_at DESC 고정이다.
--%>
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
				<%-- LOGIN_AS 의 detail 에 impersonated_by 가 들어 있다 --%>
				<td><c:out value="${l.detail}" /></td>
			</tr>
		</c:forEach>
		<c:if test="${empty pageData.list}"><tr><td colspan="7" class="empty">조회된 로그가 없습니다.</td></tr></c:if>
	</tbody>
</table>

<%@ include file="/common/paging.jsp" %>
<%@ include file="/common/footer.jsp" %>
