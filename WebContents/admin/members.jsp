<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ include file="/common/header.jsp" %>

<ul class="admin_nav">
	<li><a href="${ctx}/admin/dashboard">대시보드</a></li>
	<li><a href="${ctx}/admin/products">상품</a></li>
	<li><a href="${ctx}/admin/categories">카테고리</a></li>
	<li><a href="${ctx}/admin/orders">주문·배송</a></li>
	<li><a href="${ctx}/admin/members">회원</a></li>
	<li><a href="${ctx}/admin/qna">문의답변</a></li>
	<li><a href="${ctx}/admin/reviews">리뷰</a></li>
	<li><a href="${ctx}/admin/notices">공지</a></li>
</ul>

<h2>회원 관리</h2>

<form class="admin_filter" action="${ctx}/admin/members" method="get">
	<input type="text" name="keyword" value="<c:out value='${param.keyword}' />"
	       placeholder="아이디 · 이름 · 이메일">
	<select name="role">
		<option value="">전체 권한</option>
		<option value="USER" ${param.role == 'USER' ? 'selected' : ''}>USER</option>
		<option value="ADMIN" ${param.role == 'ADMIN' ? 'selected' : ''}>ADMIN</option>
		<option value="DEV" ${param.role == 'DEV' ? 'selected' : ''}>DEV</option>
	</select>
	<button type="submit" class="btn">검색</button>
</form>

<table class="listtable">
	<thead class="listtable_head">
		<tr><th>번호</th><th>아이디</th><th>이름</th><th>이메일</th><th>연락처</th>
		    <th>권한</th><th>상태</th><th>가입일</th><th>권한 변경</th></tr>
	</thead>
	<tbody>
		<c:forEach var="m" items="${pageData.list}">
			<tr class="${m.status == 'LEFT' ? 'disabled' : ''}">
				<td>${m.memberId}</td>
				<td><c:out value="${m.loginId}" /></td>
				<td><c:out value="${m.name}" /></td>
				<td><c:out value="${m.email}" /></td>
				<td><c:out value="${m.phone}" /></td>
				<td><c:out value="${m.role}" /></td>
				<td><c:out value="${m.status}" /></td>
				<td><fmt:formatDate value="${m.createdAt}" pattern="yyyy-MM-dd" /></td>
				<td>
					<%--
					  ★ DEV 는 선택지에 없다. ADMIN 은 DEV 권한을 줄 수 없고(ERD § 5),
					    DEV 계정의 권한도 바꿀 수 없다. 서버에서도 같은 판정을 한다 —
					    화면에서 빼는 것만으로는 막은 것이 아니다.
					--%>
					<c:choose>
						<c:when test="${m.role == 'DEV'}">변경 불가 (DEV)</c:when>
						<c:when test="${m.memberId == sessionScope.loginMember.memberId}">본인</c:when>
						<c:otherwise>
							<form action="${ctx}/admin/members" method="post">
								<input type="hidden" name="memberId" value="${m.memberId}">
								<select name="role">
									<option value="USER" ${m.role == 'USER' ? 'selected' : ''}>USER</option>
									<option value="ADMIN" ${m.role == 'ADMIN' ? 'selected' : ''}>ADMIN</option>
								</select>
								<button type="submit" class="btn btn_small"
								        data-confirm-message="권한을 변경하시겠습니까?">변경</button>
							</form>
						</c:otherwise>
					</c:choose>
				</td>
			</tr>
		</c:forEach>
		<c:if test="${empty pageData.list}"><tr><td colspan="9" class="empty">회원이 없습니다.</td></tr></c:if>
	</tbody>
</table>

<%@ include file="/common/paging.jsp" %>
<%@ include file="/common/footer.jsp" %>
