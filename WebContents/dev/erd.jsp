<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<c:set var="pageCss" value="admin" />
<%@ include file="/common/header.jsp" %>

<ul class="admin_nav">
	<li><a href="${ctx}/dev/">dev 허브</a></li>
	<li><a href="${ctx}/dev/state">상태 컨트롤러</a></li>
	<li><a href="${ctx}/dev/data">데이터 브라우저</a></li>
	<li><a href="${ctx}/dev/erd">설계 보기</a></li>
	<li><a href="${ctx}/dev/seed">데이터 관리</a></li>
	<li><a href="${ctx}/dev/loginAs">계정 전환</a></li>
	<li><a href="${ctx}/dev/log">감사 로그</a></li>
</ul>

<h2>dev · 설계 보기</h2>

<%--
  업무 테이블을 읽지 않는다. information_schema 만 조회한다.
  DATABASE() 를 쓰므로 스키마명 파라미터가 없고, 인젝션 표면도 없다 (ERD § 6.6).

  ★ Mermaid 렌더링 탭은 빠져 있다. mermaid.min.js 가 저장소에 없고 CDN 로드는
    금지돼 있다 (CLAUDE.md § 5 · ERD § 8 쟁점 8). 파일이 들어오면 탭을 더한다.
--%>
<p class="field_help">
	ERD 다이어그램 탭은 mermaid.min.js 가 없어 제외했습니다.
	아래는 실제 DB 에서 읽은 스키마입니다.
</p>

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
<%@ include file="/common/footer.jsp" %>
