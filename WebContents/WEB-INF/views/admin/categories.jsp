<%@ include file="../common/header.jsp" %>
<%@ include file="../common/adminnav.jsp" %>
<h2>카테고리 관리</h2>
<div class="admin_filter">
	<span class="field_help">상품은 소분류에만 등록됩니다. 판매 중인 상품이 없는 카테고리는 고객 메뉴에 보이지 않습니다.</span>
	<button type="button" class="btn btn_primary" data-popup-open="categoryPopup" data-fill-category-id="0" data-fill-category-mode="카테고리 등록"
	        data-fill-name="" data-fill-parent-id="0" data-fill-sort-order="0">카테고리 등록</button>
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
				<td class="col_name">
					<c:out value="${empty cat.parentId ? '(대분류)' : cat.parentName}" />
				</td>
				<td class="col_state">${cat.sortOrder}</td>
				<td class="col_action">
					<button type="button" class="btn btn_small" data-popup-open="categoryPopup" data-fill-category-id="${cat.categoryId}"
					        data-fill-category-mode="카테고리 수정 — <c:out value='${cat.name}' />" data-fill-name="<c:out value='${cat.name}' />"
					        data-fill-parent-id="${empty cat.parentId ? 0 : cat.parentId}" data-fill-sort-order="${cat.sortOrder}">수정</button>
					<form action="${ctx}/admin/categories" method="post">
						<input type="hidden" name="mode" value="delete">
						<input type="hidden" name="categoryId" value="${cat.categoryId}">
						<button type="submit" class="btn btn_danger btn_small"
						        data-confirm-message="삭제하시겠습니까?">삭제</button>
					</form>
				</td>
			</tr>
		</c:forEach>
		<c:if test="${empty categories}"><tr><td colspan="5" class="empty">카테고리가 없습니다.</td></tr></c:if>
	</tbody>
</table>
<dialog id="categoryPopup" class="popup">
	<form class="popup_form" action="${ctx}/admin/categories" method="post" data-validate>
		<input type="hidden" name="mode" value="save">
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
<%@ include file="../common/footer.jsp" %>
