<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<c:set var="pageCss" value="admin" />
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
		    <th>권한</th><th>상태</th><th>가입일</th><th>처리</th></tr>
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
					  역할은 바꿀 수 없다 (ERD § 5.3). 탈퇴 처리만 있고, 대상은 활동 중인 USER 뿐이다.
					  탈퇴 회원 · 운영 계정에는 폼을 내지 않는다. 서버도 같은 조건으로 거부한다.
					--%>
					<c:choose>
						<c:when test="${m.status == 'LEFT'}">탈퇴</c:when>
						<c:when test="${m.role != 'USER'}">-</c:when>
						<c:otherwise>
							<form action="${ctx}/admin/members" method="post">
								<input type="hidden" name="memberId" value="${m.memberId}">
								<button type="submit" class="btn btn_danger btn_small"
								        data-confirm-message="이 회원을 탈퇴 처리하시겠습니까? 되돌릴 수 없습니다.">탈퇴 처리</button>
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
