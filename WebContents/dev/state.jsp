<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<c:set var="pageCss" value="admin" />
<%@ include file="/common/header.jsp" %>

<ul class="admin_nav">
	<li><a href="${ctx}/dev/">dev 허브</a></li>
	<li><a href="${ctx}/dev/state">상태 컨트롤러</a></li>
	<li><a href="${ctx}/dev/data">데이터 브라우저</a></li>
	<li><a href="${ctx}/dev/erd">설계 보기</a></li>
	<li><a href="${ctx}/dev/seed">데이터 관리</a></li>
	<li><a href="${ctx}/dev/loginAs">계정 전환</a></li>
	<li><a href="${ctx}/dev/log">감사 로그</a></li>
</ul>

<h2>dev · 상태 컨트롤러</h2>

<h3>점검 모드 · 전역 설정</h3>
<form action="${ctx}/dev/state" method="post">
	<input type="hidden" name="mode" value="shop">
	<button type="submit" class="btn btn_primary">점검 모드 토글</button>
</form>

<table class="listtable">
	<thead class="listtable_head"><tr><th>키</th><th>현재 값</th><th>변경</th></tr></thead>
	<tbody>
		<c:forEach var="cfg" items="${configs}">
			<tr>
				<td><c:out value="${cfg.configKey}" /><br><span class="field_help"><c:out value="${cfg.description}" /></span></td>
				<td><strong><c:out value="${cfg.configValue}" /></strong></td>
				<td>
					<form action="${ctx}/dev/state" method="post">
						<input type="hidden" name="mode" value="config">
						<input type="hidden" name="configKey" value="${cfg.configKey}">
						<input type="text" name="configValue" value="<c:out value='${cfg.configValue}' />">
						<button type="submit" class="btn btn_small">변경</button>
					</form>
				</td>
			</tr>
		</c:forEach>
	</tbody>
</table>

<h3>재고 강제 지정</h3>
<%-- 0 으로 두면 노출 규칙 때문에 고객 화면에서 사라진다 — 그걸 눈으로 확인하는 장치다 --%>
<form class="formtable_form" action="${ctx}/dev/state" method="post">
	<input type="hidden" name="mode" value="stock">
	<select name="productId">
		<c:forEach var="p" items="${products}">
			<option value="${p.productId}">
				[${p.productId}] <c:out value="${p.name}" /> (현재 ${p.stock}개 · <c:out value="${p.status}" />)
			</option>
		</c:forEach>
	</select>
	<input type="number" name="stock" value="0" min="0">
	<button type="submit" class="btn">재고 설정</button>
</form>

<h3>주문별 강제 조작</h3>
<form class="admin_filter" action="${ctx}/dev/state" method="get">
	<input type="text" name="keyword" value="<c:out value='${param.keyword}' />" placeholder="주문번호 · 아이디">
	<button type="submit" class="btn">검색</button>
</form>

<table class="listtable">
	<thead class="listtable_head">
		<tr><th>주문번호</th><th>회원</th><th>주문상태</th><th>결제상태</th><th>배송상태</th><th>강제 변경</th></tr>
	</thead>
	<tbody>
		<c:forEach var="o" items="${pageData.list}">
			<tr>
				<td><a href="${ctx}/dev/state?orderId=${o.orderId}"><c:out value="${o.orderNo}" /></a></td>
				<td><c:out value="${o.loginId}" /></td>
				<td><c:out value="${o.orderStatus}" /></td>
				<td><c:out value="${empty o.payment ? '-' : o.payment.payStatus}" /></td>
				<td><c:out value="${empty o.delivery ? '-' : o.delivery.status}" /></td>
				<td>
					<c:if test="${not empty o.delivery}">
						<form action="${ctx}/dev/state" method="post">
							<input type="hidden" name="mode" value="delivery">
							<input type="hidden" name="deliveryId" value="${o.delivery.deliveryId}">
							<select name="status">
								<c:forEach var="s" items="${flow}">
									<option value="${s}" ${o.delivery.status == s ? 'selected' : ''}>${s}</option>
								</c:forEach>
							</select>
							<%-- 배송중 이상으로 밀면 운송장이 필요하다. 비우면 서버가 DEV 값으로 채운다. --%>
							<input type="text" name="courier" placeholder="택배사" size="8">
							<input type="text" name="trackingNo" placeholder="운송장" size="10">
							<button type="submit" class="btn btn_small">배송 강제</button>
						</form>
					</c:if>

					<form action="${ctx}/dev/state" method="post">
						<input type="hidden" name="mode" value="order">
						<input type="hidden" name="orderId" value="${o.orderId}">
						<select name="status">
							<option value="정상" ${o.orderStatus == '정상' ? 'selected' : ''}>정상</option>
							<option value="취소" ${o.orderStatus == '취소' ? 'selected' : ''}>취소</option>
						</select>
						<label><input type="checkbox" name="restoreStock" value="Y" checked> 재고 반영</label>
						<button type="submit" class="btn btn_small">주문 강제</button>
					</form>

					<form action="${ctx}/dev/state" method="post">
						<input type="hidden" name="mode" value="pay">
						<input type="hidden" name="orderId" value="${o.orderId}">
						<select name="status">
							<option value="완료">완료</option>
							<option value="환불">환불</option>
						</select>
						<button type="submit" class="btn btn_small">결제 강제</button>
					</form>
				</td>
			</tr>
		</c:forEach>
		<c:if test="${empty pageData.list}"><tr><td colspan="6" class="empty">주문이 없습니다.</td></tr></c:if>
	</tbody>
</table>

<%@ include file="/common/paging.jsp" %>

<c:if test="${not empty detail}">
<h3>배송 이력 — <c:out value="${detail.orderNo}" /></h3>
<%--
  ★ is_forced 를 읽는 유일한 화면이다 (CRUD.md § 6-1).
    강제로 비트는 곳과 결과를 확인하는 곳이 같아야 시연이 성립한다.
    고객 화면(주문상세 타임라인)에는 이 값을 노출하지 않는다.
--%>
<table class="listtable">
	<thead class="listtable_head">
		<tr><th>시각</th><th>이전</th><th>이후</th><th>강제</th><th>변경자</th></tr>
	</thead>
	<tbody>
		<c:forEach var="h" items="${detail.delivery.history}">
			<tr class="${h.isForced == 'Y' ? 'on' : ''}">
				<td><fmt:formatDate value="${h.changedAt}" pattern="yyyy-MM-dd HH:mm:ss" /></td>
				<td><c:out value="${empty h.prevStatus ? '(최초)' : h.prevStatus}" /></td>
				<td><c:out value="${h.newStatus}" /></td>
				<td>${h.isForced}</td>
				<td><c:out value="${empty h.changedByName ? '시스템' : h.changedByName}" /></td>
			</tr>
		</c:forEach>
	</tbody>
</table>
</c:if>
<%@ include file="/common/footer.jsp" %>
