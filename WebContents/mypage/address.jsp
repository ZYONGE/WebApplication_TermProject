<%@ include file="../common/header.jsp" %>
<%@ include file="../common/mynav.jsp" %>
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
					<button type="button" class="btn btn_small" data-popup-open="addressPopup" data-fill-address-popup-title="배송지 수정" data-fill-addr-id="${a.addrId}"
					        data-fill-addr-name="<c:out value='${a.addrName}' />" data-fill-receiver="<c:out value='${a.receiver}' />" data-fill-receiver-phone="<c:out value='${a.receiverPhone}' />"
					        data-fill-zipcode="<c:out value='${a.zipcode}' />" data-fill-addr1="<c:out value='${a.addr1}' />" data-fill-addr2="<c:out value='${a.addr2}' />">수정</button>
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
	<button type="button" class="btn btn_primary" data-popup-open="addressPopup" data-fill-address-popup-title="배송지 추가" data-fill-addr-id="0"
	        data-fill-addr-name="" data-fill-receiver="" data-fill-receiver-phone="" data-fill-zipcode="" data-fill-addr1="" data-fill-addr2="">배송지 추가</button>
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
			<p class="field_help">우편번호는 직접 입력합니다.</p>
		</div>
		<div class="popup_foot">
			<button type="submit" class="btn btn_primary">저장</button>
			<button type="button" class="btn btn_sub popup_close" data-popup-close="addressPopup">닫기</button>
		</div>
	</form>
</dialog>
<%@ include file="../common/footer.jsp" %>
