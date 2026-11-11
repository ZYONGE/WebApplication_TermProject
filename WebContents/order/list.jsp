<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%-- 마이페이지 탭(my_nav) 안의 화면이라 mypage.css 를 쓴다. 이 화면은 order_* 클래스를 쓰지 않는다 --%>
<c:set var="pageCss" value="mypage" />
<%@ include file="/common/header.jsp" %>

<ul class="my_nav">
	<li><a href="${ctx}/mypage/">마이페이지</a></li>
	<li><a href="${ctx}/mypage/edit">정보수정</a></li>
	<li><a href="${ctx}/mypage/address">배송지</a></li>
	<li><a href="${ctx}/mypage/wishlist">찜 목록</a></li>
	<li><a href="${ctx}/mypage/combo">내 조합</a></li>
	<li><a href="${ctx}/mypage/review">내 리뷰</a></li>
	<li><a href="${ctx}/mypage/qna">내 문의</a></li>
	<li><a href="${ctx}/order/list">주문 내역</a></li>
</ul>

<h2>주문 내역</h2>

<table class="listtable">
	<thead class="listtable_head">
		<tr>
			<th class="col_no">주문번호</th>
			<th class="col_date">주문일</th>
			<th class="col_name">상품</th>
			<th class="col_total">결제금액</th>
			<th class="col_state">주문상태</th>
			<th class="col_state">배송상태</th>
			<th class="col_action"></th>
		</tr>
	</thead>
	<tbody>
		<c:forEach var="order" items="${pageData.list}">
			<tr class="${order.orderStatus == '취소' ? 'disabled' : ''}">
				<td class="col_no"><c:out value="${order.orderNo}" /></td>
				<td class="col_date"><fmt:formatDate value="${order.orderedAt}" pattern="yyyy-MM-dd" /></td>
				<td class="col_name">
					<c:forEach var="item" items="${order.items}" varStatus="st">
						<c:if test="${st.first}"><c:out value="${item.productName}" /></c:if>
					</c:forEach>
					<c:if test="${fn:length(order.items) > 1}">외 ${fn:length(order.items) - 1}건</c:if>
				</td>
				<td class="col_total"><fmt:formatNumber value="${order.payAmount}" pattern="#,##0" />원</td>
				<td class="col_state"><c:out value="${order.orderStatus}" /></td>
				<%-- 취소 주문은 배송 행이 결제완료로 남아 있다 — 주문 상태를 먼저 본다 (ERD § 7 취소 주문 조회) --%>
				<td class="col_state">
					<c:choose>
						<c:when test="${order.orderStatus == '취소'}">취소</c:when>
						<c:otherwise><c:out value="${empty order.delivery ? '-' : order.delivery.status}" /></c:otherwise>
					</c:choose>
				</td>
				<td class="col_action">
					<a class="btn btn_small" href="${ctx}/order/view?orderId=${order.orderId}">상세</a>
				</td>
			</tr>
		</c:forEach>
		<c:if test="${empty pageData.list}">
			<tr><td colspan="7" class="empty">주문 내역이 없습니다.</td></tr>
		</c:if>
	</tbody>
</table>

<%@ include file="/common/paging.jsp" %>
<%@ include file="/common/footer.jsp" %>
