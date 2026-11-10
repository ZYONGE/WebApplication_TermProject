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

<h2>상품 관리</h2>

<form class="admin_filter" action="${ctx}/admin/products" method="get">
	<select name="categoryId">
		<option value="0">전체 카테고리</option>
		<c:forEach var="cat" items="${categories}">
			<option value="${cat.categoryId}" ${param.categoryId == cat.categoryId ? 'selected' : ''}>
				<c:if test="${not empty cat.parentId}">— </c:if><c:out value="${cat.name}" />
			</option>
		</c:forEach>
	</select>
	<select name="type">
		<option value="">전체 구분</option>
		<option value="SINGLE" ${param.type == 'SINGLE' ? 'selected' : ''}>낱개</option>
		<option value="TRACK" ${param.type == 'TRACK' ? 'selected' : ''}>트랙</option>
	</select>
	<select name="status">
		<option value="">전체 상태</option>
		<option value="판매중" ${param.status == '판매중' ? 'selected' : ''}>판매중</option>
		<option value="숨김" ${param.status == '숨김' ? 'selected' : ''}>숨김</option>
	</select>
	<input type="text" name="keyword" value="<c:out value='${param.keyword}' />" placeholder="상품명">
	<button type="submit" class="btn">검색</button>
	<button type="button" class="btn btn_primary" onclick="newProduct()">상품 등록</button>
</form>

<table class="listtable">
	<thead class="listtable_head">
		<tr><th>번호</th><th>구분</th><th>상품명</th><th>카테고리</th><th>1포 가격</th><th>재고(포)</th>
		    <th>상태</th><th>등록일</th><th></th></tr>
	</thead>
	<tbody>
		<c:forEach var="p" items="${pageData.list}">
			<tr class="${p.status == '숨김' ? 'disabled' : ''}">
				<td>${p.productId}</td>
				<td>${p.typeLabel}</td>
				<td><c:out value="${p.name}" /></td>
				<td><c:out value="${p.categoryName}" /></td>
				<td><fmt:formatNumber value="${p.price}" pattern="#,##0" />원</td>
				<td>${p.stock}</td>
				<td><c:out value="${p.status}" /></td>
				<td><fmt:formatDate value="${p.createdAt}" pattern="yyyy-MM-dd" /></td>
				<td>
					<a class="btn btn_small" href="${ctx}/admin/products?editId=${p.productId}">수정</a>
					<form action="${ctx}/admin/products" method="post">
						<input type="hidden" name="mode" value="delete">
						<input type="hidden" name="productId" value="${p.productId}">
						<%-- order_item · track_item(구성품) · supplement_conflict FK 가 RESTRICT 라 걸려 있으면 삭제가 거부된다 --%>
						<button type="submit" class="btn btn_danger btn_small"
						        data-confirm-message="삭제하시겠습니까? 주문 이력이 있거나 트랙 구성품 · 금지 조합에 걸려 있으면 실패합니다.">삭제</button>
					</form>
				</td>
			</tr>
		</c:forEach>
		<c:if test="${empty pageData.list}"><tr><td colspan="9" class="empty">상품이 없습니다.</td></tr></c:if>
	</tbody>
</table>

<%@ include file="/common/paging.jsp" %>

<dialog id="productPopup" class="popup">
	<%-- 업로드가 있으므로 multipart. 서버는 cos.jar 로 받는다. --%>
	<form class="popup_form" action="${ctx}/admin/products" method="post"
	      enctype="multipart/form-data" data-validate>
		<input type="hidden" name="productId" id="productId" value="${editProduct.productId}">
		<input type="hidden" name="currentThumbnail" id="currentThumbnail"
		       value="<c:out value='${editProduct.thumbnail}' />">

		<h3 class="popup_head" id="productPopupTitle">
			${empty editProduct ? '상품 등록' : '상품 수정'}
		</h3>
		<div class="popup_body">
			<table class="formtable">
				<tr><th><label for="name">상품명</label></th>
				    <td><input type="text" id="name" name="name" class="field" data-rule="required"
				               value="<c:out value='${editProduct.name}' />"></td></tr>
				<tr><th><label for="productType">구분</label></th>
				    <td>
				    	<%-- 판매 이력이 생긴 뒤에는 구분을 바꿀 수 없다 (ERD § 8-15) — 서버가 거부한다 --%>
				    	<select id="productType" name="productType" onchange="toggleTrackItems()">
				    		<option value="SINGLE" ${editProduct.productType != 'TRACK' ? 'selected' : ''}>낱개 영양제</option>
				    		<option value="TRACK" ${editProduct.productType == 'TRACK' ? 'selected' : ''}>트랙 패키지</option>
				    	</select>
				    	<span class="field_help">낱개는 '낱개 영양제', 트랙은 '트랙 패키지' 밑 소분류에 등록합니다.</span>
				    </td></tr>
				<tr><th><label for="pCategoryId">카테고리</label></th>
				    <td>
				    	<select id="pCategoryId" name="categoryId">
				    		<c:forEach var="cat" items="${categories}">
				    			<option value="${cat.categoryId}"
				    			        ${editProduct.categoryId == cat.categoryId ? 'selected' : ''}>
				    				<c:if test="${not empty cat.parentId}">— </c:if><c:out value="${cat.name}" />
				    			</option>
				    		</c:forEach>
				    	</select>
				    </td></tr>
				<tr><th><label for="price">1포 가격</label></th>
				    <td><input type="number" id="price" name="price" class="field" min="0"
				               value="${empty editProduct ? 0 : editProduct.price}"></td></tr>
				<tr><th><label for="stock">재고 (포)</label></th>
				    <td><input type="number" id="stock" name="stock" class="field" min="0"
				               value="${empty editProduct ? 0 : editProduct.stock}"></td></tr>
				<tr><th><label for="status">상태</label></th>
				    <td>
				    	<select id="status" name="status">
				    		<option value="판매중" ${editProduct.status == '판매중' ? 'selected' : ''}>판매중</option>
				    		<option value="숨김" ${editProduct.status == '숨김' ? 'selected' : ''}>숨김</option>
				    	</select>
				    	<span class="field_help">'품절' 상태는 없습니다. 재고 0 이면 자동으로 숨겨집니다.</span>
				    </td></tr>
				<%-- 트랙 구성 — 트랙일 때만 보인다. 구성품끼리 금지 조합이 있으면 저장이 거부된다 (ERD § 8-14) --%>
				<tr id="trackItemsRow">
				    <th>트랙 구성</th>
				    <td>
				    	<ul class="admin_check">
				    		<c:forEach var="sg" items="${singles}">
				    			<c:set var="inTrack" value="false" />
				    			<c:forEach var="ti" items="${editProduct.trackItems}">
				    				<c:if test="${ti.productId == sg.productId}"><c:set var="inTrack" value="true" /></c:if>
				    			</c:forEach>
				    			<li>
				    				<label>
				    					<input type="checkbox" name="itemId" value="${sg.productId}" ${inTrack ? 'checked' : ''}>
				    					<c:out value="${sg.name}" />
				    					(1포 <fmt:formatNumber value="${sg.price}" pattern="#,##0" />원<c:if test="${sg.status == '숨김'}"> · 숨김</c:if>)
				    				</label>
				    			</li>
				    		</c:forEach>
				    	</ul>
				    	<span class="field_help">1포에 고른 낱개가 1회분씩 들어갑니다. 트랙 재고는 구성 낱개와 별개로 이 상품의 재고만 씁니다.</span>
				    </td></tr>
				<tr><th><label for="description">설명</label></th>
				    <td><textarea id="description" name="description" rows="5" class="field"><c:out value="${editProduct.description}" /></textarea></td></tr>
				<tr><th><label for="thumbnail">대표 이미지</label></th>
				    <td>
				    	<input type="file" id="thumbnail" name="thumbnail" accept="image/*">
				    	<c:if test="${not empty editProduct.thumbnail}">
				    		<span class="field_help">현재: <c:out value="${editProduct.thumbnail}" /></span>
				    	</c:if>
				    </td></tr>
				<tr><th><label for="image1">추가 이미지</label></th>
				    <td>
				    	<input type="file" id="image1" name="image1" accept="image/*">
				    	<input type="file" id="image2" name="image2" accept="image/*">
				    </td></tr>
			</table>

			<c:if test="${not empty editProduct.images}">
				<h4>등록된 이미지</h4>
				<ul>
					<c:forEach var="img" items="${editProduct.images}">
						<li><c:out value="${img.imgPath}" /> (순서 ${img.sortOrder})</li>
					</c:forEach>
				</ul>
			</c:if>
		</div>
		<div class="popup_foot">
			<button type="submit" class="btn btn_primary">저장</button>
			<button type="button" class="btn btn_sub popup_close" data-popup-close="productPopup">닫기</button>
		</div>
	</form>
</dialog>

<script>
/*
  등록은 editId 가 붙은 상태를 지워야 하므로 한 번 이동한 뒤 연다.
  수정은 서버가 editProduct 를 채워 보내므로 로드 직후 바로 연다.
*/
function newProduct() {
	location.href = "${ctx}/admin/products?create=1";
}

/* 트랙 구성 칸은 트랙일 때만 보인다 */
function toggleTrackItems() {
	var type = document.getElementById("productType").value;
	document.getElementById("trackItemsRow").hidden = (type !== "TRACK");
}
document.addEventListener("DOMContentLoaded", toggleTrackItems);

<c:if test="${not empty editProduct or param.create == '1'}">
document.addEventListener("DOMContentLoaded", function () {
	window.openPopup("productPopup");
});
</c:if>
</script>
<%@ include file="/common/footer.jsp" %>
