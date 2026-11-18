<%@ include file="../common/header.jsp" %>
<article class="notice_view">
	<h2><c:out value="${notice.title}" /></h2>
	<p class="field_help">
		<fmt:formatDate value="${notice.createdAt}" pattern="yyyy-MM-dd HH:mm" />
		· 조회 ${notice.viewCount}
	</p>
	<div class="notice_content"><c:out value="${notice.content}" /></div>
</article>
<div class="btnstyle">
	<a class="btn btn_sub" href="${ctx}/notice/list">목록</a>
</div>
<%@ include file="../common/footer.jsp" %>
