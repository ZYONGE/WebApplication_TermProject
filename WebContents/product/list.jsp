<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<c:set var="pageCss" value="product" />
<%@ include file="/common/header.jsp" %>

<h2>
	<c:choose>
		<c:when test="${not empty currentCategory}"><c:out value="${currentCategory.name}" /></c:when>
		<c:when test="${not empty tabTitle}"><c:out value="${tabTitle}" /></c:when>
		<c:when test="${not empty param.keyword}">'<c:out value="${param.keyword}" />' 검색 결과</c:when>
		<c:otherwise>전체 영양제</c:otherwise>
	</c:choose>
</h2>

<p class="field_help">가격은 1포(하루 섭취분) 기준입니다. 며칠치를 살지는 상품 화면에서 정합니다.
	필요한 영양소만 고르려면 <a href="${ctx}/custom">조합 맞추기</a>를 이용하세요.</p>

<form class="prod_filter" action="${ctx}/product/list" method="get">
	<%--
	  필터는 카테고리 · 정렬 · 검색어 세 가지 (구분 선택은 두지 않는다).
	  카테고리 선택지는 지금 탭의 대분류 묶음뿐이다 — 낱개 영양제 탭이면 낱개 영양제 쪽만,
	  트랙 패키지 탭이면 트랙 패키지 쪽만 (ProductServlet 이 filterRoots 로 내려준다).
	  탭 안의 '전체' 는 그 대분류 자체를 골라 탭을 벗어나지 않는다.
	--%>
	<label for="categoryId">카테고리</label>
	<select id="categoryId" name="categoryId">
		<c:if test="${not tabMode}"><option value="0">전체</option></c:if>
		<c:forEach var="root" items="${filterRoots}">
			<option value="${root.categoryId}"
			        ${selectedCategoryId == root.categoryId or (tabMode and selectedCategoryId == 0) ? 'selected' : ''}>
				<c:out value="${root.name}" /><c:if test="${tabMode}"> 전체</c:if>
			</option>
			<c:forEach var="sub" items="${root.children}">
				<option value="${sub.categoryId}" ${selectedCategoryId == sub.categoryId ? 'selected' : ''}>— <c:out value="${sub.name}" /></option>
			</c:forEach>
		</c:forEach>
	</select>

	<label for="sort">정렬</label>
	<%-- 정렬 키는 서버 화이트리스트를 통과한 것만 쓰인다 (ERD § 6.5) --%>
	<select id="sort" name="sort">
		<option value="latest" ${param.sort == 'latest' or empty param.sort ? 'selected' : ''}>최신순</option>
		<option value="priceAsc" ${param.sort == 'priceAsc' ? 'selected' : ''}>낮은 가격순</option>
		<option value="priceDesc" ${param.sort == 'priceDesc' ? 'selected' : ''}>높은 가격순</option>
		<option value="rating" ${param.sort == 'rating' ? 'selected' : ''}>별점순</option>
		<option value="name" ${param.sort == 'name' ? 'selected' : ''}>이름순</option>
	</select>

	<label for="keyword">검색어</label>
	<input type="text" id="keyword" name="keyword" value="<c:out value='${param.keyword}' />">

	<button type="submit" class="btn">적용</button>
</form>

<ul class="prodlist">
	<c:forEach var="p" items="${pageData.list}">
		<li class="prodcard">
			<a href="${ctx}/product/view?productId=${p.productId}">
				<span class="prodcard_thumb">
					<c:choose>
						<c:when test="${not empty p.thumbnail}">
							<img src="${ctx}${p.thumbnail}" alt="">
						</c:when>
						<c:otherwise>[이미지 없음]</c:otherwise>
					</c:choose>
				</span>
				<span class="prodcard_name"><c:out value="${p.name}" /></span>
				<span class="prodcard_price">1포 <fmt:formatNumber value="${p.price}" pattern="#,##0" />원</span>
			</a>
			<c:if test="${p.track}">
				<span class="prodcard_badge">
					구성 <c:forEach var="item" items="${p.trackItems}" varStatus="st"><c:if test="${not st.first}"> + </c:if><c:out value="${item.name}" /></c:forEach>
				</span>
			</c:if>
			<span class="prodcard_badge">
				${p.typeLabel} · <c:out value="${p.categoryName}" /> ·
				별점 <fmt:formatNumber value="${p.avgRating}" pattern="0.0" /> (${p.reviewCount})
			</span>
		</li>
	</c:forEach>
	<c:if test="${empty pageData.list}">
		<li class="empty">조건에 맞는 상품이 없습니다.</li>
	</c:if>
</ul>

<%@ include file="/common/paging.jsp" %>
<%@ include file="/common/footer.jsp" %>
