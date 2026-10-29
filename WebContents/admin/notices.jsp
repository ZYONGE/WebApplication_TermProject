<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ include file="/common/header.jsp" %>

<ul class="admin_nav">
	<li><a href="${ctx}/admin/dashboard">대시보드</a></li>
	<li><a href="${ctx}/admin/products">상품</a></li>
	<li><a href="${ctx}/admin/categories">카테고리</a></li>
	<li><a href="${ctx}/admin/orders">주문·배송</a></li>
	<li><a href="${ctx}/admin/members">회원</a></li>
	<li><a href="${ctx}/admin/qna">문의답변</a></li>
	<li><a href="${ctx}/admin/reviews">리뷰</a></li>
	<li><a href="${ctx}/admin/notices">공지</a></li>
</ul>

<h2>공지 관리</h2>

<form class="admin_filter" action="${ctx}/admin/notices" method="get">
	<input type="text" name="keyword" value="<c:out value='${param.keyword}' />" placeholder="제목">
	<button type="submit" class="btn">검색</button>
	<a class="btn btn_primary" href="${ctx}/admin/notices?create=1">공지 등록</a>
</form>

<table class="listtable">
	<thead class="listtable_head">
		<tr><th>번호</th><th>제목</th><th>조회</th><th>등록일</th><th></th></tr>
	</thead>
	<tbody>
		<c:forEach var="n" items="${pageData.list}">
			<tr>
				<td>${n.noticeId}</td>
				<td><a href="${ctx}/notice/view?noticeId=${n.noticeId}"><c:out value="${n.title}" /></a></td>
				<td>${n.viewCount}</td>
				<td><fmt:formatDate value="${n.createdAt}" pattern="yyyy-MM-dd" /></td>
				<td>
					<a class="btn btn_small" href="${ctx}/admin/notices?editId=${n.noticeId}">수정</a>
					<form action="${ctx}/admin/notices" method="post">
						<input type="hidden" name="mode" value="delete">
						<input type="hidden" name="noticeId" value="${n.noticeId}">
						<button type="submit" class="btn btn_danger btn_small"
						        data-confirm-message="공지를 삭제하시겠습니까?">삭제</button>
					</form>
				</td>
			</tr>
		</c:forEach>
		<c:if test="${empty pageData.list}"><tr><td colspan="5" class="empty">공지가 없습니다.</td></tr></c:if>
	</tbody>
</table>

<%@ include file="/common/paging.jsp" %>

<dialog id="noticePopup" class="popup">
	<form class="popup_form" action="${ctx}/admin/notices" method="post" data-validate>
		<input type="hidden" name="mode" value="save">
		<input type="hidden" name="noticeId" value="${empty editNotice ? 0 : editNotice.noticeId}">

		<h3 class="popup_head">${empty editNotice ? '공지 등록' : '공지 수정'}</h3>
		<div class="popup_body">
			<label for="title">제목</label>
			<input type="text" id="title" name="title" class="field" data-rule="required"
			       value="<c:out value='${editNotice.title}' />">
			<label for="content">내용</label>
			<textarea id="content" name="content" rows="10" class="field"
			          data-rule="required"><c:out value="${editNotice.content}" /></textarea>
		</div>
		<div class="popup_foot">
			<button type="submit" class="btn btn_primary">저장</button>
			<button type="button" class="btn btn_sub popup_close" data-popup-close="noticePopup">닫기</button>
		</div>
	</form>
</dialog>

<c:if test="${not empty editNotice or param.create == '1'}">
<script>
document.addEventListener("DOMContentLoaded", function () {
	window.openPopup("noticePopup");
});
</script>
</c:if>
<%@ include file="/common/footer.jsp" %>
