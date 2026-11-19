<%@ include file="../common/header.jsp" %>
<%@ include file="../common/devnav.jsp" %>
<h2>dev · 설계 보기</h2>
<h3>ERD 다이어그램</h3>
<c:choose>
	<c:when test="${empty erdEntityCount}">
		<p class="empty">ERD 원본(README.md 의 mermaid 블록)을 찾지 못했습니다.</p>
	</c:when>
	<c:otherwise>
		<p class="field_help">
			원본: <c:out value="${erdOrigin}" /> · 엔티티 ${erdEntityCount}개 · 관계 ${erdRelationCount}개
			· 그림을 누르면 원본 크기로 열립니다
		</p>
		<div class="dev_erd">
			<a href="${ctx}/dev/erd/diagram.svg" target="_blank" title="원본 크기로 보기">${erdSvg}</a>
		</div>
	</c:otherwise>
</c:choose>
<h2>실제 스키마 (information_schema)</h2>
<h3>컬럼 (information_schema.columns)</h3>
<table class="listtable">
	<thead class="listtable_head">
		<tr><th>테이블</th><th>컬럼</th><th>순서</th><th>타입</th>
		    <th>NULL</th><th>키</th><th>기본값</th><th>기타</th></tr>
	</thead>
	<tbody>
		<c:forEach var="c" items="${columns}">
			<tr>
				<td><c:out value="${c.table_name}" /></td>
				<td><c:out value="${c.column_name}" /></td>
				<td><c:out value="${c.ordinal_position}" /></td>
				<td><c:out value="${c.column_type}" /></td>
				<td><c:out value="${c.is_nullable}" /></td>
				<td><c:out value="${c.column_key}" /></td>
				<td><c:out value="${c.column_default}" /></td>
				<td><c:out value="${c.extra}" /></td>
			</tr>
		</c:forEach>
	</tbody>
</table>
<h3>제약 (table_constraints + key_column_usage)</h3>
<table class="listtable">
	<thead class="listtable_head">
		<tr><th>테이블</th><th>제약명</th><th>종류</th><th>컬럼</th>
		    <th>참조 테이블</th><th>참조 컬럼</th><th>ON DELETE</th><th>ON UPDATE</th></tr>
	</thead>
	<tbody>
		<c:forEach var="c" items="${constraints}">
			<tr>
				<td><c:out value="${c.table_name}" /></td>
				<td><c:out value="${c.constraint_name}" /></td>
				<td><c:out value="${c.constraint_type}" /></td>
				<td><c:out value="${c.column_name}" /></td>
				<td><c:out value="${c.referenced_table_name}" /></td>
				<td><c:out value="${c.referenced_column_name}" /></td>
				<td><c:out value="${c.delete_rule}" /></td>
				<td><c:out value="${c.update_rule}" /></td>
			</tr>
		</c:forEach>
	</tbody>
</table>
<h3>CHECK 제약 (MySQL 8.0.16+)</h3>
<table class="listtable">
	<thead class="listtable_head"><tr><th>제약명</th><th>조건</th></tr></thead>
	<tbody>
		<c:forEach var="c" items="${checks}">
			<tr>
				<td><c:out value="${c.constraint_name}" /></td>
				<td><c:out value="${c.check_clause}" /></td>
			</tr>
		</c:forEach>
	</tbody>
</table>
<%@ include file="../common/footer.jsp" %>
