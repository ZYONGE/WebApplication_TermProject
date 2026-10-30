<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<c:set var="pageCss" value="order" />
<%@ include file="/common/header.jsp" %>

<h2>주문서</h2>

<table class="listtable">
	<thead class="listtable_head">
		<tr><th class="col_name">상품</th><th class="col_price">단가</th>
		    <th class="col_qty">수량</th><th class="col_total">합계</th></tr>
	</thead>
	<tbody>
		<c:forEach var="item" items="${items}">
			<tr>
				<td class="col_name"><c:out value="${item.productName}" /></td>
				<td class="col_price"><fmt:formatNumber value="${item.price}" pattern="#,##0" />원</td>
				<td class="col_qty">${item.quantity}</td>
				<td class="col_total"><fmt:formatNumber value="${item.lineTotal}" pattern="#,##0" />원</td>
			</tr>
		</c:forEach>
	</tbody>
</table>

<form class="order_form" action="${ctx}/order/pay" method="post" data-validate>
	<h3>배송지</h3>

	<%-- 저장된 배송지를 고르면 아래 입력란을 채운다. 기본 배송지는 미리 채워져 있다. --%>
	<c:if test="${not empty addresses}">
		<p>
			<label for="addrPick">저장된 배송지</label>
			<select id="addrPick" onchange="applyAddress(this)">
				<option value="">직접 입력</option>
				<c:forEach var="a" items="${addresses}">
					<option value="${a.addrId}"
					        data-receiver="<c:out value='${a.receiver}' />"
					        data-phone="<c:out value='${a.receiverPhone}' />"
					        data-zipcode="<c:out value='${a.zipcode}' />"
					        data-addr1="<c:out value='${a.addr1}' />"
					        data-addr2="<c:out value='${a.addr2}' />"
					        ${a.isDefault == 'Y' ? 'selected' : ''}>
						<c:out value="${a.addrName}" /> — <c:out value="${a.receiver}" />
						<c:if test="${a.isDefault == 'Y'}"> (기본)</c:if>
					</option>
				</c:forEach>
			</select>
		</p>
	</c:if>

	<table class="formtable">
		<tr>
			<th><label for="receiver">받는 사람 <span class="required">*</span></label></th>
			<td><input type="text" id="receiver" name="receiver" class="field" data-rule="required"
			           value="<c:out value='${empty defaultAddr ? member.name : defaultAddr.receiver}' />"></td>
		</tr>
		<tr>
			<th><label for="receiverPhone">연락처 <span class="required">*</span></label></th>
			<td><input type="text" id="receiverPhone" name="receiverPhone" class="field" data-rule="phone"
			           value="<c:out value='${empty defaultAddr ? member.phone : defaultAddr.receiverPhone}' />"></td>
		</tr>
		<tr>
			<th><label for="zipcode">우편번호 <span class="required">*</span></label></th>
			<td>
				<input type="text" id="zipcode" name="zipcode" class="field" data-rule="required"
				       value="<c:out value='${defaultAddr.zipcode}' />">
				<%-- 외부 주소 API 는 범위 외다 (CLAUDE.md § 5 · ERD § 8 쟁점 5). 직접 입력한다. --%>
				<span class="field_help">우편번호는 직접 입력합니다.</span>
			</td>
		</tr>
		<tr>
			<th><label for="addr1">주소 <span class="required">*</span></label></th>
			<td><input type="text" id="addr1" name="addr1" class="field" data-rule="required"
			           value="<c:out value='${defaultAddr.addr1}' />"></td>
		</tr>
		<tr>
			<th><label for="addr2">상세 주소</label></th>
			<td><input type="text" id="addr2" name="addr2" class="field"
			           value="<c:out value='${defaultAddr.addr2}' />"></td>
		</tr>
		<tr>
			<th><label for="deliveryMemo">요청사항</label></th>
			<td><input type="text" id="deliveryMemo" name="deliveryMemo" class="field"></td>
		</tr>
	</table>

	<h3>결제 수단</h3>
	<%-- 외부 PG 연동은 범위 외다. 선택값만 저장한다 (ERD § 2.10). --%>
	<p class="order_method">
		<label><input type="radio" name="method" value="카드" checked> 카드</label>
		<label><input type="radio" name="method" value="무통장"> 무통장</label>
		<label><input type="radio" name="method" value="간편결제"> 간편결제</label>
	</p>

	<h3>결제 금액</h3>
	<table class="formtable">
		<tr><th>상품 합계</th><td><fmt:formatNumber value="${totalPrice}" pattern="#,##0" />원</td></tr>
		<tr><th>배송비</th><td><fmt:formatNumber value="${deliveryFee}" pattern="#,##0" />원</td></tr>
		<tr><th>최종 결제 금액</th><td><fmt:formatNumber value="${payAmount}" pattern="#,##0" />원</td></tr>
	</table>
	<%--
	  금액을 hidden 으로 보내지 않는다. 서버가 장바구니를 다시 읽어 계산한다
	  (OrderService.pay). 폼 값을 믿으면 가격을 조작할 수 있다.
	--%>

	<div class="btnstyle">
		<button type="submit" class="btn btn_primary"
		        data-confirm-message="결제를 진행하시겠습니까?">결제하기</button>
		<a class="btn btn_sub" href="${ctx}/cart/list">장바구니로</a>
	</div>
</form>

<script>
function applyAddress(select) {
	var opt = select.options[select.selectedIndex];
	if (!opt.value) { return; }
	document.getElementById("receiver").value = opt.getAttribute("data-receiver") || "";
	document.getElementById("receiverPhone").value = opt.getAttribute("data-phone") || "";
	document.getElementById("zipcode").value = opt.getAttribute("data-zipcode") || "";
	document.getElementById("addr1").value = opt.getAttribute("data-addr1") || "";
	document.getElementById("addr2").value = opt.getAttribute("data-addr2") || "";
}
</script>
<%@ include file="/common/footer.jsp" %>
