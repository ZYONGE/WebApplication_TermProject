<%@ include file="../common/header.jsp" %>
<%@ include file="../common/adminnav.jsp" %>
<h2>리뷰 관리</h2>
<form class="admin_filter" action="${ctx}/admin/reviews" method="get">
	<input type="text" name="keyword" value="<c:out value='${param.keyword}' />"
	       placeholder="상품명 · 아이디 · 내용">
	<button type="submit" class="btn">검색</button>
</form>
<table class="listtable">
	<thead class="listtable_head">
		<tr><th>번호</th><th>상품</th><th>작성자</th><th>별점</th>
		    <th>내용</th><th>작성일</th><th></th></tr>
	</thead>
	<tbody>
		<c:forEach var="r" items="${pageData.list}">
			<tr>
				<td>${r.reviewId}</td>
				<td><c:out value="${r.productName}" /></td>
				<td><c:out value="${r.memberName}" /></td>
				<td>${r.rating} / 5</td>
				<td><c:out value="${r.content}" /></td>
				<td><fmt:formatDate value="${r.createdAt}" pattern="yyyy-MM-dd" /></td>
				<td>
					<form action="${ctx}/admin/reviews" method="post">
						<input type="hidden" name="reviewId" value="${r.reviewId}">
						<button type="submit" class="btn btn_danger btn_small"
						        data-confirm-message="리뷰를 삭제하시겠습니까? 되돌릴 수 없습니다.">삭제</button>
					</form>
				</td>
			</tr>
		</c:forEach>
		<c:if test="${empty pageData.list}"><tr><td colspan="7" class="empty">리뷰가 없습니다.</td></tr></c:if>
	</tbody>
</table>
<%@ include file="../common/paging.jsp" %>
<%@ include file="../common/footer.jsp" %>
