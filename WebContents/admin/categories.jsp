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

<h2>카테고리 관리</h2>

<div class="admin_filter">
	<span class="field_help">상품은 소분류에만 등록됩니다. 판매 중인 상품이 없는 카테고리는 고객 메뉴에 보이지 않습니다.</span>
	<button type="button" class="btn btn_primary" onclick="newCategory()">카테고리 등록</button>
</div>

<table class="listtable">
	<thead class="listtable_head">
		<tr><th class="col_no">번호</th><th class="col_name">이름</th><th class="col_name">상위</th>
		    <th class="col_state">순서</th><th class="col_action"></th></tr>
	</thead>
	<tbody>
		<c:forEach var="cat" items="${categories}">
			<tr>
				<td class="col_no">${cat.categoryId}</td>
				<td class="col_name"><c:if test="${not empty cat.parentId}">— </c:if><c:out value="${cat.name}" /></td>
				<%-- 상위는 번호가 아니라 이름으로 --%>
				<td class="col_name">
					<c:choose>
						<c:when test="${empty cat.parentId}">(대분류)</c:when>
						<c:otherwise>
							<c:forEach var="r" items="${tree}"><c:if test="${r.categoryId == cat.parentId}"><c:out value="${r.name}" /></c:if></c:forEach>
						</c:otherwise>
					</c:choose>
				</td>
				<td class="col_state">${cat.sortOrder}</td>
				<td class="col_action">
					<button type="button" class="btn btn_small"
					        onclick="editCategory(${cat.categoryId}, '<c:out value="${cat.name}" />', ${empty cat.parentId ? 0 : cat.parentId}, ${cat.sortOrder})">수정</button>
					<form action="${ctx}/admin/categories" method="post">
						<input type="hidden" name="mode" value="delete">
						<input type="hidden" name="categoryId" value="${cat.categoryId}">
						<%-- 하위 카테고리나 상품이 남아 있으면 서비스가 먼저 거절한다 --%>
						<button type="submit" class="btn btn_danger btn_small"
						        data-confirm-message="삭제하시겠습니까?">삭제</button>
					</form>
				</td>
			</tr>
		</c:forEach>
		<c:if test="${empty categories}"><tr><td colspan="5" class="empty">카테고리가 없습니다.</td></tr></c:if>
	</tbody>
</table>

<%-- 등록 · 수정 — 다른 관리 화면처럼 팝업 (DECISIONS § 4) --%>
<dialog id="categoryPopup" class="popup">
	<form class="popup_form" action="${ctx}/admin/categories" method="post" data-validate>
		<input type="hidden" name="mode" value="save">
		<%-- 번호는 내부 값이라 입력란으로 노출하지 않는다. 목록의 '수정' 이 채운다. --%>
		<input type="hidden" id="categoryId" name="categoryId" value="0">
		<h3 class="popup_head" id="categoryMode">카테고리 등록</h3>
		<div class="popup_body">
			<table class="formtable">
				<tr><th><label for="name">이름</label></th>
				    <td><input type="text" id="name" name="name" class="field" data-rule="required"></td></tr>
				<tr><th><label for="parentId">상위 카테고리</label></th>
				    <td>
				    	<select id="parentId" name="parentId">
				    		<option value="0">(대분류로 등록)</option>
				    		<c:forEach var="cat" items="${tree}">
				    			<option value="${cat.categoryId}"><c:out value="${cat.name}" /></option>
				    		</c:forEach>
				    	</select>
				    	<%-- 대분류 이름 중복은 서비스가 막는다. UNIQUE 가 parent_id IS NULL 을 보호하지 못하기 때문이다. --%>
				    </td></tr>
				<tr><th><label for="sortOrder">정렬 순서</label></th>
				    <td><input type="number" id="sortOrder" name="sortOrder" value="0"></td></tr>
			</table>
		</div>
		<div class="popup_foot">
			<button type="submit" class="btn btn_primary">저장</button>
			<button type="button" class="btn btn_sub popup_close" data-popup-close="categoryPopup">닫기</button>
		</div>
	</form>
</dialog>

<script>
function fillCategory(id, name, parentId, sortOrder, title) {
	document.getElementById("categoryId").value = id;
	document.getElementById("categoryMode").textContent = title;
	document.getElementById("name").value = name;
	document.getElementById("parentId").value = parentId;
	document.getElementById("sortOrder").value = sortOrder;
	window.openPopup("categoryPopup");
}

function editCategory(id, name, parentId, sortOrder) {
	fillCategory(id, name, parentId, sortOrder, "카테고리 수정 — " + name);
}

function newCategory() {
	fillCategory(0, "", 0, 0, "카테고리 등록");
}
</script>
<%@ include file="/common/footer.jsp" %>
