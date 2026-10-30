<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<c:set var="pageCss" value="notice" />
<%@ include file="/common/header.jsp" %>

<h2>공지사항</h2>

<form class="notice_search" action="${ctx}/notice/list" method="get">
	<label for="keyword">제목 검색</label>
	<input type="text" id="keyword" name="keyword" value="<c:out value='${param.keyword}' />">
	<button type="submit" class="btn">검색</button>
</form>

<table class="listtable">
	<thead class="listtable_head">
		<tr><th class="col_no">번호</th><th class="col_name">제목</th>
		    <th class="col_date">등록일</th><th class="col_qty">조회</th></tr>
	</thead>
	<tbody>
		<c:forEach var="n" items="${pageData.list}">
			<tr>
				<td class="col_no">${n.noticeId}</td>
				<td class="col_name">
					<a href="${ctx}/notice/view?noticeId=${n.noticeId}"><c:out value="${n.title}" /></a>
				</td>
				<td class="col_date"><fmt:formatDate value="${n.createdAt}" pattern="yyyy-MM-dd" /></td>
				<td class="col_qty">${n.viewCount}</td>
			</tr>
		</c:forEach>
		<c:if test="${empty pageData.list}">
			<tr><td colspan="4" class="empty">등록된 공지가 없습니다.</td></tr>
		</c:if>
	</tbody>
</table>

<%@ include file="/common/paging.jsp" %>
<%@ include file="/common/footer.jsp" %>
