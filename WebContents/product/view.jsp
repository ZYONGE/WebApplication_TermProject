<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ include file="/common/header.jsp" %>

<article class="prod_detail">
	<h2><c:out value="${product.name}" /></h2>

	<div class="prod_gallery">
		<c:choose>
			<c:when test="${not empty product.thumbnail}">
				<img src="${ctx}${product.thumbnail}" alt="<c:out value='${product.name}' />">
			</c:when>
			<c:otherwise><p>[대표 이미지 없음]</p></c:otherwise>
		</c:choose>
		<c:forEach var="img" items="${product.images}">
			<img src="${ctx}${img.imgPath}" alt="상품 이미지 ${img.sortOrder}">
		</c:forEach>
	</div>

	<table class="formtable">
		<tr><th>카테고리</th><td><c:out value="${product.categoryName}" /></td></tr>
		<tr><th>판매가</th><td><fmt:formatNumber value="${product.price}" pattern="#,##0" />원</td></tr>
		<tr><th>재고</th><td>${product.stock}개</td></tr>
		<tr>
			<th>별점</th>
			<td><fmt:formatNumber value="${product.avgRating}" pattern="0.0" /> / 5 (리뷰 ${product.reviewCount}건)</td>
		</tr>
	</table>

	<div class="prod_desc"><c:out value="${product.description}" /></div>

	<%-- 담기 · 찜은 로그인 필요. LoginFilter 가 /cart/*, /wish/* 를 지킨다. --%>
	<form class="prod_buy" action="${ctx}/cart/add" method="post" data-validate>
		<input type="hidden" name="productId" value="${product.productId}">
		<input type="hidden" name="back" value="/product/view?productId=${product.productId}">
		<label for="quantity">수량</label>
		<input type="number" id="quantity" name="quantity" value="1" min="1" max="${product.stock}"
		       data-rule="positiveInt">
		<button type="submit" class="btn btn_primary">장바구니 담기</button>
	</form>

	<form class="prod_wish" action="${ctx}/wish/toggle" method="post">
		<input type="hidden" name="productId" value="${product.productId}">
		<input type="hidden" name="back" value="/product/view?productId=${product.productId}">
		<button type="submit" class="btn btn_sub">${wished ? '찜 해제' : '찜하기'}</button>
	</form>
</article>

<section class="prod_review">
	<h3>리뷰 (${product.reviewCount}건)</h3>
	<%--
	  모든 리뷰는 구매 확인된 글이다 — review.item_id 가 NOT NULL 이라
	  구매하지 않은 사람이 쓸 수 없다 (CRUD.md § 6-3).
	  작성자명은 서버에서 마스킹해서 내려온다.
	--%>
	<table class="listtable">
		<thead class="listtable_head">
			<tr><th class="col_rating">별점</th><th class="col_content">내용</th>
			    <th class="col_writer">작성자</th><th class="col_date">작성일</th></tr>
		</thead>
		<tbody>
			<c:forEach var="r" items="${reviews}">
				<tr>
					<td class="col_rating">${r.rating} / 5</td>
					<td class="col_content">
						<c:out value="${r.content}" />
						<span class="prodcard_badge">구매자 작성</span>
					</td>
					<td class="col_writer"><c:out value="${r.memberName}" /></td>
					<td class="col_date"><fmt:formatDate value="${r.createdAt}" pattern="yyyy-MM-dd" /></td>
				</tr>
			</c:forEach>
			<c:if test="${empty reviews}">
				<tr><td colspan="4" class="empty">등록된 리뷰가 없습니다.</td></tr>
			</c:if>
		</tbody>
	</table>
	<p class="field_help">리뷰는 배송완료된 주문 건에서만 쓸 수 있습니다. (마이페이지 &gt; 내 리뷰)</p>
</section>

<section class="prod_qna">
	<h3>상품 문의</h3>
	<c:if test="${not empty sessionScope.loginMember}">
		<button type="button" class="btn" data-popup-open="qnaPopup">문의하기</button>
	</c:if>

	<table class="listtable">
		<thead class="listtable_head">
			<tr><th class="col_content">문의</th><th class="col_writer">작성자</th>
			    <th class="col_date">작성일</th><th class="col_state">답변</th></tr>
		</thead>
		<tbody>
			<c:forEach var="q" items="${qnaList}">
				<tr>
					<td class="col_content">
						<%-- 비밀글은 서버가 본문을 비워서 내려보낸다. 화면은 가리는 일을 하지 않는다. --%>
						<c:choose>
							<c:when test="${q.secretPost and empty q.question}">비밀글입니다.</c:when>
							<c:otherwise>
								<c:out value="${q.question}" />
								<c:if test="${q.answered}">
									<div class="qna_answer">답변: <c:out value="${q.answer}" /></div>
								</c:if>
							</c:otherwise>
						</c:choose>
					</td>
					<td class="col_writer"><c:out value="${q.memberName}" /></td>
					<td class="col_date"><fmt:formatDate value="${q.createdAt}" pattern="yyyy-MM-dd" /></td>
					<td class="col_state">${q.answered ? '답변완료' : '미답변'}</td>
				</tr>
			</c:forEach>
			<c:if test="${empty qnaList}">
				<tr><td colspan="4" class="empty">등록된 문의가 없습니다.</td></tr>
			</c:if>
		</tbody>
	</table>
</section>

<%--
  팝업 — HTML 표준 <dialog> 를 쓴다.

  css/DECISIONS.md O-02(별창 window.open vs 인페이지 모달)가 아직 미결이라
  "CSS 없이도 모달로 동작하는" 쪽을 골랐다. 확정되면 그에 맞춰 바꾼다.
  클래스 이름은 부록 B 의 popup.css 목록을 그대로 쓴다.
--%>
<dialog id="qnaPopup" class="popup">
	<form class="popup_form" action="${ctx}/mypage/qna" method="post" data-validate>
		<input type="hidden" name="mode" value="write">
		<input type="hidden" name="productId" value="${product.productId}">
		<input type="hidden" name="back" value="/product/view?productId=${product.productId}">

		<h3 class="popup_head">상품 문의 작성</h3>
		<div class="popup_body">
			<p><c:out value="${product.name}" /></p>
			<label for="question">문의 내용</label>
			<textarea id="question" name="question" rows="5" class="field" data-rule="required"></textarea>
			<label><input type="checkbox" name="isSecret" value="Y"> 비밀글로 작성</label>
		</div>
		<div class="popup_foot">
			<button type="submit" class="btn btn_primary">등록</button>
			<button type="button" class="btn btn_sub popup_close" data-popup-close="qnaPopup">닫기</button>
		</div>
	</form>
</dialog>
<%@ include file="/common/footer.jsp" %>
