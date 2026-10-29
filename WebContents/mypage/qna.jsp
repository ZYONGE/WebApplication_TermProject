<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ include file="/common/header.jsp" %>

<ul class="my_nav">
	<li><a href="${ctx}/mypage/">마이페이지</a></li>
	<li><a href="${ctx}/mypage/edit">정보수정</a></li>
	<li><a href="${ctx}/mypage/address">배송지</a></li>
	<li><a href="${ctx}/mypage/wishlist">찜 목록</a></li>
	<li><a href="${ctx}/mypage/review">내 리뷰</a></li>
	<li><a href="${ctx}/mypage/qna">내 문의</a></li>
	<li><a href="${ctx}/order/list">주문 내역</a></li>
</ul>

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
				<td class="col_state">${q.secretPost ? 'Y' : 'N'}</td>
				<td class="col_date"><fmt:formatDate value="${q.createdAt}" pattern="yyyy-MM-dd" /></td>
				<td class="col_state">${q.answered ? '답변완료' : '미답변'}</td>
				<td class="col_action">
					<%-- 답변이 달린 문의는 고칠 수 없다 — 답변이 다른 질문에 붙게 된다. --%>
					<c:if test="${not q.answered}">
						<button type="button" class="btn btn_small"
						        onclick="editQna(${q.qnaId}, this, ${q.secretPost})">수정</button>
						<span hidden><c:out value="${q.question}" /></span>
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

<script>
function editQna(qnaId, button, secret) {
	document.getElementById("qnaId").value = qnaId;
	document.getElementById("isSecret").checked = secret;
	var holder = button.parentNode.querySelector("span[hidden]");
	document.getElementById("question").value = holder ? holder.textContent.trim() : "";
	window.openPopup("qnaPopup");
}
</script>
<%@ include file="/common/footer.jsp" %>
