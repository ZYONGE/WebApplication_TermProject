<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<c:set var="pageCss" value="admin" />
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

<h2>대시보드</h2>

<table class="formtable">
	<tr><th>활동 회원</th><td>${summary.memberCount}명</td></tr>
	<tr><th>판매중 상품</th><td>${summary.productCount}개</td></tr>
	<tr><th>정상 주문</th><td>${summary.orderCount}건</td></tr>
	<tr><th>오늘 주문</th><td>${summary.todayOrderCount}건 / <fmt:formatNumber value="${summary.todayAmount}" pattern="#,##0" />원</td></tr>
	<tr><th>미답변 문의</th><td><a href="${ctx}/admin/qna">${summary.qnaWaiting}건</a></td></tr>
</table>

<h3>배송 상태별 건수</h3>
<%--
  뷰는 건수 0 인 상태의 행을 내보내지 않는다. 4단계를 항상 보여주기 위해
  서버(StatsDAO)에서 0 으로 깔고 조회 결과로 덮어쓴다.
--%>
<table class="listtable">
	<thead class="listtable_head"><tr><th>상태</th><th>건수</th></tr></thead>
	<tbody>
		<c:forEach var="d" items="${deliveryCounts}">
			<tr>
				<td><c:out value="${d.deliveryStatus}" /></td>
				<%-- 집계 뷰는 정상 주문만 센다. 링크도 같은 조건을 실어야 숫자와 목록이 맞는다. --%>
				<td><a href="${ctx}/admin/orders?deliveryStatus=${d.deliveryStatus}&orderStatus=정상">${d.orderCount}건</a></td>
			</tr>
		</c:forEach>
	</tbody>
</table>

<h3>최근 7일 매출 (v_sales_daily)</h3>
<table class="listtable">
	<thead class="listtable_head">
		<tr><th>일자</th><th>주문수</th><th>상품금액</th><th>배송비</th><th>합계</th></tr>
	</thead>
	<tbody>
		<c:forEach var="s" items="${salesDaily}">
			<tr>
				<td><fmt:formatDate value="${s.salesDate}" pattern="yyyy-MM-dd" /></td>
				<td>${s.orderCount}</td>
				<td><fmt:formatNumber value="${s.goodsAmount}" pattern="#,##0" /></td>
				<td><fmt:formatNumber value="${s.deliveryAmount}" pattern="#,##0" /></td>
				<td><fmt:formatNumber value="${s.totalAmount}" pattern="#,##0" /></td>
			</tr>
		</c:forEach>
		<c:if test="${empty salesDaily}"><tr><td colspan="5" class="empty">매출 데이터가 없습니다.</td></tr></c:if>
	</tbody>
</table>

<h3>재고 부족 (기준 ${lowStockThreshold}개 이하)</h3>
<%--
  노출 규칙 때문에 stock = 0 인 상품은 고객 화면에서 조용히 사라진다.
  관리자가 그 사실을 알 수 있는 유일한 경로가 이 표다 (v_low_stock).
--%>
<table class="listtable">
	<thead class="listtable_head">
		<tr><th>상품</th><th>카테고리</th><th>재고</th><th>상태</th></tr>
	</thead>
	<tbody>
		<c:forEach var="p" items="${lowStock}">
			<tr class="${p.stock == 0 ? 'soldout' : ''}">
				<td><a href="${ctx}/admin/products?editId=${p.productId}"><c:out value="${p.name}" /></a></td>
				<td><c:out value="${p.categoryName}" /></td>
				<td>${p.stock}<c:if test="${p.stock == 0}"> (고객 화면에서 숨겨짐)</c:if></td>
				<td><c:out value="${p.status}" /></td>
			</tr>
		</c:forEach>
		<c:if test="${empty lowStock}"><tr><td colspan="4" class="empty">재고 부족 상품이 없습니다.</td></tr></c:if>
	</tbody>
</table>
<%@ include file="/common/footer.jsp" %>
