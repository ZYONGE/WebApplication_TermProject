<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<c:set var="pageCss" value="member" />
<%@ include file="/common/header.jsp" %>

<h2>로그인</h2>

<form class="formtable_form" action="${ctx}/member/login" method="post" data-validate>
	<input type="hidden" name="returnUrl" value="<c:out value='${returnUrl}' />">
	<table class="formtable">
		<tr>
			<th><label for="loginId">아이디</label></th>
			<td>
				<input type="text" id="loginId" name="loginId" class="field"
				       value="<c:out value='${loginId}' />" data-rule="required" autofocus>
			</td>
		</tr>
		<tr>
			<th><label for="password">비밀번호</label></th>
			<td><input type="password" id="password" name="password" class="field" data-rule="required"></td>
		</tr>
	</table>
	<div class="btnstyle">
		<button type="submit" class="btn btn_primary">로그인</button>
		<a class="btn btn_sub" href="${ctx}/member/join">회원가입</a>
	</div>
</form>

<%-- 샌드박스 더미 계정 — data.sql 이 넣어 둔 값이다. 제출물에서는 지운다. --%>
<p class="field_help">
	테스트 계정: admin / devops / hong / kim (비밀번호는 모두 test1234)
</p>
<%@ include file="/common/footer.jsp" %>
