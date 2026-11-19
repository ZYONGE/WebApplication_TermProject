<%@ include file="../common/header.jsp" %>
<%@ include file="../common/adminnav.jsp" %>
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
					<button type="button" class="btn btn_small" data-popup-open="answerPopup" data-fill-answer-qna-id="${q.qnaId}"
					        data-fill-answer-target="문의 #${q.qnaId} — <c:out value='${q.productName}' />" data-fill-answer-question="<c:out value='${q.question}' />"
					        data-fill-answer="<c:out value='${q.answer}' />">${q.answered ? '답변 수정' : '답변 쓰기'}</button>
				</td>
			</tr>
		</c:forEach>
		<c:if test="${empty pageData.list}"><tr><td colspan="8" class="empty">문의가 없습니다.</td></tr></c:if>
	</tbody>
</table>
<%@ include file="../common/paging.jsp" %>
<dialog id="answerPopup" class="popup">
	<form class="popup_form" action="${ctx}/admin/qna" method="post" data-validate>
		<input type="hidden" name="qnaId" id="answerQnaId">
		<h3 class="popup_head">문의 답변</h3>
		<div class="popup_body">
			<p id="answerTarget"></p>
			<blockquote id="answerQuestion"></blockquote>
			<label for="answer">답변 내용</label>
			<textarea id="answer" name="answer" rows="6" class="field" data-rule="required"></textarea>
			<span class="field_help">답변을 수정하면 답변일시도 함께 갱신됩니다.</span>
		</div>
		<div class="popup_foot">
			<button type="submit" class="btn btn_primary">등록</button>
			<button type="button" class="btn btn_sub popup_close" data-popup-close="answerPopup">닫기</button>
		</div>
	</form>
</dialog>
<%@ include file="../common/footer.jsp" %>
