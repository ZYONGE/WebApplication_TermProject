<%@ include file="../common/header.jsp" %>
<%@ include file="../common/mynav.jsp" %>
<h2>내 문의</h2>
<table class="listtable">
	<thead class="listtable_head">
		<tr><th class="col_name">상품</th><th class="col_content">문의</th>
		    <th class="col_state">비밀</th><th class="col_date">작성일</th>
		    <th class="col_state">답변</th><th class="col_action"></th></tr>
	</thead>
	<tbody>
		<c:forEach var="q" items="${qnaList}">
			<tr>
				<td class="col_name">
					<a href="${ctx}/product/view?productId=${q.productId}"><c:out value="${q.productName}" /></a>
				</td>
				<td class="col_content">
					<c:out value="${q.question}" />
					<c:if test="${q.answered}">
						<div class="qna_answer">
							답변: <c:out value="${q.answer}" />
							(<fmt:formatDate value="${q.answeredAt}" pattern="yyyy-MM-dd" />)
						</div>
					</c:if>
				</td>
				<td class="col_state">${q.secretPost ? '비밀' : '공개'}</td>
				<td class="col_date"><fmt:formatDate value="${q.createdAt}" pattern="yyyy-MM-dd" /></td>
				<td class="col_state">${q.answered ? '답변완료' : '미답변'}</td>
				<td class="col_action">
					<c:if test="${not q.answered}">
						<button type="button" class="btn btn_small" data-popup-open="qnaPopup" data-fill-qna-id="${q.qnaId}" data-fill-is-secret="${q.secretPost}"
						        data-fill-question="<c:out value='${q.question}' />">수정</button>
					</c:if>
					<form action="${ctx}/mypage/qna" method="post">
						<input type="hidden" name="mode" value="delete">
						<input type="hidden" name="qnaId" value="${q.qnaId}">
						<button type="submit" class="btn btn_danger btn_small"
						        data-confirm-message="문의를 삭제하시겠습니까?">삭제</button>
					</form>
				</td>
			</tr>
		</c:forEach>
		<c:if test="${empty qnaList}">
			<tr><td colspan="6" class="empty">작성한 문의가 없습니다.</td></tr>
		</c:if>
	</tbody>
</table>
<p class="field_help">문의 작성은 상품 상세 화면에서 할 수 있습니다.</p>
<dialog id="qnaPopup" class="popup">
	<form class="popup_form" action="${ctx}/mypage/qna" method="post" data-validate>
		<input type="hidden" name="mode" value="update">
		<input type="hidden" name="qnaId" id="qnaId">
		<h3 class="popup_head">문의 수정</h3>
		<div class="popup_body">
			<label for="question">문의 내용</label>
			<textarea id="question" name="question" rows="5" class="field" data-rule="required"></textarea>
			<label><input type="checkbox" name="isSecret" id="isSecret" value="Y"> 비밀글</label>
		</div>
		<div class="popup_foot">
			<button type="submit" class="btn btn_primary">저장</button>
			<button type="button" class="btn btn_sub popup_close" data-popup-close="qnaPopup">닫기</button>
		</div>
	</form>
</dialog>
<%@ include file="../common/footer.jsp" %>
