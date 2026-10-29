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

<h2>문의 답변</h2>

<form class="admin_filter" action="${ctx}/admin/qna" method="get">
	<select name="filter">
		<option value="wait" ${onlyUnanswered ? 'selected' : ''}>미답변만</option>
		<option value="all" ${onlyUnanswered ? '' : 'selected'}>전체</option>
	</select>
	<input type="text" name="keyword" value="<c:out value='${param.keyword}' />"
	       placeholder="상품명 · 아이디 · 내용">
	<button type="submit" class="btn">검색</button>
</form>

<%-- 미답변 판정은 answered_at IS NULL 이다 (answer IS NULL 이 아니다 — ERD § 2.14) --%>
<table class="listtable">
	<thead class="listtable_head">
		<tr><th>번호</th><th>상품</th><th>작성자</th><th>문의</th>
		    <th>비밀</th><th>작성일</th><th>상태</th><th>답변</th></tr>
	</thead>
	<tbody>
		<c:forEach var="q" items="${pageData.list}">
			<tr>
				<td>${q.qnaId}</td>
				<td><c:out value="${q.productName}" /></td>
				<td><c:out value="${q.memberName}" /></td>
				<td>
					<c:out value="${q.question}" />
					<c:if test="${q.answered}">
						<div class="qna_answer">답변: <c:out value="${q.answer}" /></div>
					</c:if>
				</td>
				<td>${q.secretPost ? 'Y' : 'N'}</td>
				<td><fmt:formatDate value="${q.createdAt}" pattern="yyyy-MM-dd" /></td>
				<td>${q.answered ? '답변완료' : '미답변'}</td>
				<td>
					<button type="button" class="btn btn_small"
					        onclick="openAnswer(${q.qnaId}, this)">${q.answered ? '답변 수정' : '답변 쓰기'}</button>
					<span hidden><c:out value="${q.answer}" /></span>
				</td>
			</tr>
		</c:forEach>
		<c:if test="${empty pageData.list}"><tr><td colspan="8" class="empty">문의가 없습니다.</td></tr></c:if>
	</tbody>
</table>

<%@ include file="/common/paging.jsp" %>

<dialog id="answerPopup" class="popup">
	<form class="popup_form" action="${ctx}/admin/qna" method="post" data-validate>
		<input type="hidden" name="qnaId" id="answerQnaId">

		<h3 class="popup_head">문의 답변</h3>
		<div class="popup_body">
			<label for="answer">답변 내용</label>
			<textarea id="answer" name="answer" rows="6" class="field" data-rule="required"></textarea>
			<%-- answer 와 answered_at 은 같은 UPDATE 로 함께 쓰인다 (chk_qna_answered) --%>
			<span class="field_help">답변을 수정하면 답변일시도 함께 갱신됩니다.</span>
		</div>
		<div class="popup_foot">
			<button type="submit" class="btn btn_primary">등록</button>
			<button type="button" class="btn btn_sub popup_close" data-popup-close="answerPopup">닫기</button>
		</div>
	</form>
</dialog>

<script>
function openAnswer(qnaId, button) {
	document.getElementById("answerQnaId").value = qnaId;
	var holder = button.parentNode.querySelector("span[hidden]");
	document.getElementById("answer").value = holder ? holder.textContent.trim() : "";
	window.openPopup("answerPopup");
}
</script>
<%@ include file="/common/footer.jsp" %>
