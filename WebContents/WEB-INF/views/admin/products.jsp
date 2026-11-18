<%@ include file="../common/header.jsp" %>
<%@ include file="../common/adminnav.jsp" %>
<h2>상품 관리</h2>
<form class="admin_filter" action="${ctx}/admin/products" method="get">
	<select name="categoryId">
		<option value="0">전체 카테고리</option>
		<c:forEach var="cat" items="${categories}">
			<option value="${cat.categoryId}" ${selectedCategoryId == cat.categoryId ? 'selected' : ''}>
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
	<a class="btn btn_primary" href="${ctx}/admin/products?create=1">상품 등록</a>
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
				<td>
					<c:out value="${p.status}" />
					<c:if test="${p.status == '판매중' and p.stock == 0}"><span class="field_help">재고 0 · 고객 화면 숨김</span></c:if>
				</td>
				<td><fmt:formatDate value="${p.createdAt}" pattern="yyyy-MM-dd" /></td>
				<td>
					<a class="btn btn_small" href="${ctx}/admin/products?editId=${p.productId}">수정</a>
					<form action="${ctx}/admin/products" method="post">
						<input type="hidden" name="mode" value="delete">
						<input type="hidden" name="productId" value="${p.productId}">
						<button type="submit" class="btn btn_danger btn_small"
						        data-confirm-message="삭제하시겠습니까? 주문 이력이 있거나 트랙 구성품 · 금지 조합에 걸려 있으면 실패합니다.">삭제</button>
					</form>
				</td>
			</tr>
		</c:forEach>
		<c:if test="${empty pageData.list}"><tr><td colspan="9" class="empty">상품이 없습니다.</td></tr></c:if>
	</tbody>
</table>
<%@ include file="../common/paging.jsp" %>
<dialog id="productPopup" class="popup" ${not empty editProduct or param.create == '1' ? 'data-popup-auto' : ''}>
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
				    	<select id="productType" name="productType"
				    	        data-rule="required" data-message="구분을 먼저 선택하세요.">
				    		<option value="">구분 선택</option>
				    		<option value="SINGLE" ${editProduct.productType == 'SINGLE' ? 'selected' : ''}>단일상품</option>
				    		<option value="TRACK" ${editProduct.productType == 'TRACK' ? 'selected' : ''}>트랙 패키지</option>
				    	</select>
				    	<span class="field_help">단일상품 = 영양소 하나짜리 낱개 영양제, 트랙 패키지 = 여러 낱개를 1포에 묶은 상품</span>
				    </td></tr>
				<tr><th><label for="pCategoryId">카테고리</label></th>
				    <td>
				    	<select id="pCategoryId" name="categoryId"
				    	        data-rule="required" data-message="카테고리를 선택하세요. (구분을 먼저 고르면 고를 수 있습니다)">
				    		<option value="">카테고리 선택</option>
				    		<c:forEach var="root" items="${categoryTree}">
				    			<optgroup label="<c:out value='${root.name}' />" data-type="${rootTypes[root.categoryId]}">
				    				<c:forEach var="sub" items="${root.children}">
				    					<option value="${sub.categoryId}"
				    					        ${editProduct.categoryId == sub.categoryId ? 'selected' : ''}><c:out value="${sub.name}" /></option>
				    				</c:forEach>
				    			</optgroup>
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
				<tr id="trackItemsRow">
				    <th>트랙 구성</th>
				    <td>
				    	<ul class="admin_check">
				    		<c:forEach var="sg" items="${singles}">
				    			<li>
				    				<label>
				    					<input type="checkbox" name="itemId" value="${sg.productId}" ${trackItemIds.contains(sg.productId) ? 'checked' : ''}>
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
<%@ include file="../common/footer.jsp" %>
