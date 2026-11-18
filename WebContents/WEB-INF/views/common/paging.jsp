<%-- 페이지 번호 — 서블릿이 pageData(Page) 와 pageUrl("/목록?조건&") 을 넣어 준다 --%>
<c:if test="${pageData.totalPages > 1}">
<div class="paging">
  <c:if test="${pageData.hasPrev}"><a class="paging_prev" href="${ctx}${pageUrl}page=${pageData.page - 1}">이전</a></c:if>
  <c:forEach var="i" begin="${pageData.blockStart}" end="${pageData.blockEnd}">
    <c:if test="${i == pageData.page}"><strong class="paging_num on">${i}</strong></c:if>
    <c:if test="${i != pageData.page}"><a class="paging_num" href="${ctx}${pageUrl}page=${i}">${i}</a></c:if>
  </c:forEach>
  <c:if test="${pageData.hasNext}"><a class="paging_next" href="${ctx}${pageUrl}page=${pageData.page + 1}">다음</a></c:if>
</div>
<p class="paging_info">전체 ${pageData.total}건 · ${pageData.page} / ${pageData.totalPages} 페이지</p>
</c:if>
