<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ include file="/common/header.jsp" %>

<h2>회원가입</h2>

<form class="formtable_form" action="${ctx}/member/join" method="post" data-validate>
	<table class="formtable">
		<tr>
			<th><label for="loginId">아이디 <span class="required">*</span></label></th>
			<td>
				<input type="text" id="loginId" name="loginId" class="field"
				       value="<c:out value='${form.loginId}' />" data-rule="loginId">
				<button type="button" class="btn btn_small" onclick="checkLoginId()">중복확인</button>
				<span id="idCheckResult" class="field_help"></span>
				<span class="field_help">영문 · 숫자 · 밑줄 4~30자</span>
			</td>
		</tr>
		<tr>
			<th><label for="password">비밀번호 <span class="required">*</span></label></th>
			<td>
				<input type="password" id="password" name="password" class="field" data-rule="password">
				<span class="field_help">8자 이상</span>
			</td>
		</tr>
		<tr>
			<th><label for="passwordConfirm">비밀번호 확인 <span class="required">*</span></label></th>
			<td>
				<input type="password" id="passwordConfirm" name="passwordConfirm" class="field"
				       data-confirm="password">
			</td>
		</tr>
		<tr>
			<th><label for="name">이름 <span class="required">*</span></label></th>
			<td>
				<input type="text" id="name" name="name" class="field"
				       value="<c:out value='${form.name}' />" data-rule="name">
			</td>
		</tr>
		<tr>
			<th><label for="email">이메일 <span class="required">*</span></label></th>
			<td>
				<input type="text" id="email" name="email" class="field"
				       value="<c:out value='${form.email}' />" data-rule="email">
			</td>
		</tr>
		<tr>
			<th><label for="phone">연락처 <span class="required">*</span></label></th>
			<td>
				<input type="text" id="phone" name="phone" class="field"
				       value="<c:out value='${form.phone}' />" data-rule="phone">
				<span class="field_help">예: 010-1234-5678</span>
			</td>
		</tr>
	</table>
	<div class="btnstyle">
		<button type="submit" class="btn btn_primary">가입하기</button>
		<a class="btn btn_sub" href="${ctx}/">취소</a>
	</div>
</form>

<script>
/* 아이디 중복 확인 — 서버 판정을 그대로 받아 표시만 한다. */
function checkLoginId() {
	var input = document.getElementById("loginId");
	var result = document.getElementById("idCheckResult");
	var value = input.value.trim();
	if (value.length < 4) {
		result.textContent = "아이디를 4자 이상 입력하세요.";
		return;
	}
	var xhr = new XMLHttpRequest();
	xhr.open("GET", "${ctx}/member/idcheck?loginId=" + encodeURIComponent(value), true);
	xhr.onload = function () {
		result.textContent = (xhr.responseText === "OK")
			? "사용할 수 있는 아이디입니다."
			: "이미 사용 중인 아이디입니다.";
	};
	xhr.send();
}
</script>
<%@ include file="/common/footer.jsp" %>
