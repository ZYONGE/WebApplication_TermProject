<%@ include file="../common/header.jsp" %>
<h2><c:out value="${heading}" /></h2>
<p class="field_help">가격은 1포(하루 섭취분) 기준입니다. 며칠치를 살지는 상품 화면에서 정합니다.
  필요한 영양소만 고르려면 <a href="${ctx}/custom">조합 맞추기</a>를 이용하세요.</p>
<form class="prod_filter" action="${ctx}/product/list" method="get">
  <label for="categoryId">카테고리</label>
  <select id="categoryId" name="categoryId">
    <c:if test="${not tabMode}"><option value="0">전체</option></c:if>
    <c:forEach var="root" items="${filterRoots}">
      <option value="${root.categoryId}" ${selectedCategoryId == root.categoryId or (tabMode and selectedCategoryId == 0) ? 'selected' : ''}>
        <c:out value="${root.name}" /><c:if test="${tabMode}"> 전체</c:if>
      </option>
      <c:forEach var="sub" items="${root.children}">
        <option value="${sub.categoryId}" ${selectedCategoryId == sub.categoryId ? 'selected' : ''}>— <c:out value="${sub.name}" /></option>
      </c:forEach>
    </c:forEach>
  </select>
  <label for="sort">정렬</label>
  <select id="sort" name="sort">
    <option value="latest" ${empty param.sort or param.sort == 'latest' ? 'selected' : ''}>최신순</option>
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
  <c:set var="cardType" value="list" />
  <c:forEach var="p" items="${pageData.list}"><%@ include file="../common/prodcard.jsp" %></c:forEach>
  <c:if test="${empty pageData.list}"><li class="empty">조건에 맞는 상품이 없습니다.</li></c:if>
</ul>
<%@ include file="../common/paging.jsp" %>
<%@ include file="../common/footer.jsp" %>
