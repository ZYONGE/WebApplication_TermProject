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

<h2>마이페이지</h2>

<table class="formtable">
	<tr><th>아이디</th><td><c:out value="${member.loginId}" /></td></tr>
	<tr><th>이름</th><td><c:out value="${member.name}" /></td></tr>
	<tr><th>이메일</th><td><c:out value="${member.email}" /></td></tr>
	<tr><th>가입일</th><td><fmt:formatDate value="${member.createdAt}" pattern="yyyy-MM-dd" /></td></tr>
	<tr><th>찜한 상품</th><td>${wishCount}개</td></tr>
</table>

<h3>최근 주문</h3>
<table class="listtable">
	<thead class="listtable_head">
		<tr><th class="col_no">주문번호</th><th class="col_date">주문일</th>
		    <th class="col_total">결제금액</th><th class="col_state">배송상태</th></tr>
	</thead>
	<tbody>
		<c:forEach var="order" items="${recentOrders}">
			<tr>
				<td class="col_no">
					<a href="${ctx}/order/view?orderId=${order.orderId}"><c:out value="${order.orderNo}" /></a>
				</td>
				<td class="col_date"><fmt:formatDate value="${order.orderedAt}" pattern="yyyy-MM-dd" /></td>
				<td class="col_total"><fmt:formatNumber value="${order.payAmount}" pattern="#,##0" />원</td>
				<td class="col_state"><c:out value="${empty order.delivery ? '-' : order.delivery.status}" /></td>
			</tr>
		</c:forEach>
		<c:if test="${empty recentOrders}">
			<tr><td colspan="4" class="empty">주문 내역이 없습니다.</td></tr>
		</c:if>
	</tbody>
</table>

<h3>내가 쓴 글</h3>
<p>리뷰 ${empty reviews ? 0 : reviews.size()}건 / 문의 ${empty qnaList ? 0 : qnaList.size()}건</p>
<%@ include file="/common/footer.jsp" %>
