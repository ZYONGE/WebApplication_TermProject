<%@ include file="../common/header.jsp" %>
<h2 class="member_title">회원가입</h2>
<form class="formtable_form member_box" action="${ctx}/member/join" method="post" data-validate>
	<table class="formtable">
		<tr>
			<th><label for="loginId">아이디 <span class="required">*</span></label></th>
			<td>
				<div class="member_idrow">
					<input type="text" id="loginId" name="loginId" class="field"
					       value="<c:out value='${param.loginId}' />" data-rule="loginId"
					       data-check-required="아이디 중복확인을 해 주세요." data-checked-value="">
					<button type="button" class="btn" data-idcheck="${ctx}/member/idcheck">중복확인</button>
				</div>
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
				       value="<c:out value='${param.name}' />" data-rule="name">
			</td>
		</tr>
		<tr>
			<th><label for="email">이메일 <span class="required">*</span></label></th>
			<td>
				<input type="text" id="email" name="email" class="field"
				       value="<c:out value='${param.email}' />" data-rule="email">
			</td>
		</tr>
		<tr>
			<th><label for="phone">연락처 <span class="required">*</span></label></th>
			<td>
				<input type="text" id="phone" name="phone" class="field"
				       value="<c:out value='${param.phone}' />" data-rule="phone">
				<span class="field_help">예: 010-1234-5678</span>
			</td>
		</tr>
	</table>
	<div class="btnstyle">
		<button type="submit" class="btn btn_primary">가입하기</button>
		<a class="btn btn_sub" href="${ctx}/">취소</a>
	</div>
</form>
<%@ include file="../common/footer.jsp" %>
