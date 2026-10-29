<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
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

<h2>배송지 관리</h2>

<table class="listtable">
	<thead class="listtable_head">
		<tr><th class="col_name">별칭</th><th class="col_name">받는 사람</th>
		    <th class="col_addr">주소</th><th class="col_state">기본</th><th class="col_action"></th></tr>
	</thead>
	<tbody>
		<c:forEach var="a" items="${addresses}">
			<tr class="${a.isDefault == 'Y' ? 'on' : ''}">
				<td class="col_name"><c:out value="${a.addrName}" /></td>
				<td class="col_name"><c:out value="${a.receiver}" /> (<c:out value="${a.receiverPhone}" />)</td>
				<td class="col_addr">
					[<c:out value="${a.zipcode}" />] <c:out value="${a.addr1}" /> <c:out value="${a.addr2}" />
				</td>
				<td class="col_state">
					<c:choose>
						<c:when test="${a.isDefault == 'Y'}">기본</c:when>
						<c:otherwise>
							<form action="${ctx}/mypage/address" method="post">
								<input type="hidden" name="mode" value="default">
								<input type="hidden" name="addrId" value="${a.addrId}">
								<button type="submit" class="btn btn_small">기본으로</button>
							</form>
						</c:otherwise>
					</c:choose>
				</td>
				<td class="col_action">
					<button type="button" class="btn btn_small"
					        onclick="editAddress(${a.addrId}, '<c:out value="${a.addrName}" />', '<c:out value="${a.receiver}" />', '<c:out value="${a.receiverPhone}" />', '<c:out value="${a.zipcode}" />', '<c:out value="${a.addr1}" />', '<c:out value="${a.addr2}" />')">수정</button>
					<form action="${ctx}/mypage/address" method="post">
						<input type="hidden" name="mode" value="delete">
						<input type="hidden" name="addrId" value="${a.addrId}">
						<button type="submit" class="btn btn_danger btn_small"
						        data-confirm-message="이 배송지를 삭제하시겠습니까?">삭제</button>
					</form>
				</td>
			</tr>
		</c:forEach>
		<c:if test="${empty addresses}">
			<tr><td colspan="5" class="empty">등록된 배송지가 없습니다.</td></tr>
		</c:if>
	</tbody>
</table>

<div class="btnstyle">
	<button type="button" class="btn btn_primary" onclick="newAddress()">배송지 추가</button>
</div>

<dialog id="addressPopup" class="popup">
	<form class="popup_form" action="${ctx}/mypage/address" method="post" data-validate>
		<input type="hidden" name="mode" value="save">
		<input type="hidden" name="addrId" id="addrId" value="0">

		<h3 class="popup_head" id="addressPopupTitle">배송지 추가</h3>
		<div class="popup_body">
			<table class="formtable">
				<tr><th><label for="addrName">별칭</label></th>
				    <td><input type="text" id="addrName" name="addrName" class="field"></td></tr>
				<tr><th><label for="receiver">받는 사람</label></th>
				    <td><input type="text" id="receiver" name="receiver" class="field" data-rule="required"></td></tr>
				<tr><th><label for="receiverPhone">연락처</label></th>
				    <td><input type="text" id="receiverPhone" name="receiverPhone" class="field" data-rule="phone"></td></tr>
				<tr><th><label for="zipcode">우편번호</label></th>
				    <td><input type="text" id="zipcode" name="zipcode" class="field" data-rule="required"></td></tr>
				<tr><th><label for="addr1">주소</label></th>
				    <td><input type="text" id="addr1" name="addr1" class="field" data-rule="required"></td></tr>
				<tr><th><label for="addr2">상세 주소</label></th>
				    <td><input type="text" id="addr2" name="addr2" class="field"></td></tr>
				<tr><th>기본 배송지</th>
				    <td><label><input type="checkbox" name="isDefault" value="Y"> 기본 배송지로 지정</label></td></tr>
			</table>
			<p class="field_help">우편번호는 직접 입력합니다. (외부 주소 API 는 범위 외)</p>
		</div>
		<div class="popup_foot">
			<button type="submit" class="btn btn_primary">저장</button>
			<button type="button" class="btn btn_sub popup_close" data-popup-close="addressPopup">닫기</button>
		</div>
	</form>
</dialog>

<script>
function newAddress() {
	document.getElementById("addressPopupTitle").textContent = "배송지 추가";
	document.getElementById("addrId").value = "0";
	["addrName", "receiver", "receiverPhone", "zipcode", "addr1", "addr2"].forEach(function (id) {
		document.getElementById(id).value = "";
	});
	window.openPopup("addressPopup");
}

function editAddress(id, addrName, receiver, phone, zipcode, addr1, addr2) {
	document.getElementById("addressPopupTitle").textContent = "배송지 수정";
	document.getElementById("addrId").value = id;
	document.getElementById("addrName").value = addrName;
	document.getElementById("receiver").value = receiver;
	document.getElementById("receiverPhone").value = phone;
	document.getElementById("zipcode").value = zipcode;
	document.getElementById("addr1").value = addr1;
	document.getElementById("addr2").value = addr2;
	window.openPopup("addressPopup");
}
</script>
<%@ include file="/common/footer.jsp" %>
