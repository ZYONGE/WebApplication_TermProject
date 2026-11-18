<%@ include file="../common/header.jsp" %>
<%@ include file="../common/devnav.jsp" %>
<h2>dev · 계정 전환</h2>
<p class="field_help">
	전환하면 세션이 대상 계정으로 완전히 바뀝니다. 전환 중에는 /dev 화면도 막히고,
	상단의 '원래 계정으로'로만 돌아올 수 있습니다. 전환 중 조작 기록에는 impersonated_by 가 붙습니다.
	탈퇴 회원과 개발자 계정은 전환 대상이 아닙니다.
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
					<c:if test="${m.status == 'ACTIVE' and m.role != 'DEV'}">
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
<%@ include file="../common/footer.jsp" %>
