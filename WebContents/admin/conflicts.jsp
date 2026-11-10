<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<c:set var="pageCss" value="admin" />
<%@ include file="/common/header.jsp" %>

<ul class="admin_nav">
	<li><a href="${ctx}/admin/dashboard">대시보드</a></li>
	<li><a href="${ctx}/admin/products">상품</a></li>
	<li><a href="${ctx}/admin/conflicts">금지 조합</a></li>
	<li><a href="${ctx}/admin/categories">카테고리</a></li>
	<li><a href="${ctx}/admin/orders">주문·배송</a></li>
	<li><a href="${ctx}/admin/members">회원</a></li>
	<li><a href="${ctx}/admin/qna">문의답변</a></li>
	<li><a href="${ctx}/admin/reviews">리뷰</a></li>
	<li><a href="${ctx}/admin/notices">공지</a></li>
</ul>

<h2>금지 조합 관리</h2>

<%--
  39번 관리자 조합 관리 (CRUD.md · ERD § 7.1 · IMPACT K7)
  ★ 낱개끼리만 등록한다. 저장할 때 작은 상품 번호가 product_a 가 된다 (CHECK product_a < product_b).
  ★ 두 낱개가 이미 한 트랙 안에 같이 들어 있으면 등록이 거부된다 (ERD § 8-14).
  ★ 경고는 안내만 한다 — 고객의 구매를 막지 않는다.
--%>
<p class="field_help">여기 등록한 조합을 고객이 함께 고르면 조합 맞추기 · 상품상세 · 장바구니 · 주문서에 경고 문구가 보입니다. 경고는 구매를 막지 않습니다.</p>

<form class="admin_filter" action="${ctx}/admin/conflicts" method="post" data-validate>
	<input type="hidden" name="mode" value="create">
	<label for="productA">영양제 A</label>
	<select id="productA" name="productA">
		<c:forEach var="sg" items="${singles}">
			<option value="${sg.productId}">[${sg.productId}] <c:out value="${sg.name}" /></option>
		</c:forEach>
	</select>
	<label for="productB">영양제 B</label>
	<select id="productB" name="productB">
		<c:forEach var="sg" items="${singles}" varStatus="st">
			<option value="${sg.productId}" ${st.index == 1 ? 'selected' : ''}>[${sg.productId}] <c:out value="${sg.name}" /></option>
		</c:forEach>
	</select>
	<label for="message">경고 문구</label>
	<input type="text" id="message" name="message" class="field" maxlength="200" data-rule="required"
	       placeholder="함께 드시면 안 되는 이유와 권장 섭취 간격">
	<button type="submit" class="btn btn_primary">등록</button>
</form>

<table class="listtable">
	<thead class="listtable_head">
		<tr><th>번호</th><th>영양제 A</th><th>영양제 B</th><th>경고 문구</th><th>등록일</th><th></th></tr>
	</thead>
	<tbody>
		<c:forEach var="cf" items="${conflicts}">
			<tr>
				<td>${cf.conflictId}</td>
				<td>[${cf.productA}] <c:out value="${cf.productAName}" /></td>
				<td>[${cf.productB}] <c:out value="${cf.productBName}" /></td>
				<td><c:out value="${cf.message}" /></td>
				<td><fmt:formatDate value="${cf.createdAt}" pattern="yyyy-MM-dd" /></td>
				<td>
					<a class="btn btn_small" href="${ctx}/admin/conflicts?editId=${cf.conflictId}">문구 수정</a>
					<form action="${ctx}/admin/conflicts" method="post">
						<input type="hidden" name="mode" value="delete">
						<input type="hidden" name="conflictId" value="${cf.conflictId}">
						<button type="submit" class="btn btn_danger btn_small"
						        data-confirm-message="이 금지 조합을 삭제하시겠습니까?">삭제</button>
					</form>
				</td>
			</tr>
		</c:forEach>
		<c:if test="${empty conflicts}"><tr><td colspan="6" class="empty">등록된 금지 조합이 없습니다.</td></tr></c:if>
	</tbody>
</table>

<dialog id="conflictPopup" class="popup">
	<form class="popup_form" action="${ctx}/admin/conflicts" method="post" data-validate>
		<input type="hidden" name="mode" value="update">
		<input type="hidden" name="conflictId" value="${editConflict.conflictId}">
		<h3 class="popup_head">경고 문구 수정</h3>
		<div class="popup_body">
			<p><c:out value="${editConflict.productAName}" /> × <c:out value="${editConflict.productBName}" /></p>
			<label for="editMessage">경고 문구</label>
			<textarea id="editMessage" name="message" rows="3" class="field" maxlength="200"
			          data-rule="required"><c:out value="${editConflict.message}" /></textarea>
			<p class="field_help">쌍을 바꾸려면 삭제하고 새로 등록하세요.</p>
		</div>
		<div class="popup_foot">
			<button type="submit" class="btn btn_primary">저장</button>
			<button type="button" class="btn btn_sub popup_close" data-popup-close="conflictPopup">닫기</button>
		</div>
	</form>
</dialog>

<c:if test="${not empty editConflict}">
<script>
document.addEventListener("DOMContentLoaded", function () {
	window.openPopup("conflictPopup");
});
</script>
</c:if>
<%@ include file="/common/footer.jsp" %>
