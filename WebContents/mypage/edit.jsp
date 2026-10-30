<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<c:set var="pageCss" value="mypage" />
<%@ include file="/common/header.jsp" %>

<ul class="my_nav">
	<li><a href="${ctx}/mypage/">마이페이지</a></li>
	<li><a href="${ctx}/mypage/edit">정보수정</a></li>
	<li><a href="${ctx}/mypage/address">배송지</a></li>
	<li><a href="${ctx}/mypage/wishlist">찜 목록</a></li>
	<li><a href="${ctx}/mypage/review">내 리뷰</a></li>
	<li><a href="${ctx}/mypage/qna">내 문의</a></li>
	<li><a href="${ctx}/order/list">주문 내역</a></li>
</ul>

<h2>정보 수정</h2>

<form class="formtable_form" action="${ctx}/mypage/edit" method="post" data-validate>
	<table class="formtable">
		<tr><th>아이디</th><td><c:out value="${member.loginId}" /> <span class="field_help">아이디는 변경할 수 없습니다.</span></td></tr>
		<tr>
			<th><label for="name">이름</label></th>
			<td><input type="text" id="name" name="name" class="field" data-rule="name"
			           value="<c:out value='${member.name}' />"></td>
		</tr>
		<tr>
			<th><label for="email">이메일</label></th>
			<td><input type="text" id="email" name="email" class="field" data-rule="email"
			           value="<c:out value='${member.email}' />"></td>
		</tr>
		<tr>
			<th><label for="phone">연락처</label></th>
			<td><input type="text" id="phone" name="phone" class="field" data-rule="phone"
			           value="<c:out value='${member.phone}' />"></td>
		</tr>
	</table>
	<div class="btnstyle">
		<button type="submit" class="btn btn_primary">저장</button>
	</div>
</form>

<h3>비밀번호 변경</h3>
<form class="formtable_form" action="${ctx}/mypage/password" method="post" data-validate>
	<table class="formtable">
		<tr>
			<th><label for="currentPassword">현재 비밀번호</label></th>
			<td><input type="password" id="currentPassword" name="currentPassword" class="field" data-rule="required"></td>
		</tr>
		<tr>
			<th><label for="newPassword">새 비밀번호</label></th>
			<td><input type="password" id="newPassword" name="newPassword" class="field" data-rule="password"></td>
		</tr>
		<tr>
			<th><label for="newPasswordConfirm">새 비밀번호 확인</label></th>
			<td><input type="password" id="newPasswordConfirm" name="newPasswordConfirm" class="field"
			           data-confirm="newPassword"></td>
		</tr>
	</table>
	<div class="btnstyle">
		<button type="submit" class="btn btn_primary">비밀번호 변경</button>
	</div>
</form>

<h3>회원 탈퇴</h3>
<%--
  탈퇴는 소프트 삭제다 (status='LEFT'). 주문 · 리뷰가 FK 로 참조하므로
  물리 삭제가 불가능하다 (CRUD.md § 4 member 행).
--%>
<form class="formtable_form" action="${ctx}/mypage/leave" method="post">
	<table class="formtable">
		<tr>
			<th><label for="leavePassword">비밀번호 확인</label></th>
			<td><input type="password" id="leavePassword" name="password" class="field"></td>
		</tr>
	</table>
	<p class="field_help">탈퇴해도 주문 내역은 보존됩니다. 같은 아이디로 다시 로그인할 수 없습니다.</p>
	<div class="btnstyle">
		<button type="submit" class="btn btn_danger"
		        data-confirm-message="정말 탈퇴하시겠습니까?">탈퇴하기</button>
	</div>
</form>
<%@ include file="/common/footer.jsp" %>
