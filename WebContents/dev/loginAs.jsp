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

<h2>dev · 계정 전환</h2>

<%--
  ★ 전환하면 그 뒤의 조작이 admin_log 에 "전환된 회원" 으로 찍힌다 (ERD § 8 쟁점 7).
    그래서 전환 시점 로그의 detail 에 impersonated_by 를 남기고,
    /dev/log 에서 그 값으로 실제 행위자를 되찾는다.
--%>
<p class="field_help">
	전환 후의 조작은 감사 로그에 전환된 계정으로 기록됩니다.
	전환 기록 자체에 원래 DEV 계정(impersonated_by)이 남으므로 /dev/log 에서 추적할 수 있습니다.
</p>

<table class="listtable">
	<thead class="listtable_head">
		<tr><th>번호</th><th>아이디</th><th>이름</th><th>권한</th><th>상태</th><th></th></tr>
	</thead>
	<tbody>
		<c:forEach var="m" items="${members}">
			<tr class="${m.status == 'LEFT' ? 'disabled' : ''}">
				<td>${m.memberId}</td>
				<td><c:out value="${m.loginId}" /></td>
				<td><c:out value="${m.name}" /></td>
				<td><c:out value="${m.role}" /></td>
				<td><c:out value="${m.status}" /></td>
				<td>
					<c:if test="${m.memberId != sessionScope.loginMember.memberId}">
						<form action="${ctx}/dev/loginAs" method="post">
							<input type="hidden" name="memberId" value="${m.memberId}">
							<button type="submit" class="btn btn_small"
							        data-confirm-message="이 계정으로 전환하시겠습니까?">전환</button>
						</form>
					</c:if>
				</td>
			</tr>
		</c:forEach>
	</tbody>
</table>

<c:if test="${not empty sessionScope.devOrigin}">
	<div class="btnstyle">
		<a class="btn btn_primary" href="${ctx}/dev/loginAs?action=restore">원래 계정으로 돌아가기</a>
	</div>
</c:if>
<%@ include file="/common/footer.jsp" %>
