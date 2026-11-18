<%@ include file="../common/header.jsp" %>
<%@ include file="../common/devnav.jsp" %>
<h2>dev · 데이터 브라우저</h2>
<form class="admin_filter" action="${ctx}/dev/data" method="get">
	<select name="table">
		<option value="">테이블 선택</option>
		<c:forEach var="t" items="${tables}">
			<option value="${t}" ${param.table == t ? 'selected' : ''}>${t}</option>
		</c:forEach>
	</select>
	<input type="text" name="sort" value="<c:out value='${param.sort}' />" placeholder="정렬 컬럼">
	<select name="dir">
		<option value="asc" ${param.dir == 'asc' ? 'selected' : ''}>오름차순</option>
		<option value="desc" ${param.dir == 'desc' ? 'selected' : ''}>내림차순</option>
	</select>
	<input type="number" name="size" value="${empty param.size ? 20 : param.size}" min="1" max="100">
	<button type="submit" class="btn">조회</button>
</form>
<c:choose>
	<c:when test="${empty result}">
		<p class="empty">테이블을 선택하세요.</p>
	</c:when>
	<c:otherwise>
		<h3><c:out value="${result.table}" /> — 전체 ${result.total}행</h3>
		<table class="listtable">
			<thead class="listtable_head">
				<tr>
					<c:forEach var="col" items="${result.columns}">
						<th>
							<a href="${ctx}/dev/data?table=${result.table}&sort=${col}&dir=${param.dir == 'asc' ? 'desc' : 'asc'}&size=${result.size}">${col}</a>
						</th>
					</c:forEach>
				</tr>
			</thead>
			<tbody>
				<c:forEach var="row" items="${result.rows}">
					<tr>
						<c:forEach var="cell" items="${row}">
							<td><c:out value="${empty cell ? 'NULL' : cell}" /></td>
						</c:forEach>
					</tr>
				</c:forEach>
				<c:if test="${empty result.rows}">
					<tr><td colspan="${result.columns.size()}" class="empty">행이 없습니다.</td></tr>
				</c:if>
			</tbody>
		</table>
		<div class="paging">
			<c:forEach var="i" begin="1" end="${result.totalPages}">
				<c:choose>
					<c:when test="${i == result.page}"><strong class="paging_num on">${i}</strong></c:when>
					<c:otherwise>
						<a class="paging_num"
						   href="${ctx}/dev/data?table=${result.table}&sort=${param.sort}&dir=${param.dir}&size=${result.size}&page=${i}">${i}</a>
					</c:otherwise>
				</c:choose>
			</c:forEach>
		</div>
	</c:otherwise>
</c:choose>
<%@ include file="../common/footer.jsp" %>
