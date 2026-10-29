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

<h2>찜 목록</h2>

<%--
  장바구니와 같은 이유로 노출 조건을 걸지 않는다 — 내가 찜해 둔 기록이기 때문이다.
  판매가 중지된 상품은 그 사실만 표시한다 (CRUD.md § 2 주의).
--%>
<table class="listtable">
	<thead class="listtable_head">
		<tr><th class="col_name">상품</th><th class="col_price">가격</th>
		    <th class="col_state">상태</th><th class="col_date">찜한 날</th><th class="col_action"></th></tr>
	</thead>
	<tbody>
		<c:forEach var="w" items="${items}">
			<tr class="${w.status == '판매중' ? '' : 'soldout'}">
				<td class="col_name">
					<a href="${ctx}/product/view?productId=${w.productId}"><c:out value="${w.productName}" /></a>
				</td>
				<td class="col_price"><fmt:formatNumber value="${w.price}" pattern="#,##0" />원</td>
				<td class="col_state"><c:out value="${w.status}" /></td>
				<td class="col_date"><fmt:formatDate value="${w.createdAt}" pattern="yyyy-MM-dd" /></td>
				<td class="col_action">
					<form action="${ctx}/wish/remove" method="post">
						<input type="hidden" name="productId" value="${w.productId}">
						<input type="hidden" name="back" value="/mypage/wishlist">
						<button type="submit" class="btn btn_small">찜 해제</button>
					</form>
				</td>
			</tr>
		</c:forEach>
		<c:if test="${empty items}">
			<tr><td colspan="5" class="empty">찜한 상품이 없습니다.</td></tr>
		</c:if>
	</tbody>
</table>
<%@ include file="/common/footer.jsp" %>
