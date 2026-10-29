<%--
  페이지 번호 — 목록 화면이 공통으로 include 한다.

  쓰는 쪽에서 두 값을 미리 넣어 둔다.
      pageData : PageDTO
      pageUrl  : 페이지 번호만 빼고 완성된 링크 (예: "/product/list?categoryId=1&")
  클래스 이름은 css/DECISIONS.md 부록 B 의 pagination.css 목록을 따른다.
--%>
<c:if test="${pageData.totalPages > 1}">
<div class="paging">
	<c:if test="${pageData.hasPrev}">
		<a class="paging_prev" href="${ctx}${pageUrl}page=${pageData.page - 1}">이전</a>
	</c:if>

	<c:forEach var="i" begin="${pageData.blockStart}" end="${pageData.blockEnd}">
		<c:choose>
			<c:when test="${i == pageData.page}">
				<strong class="paging_num on">${i}</strong>
			</c:when>
			<c:otherwise>
				<a class="paging_num" href="${ctx}${pageUrl}page=${i}">${i}</a>
			</c:otherwise>
		</c:choose>
	</c:forEach>

	<c:if test="${pageData.hasNext}">
		<a class="paging_next" href="${ctx}${pageUrl}page=${pageData.page + 1}">다음</a>
	</c:if>
</div>
<p>전체 ${pageData.total}건 / ${pageData.page}·${pageData.totalPages} 페이지</p>
</c:if>
