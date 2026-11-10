<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
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

<h2>내 조합</h2>

<%--
  41번 내 조합 (CRUD.md · ERD § 7.1)
  가격은 저장하지 않았다. 아래 금액은 지금 1포 가격으로 다시 계산한 값이다.
  판매 중지 · 재고 없는 낱개는 표시만 하고, 불러와 담을 때 빠진다.
--%>
<p class="field_help">조합 맞추기에서 저장한 조합입니다. 금액은 현재 가격 기준이며, 불러오면 경고를 다시 계산합니다.</p>

<table class="listtable">
	<thead class="listtable_head">
		<tr><th class="col_name">이름</th><th class="col_content">구성</th><th class="col_qty">섭취 일수</th>
		    <th class="col_total">현재 합계</th><th class="col_date">저장일</th><th class="col_action"></th></tr>
	</thead>
	<tbody>
		<c:forEach var="cb" items="${combos}">
			<tr>
				<td class="col_name"><c:out value="${cb.name}" /></td>
				<td class="col_content">
					<c:forEach var="item" items="${cb.items}" varStatus="st">
						<c:if test="${not st.first}"> + </c:if>
						<c:choose>
							<c:when test="${item.available}"><c:out value="${item.productName}" /></c:when>
							<c:otherwise><span class="my_combo_off"><c:out value="${item.productName}" /> (재고 없음)</span></c:otherwise>
						</c:choose>
					</c:forEach>
				</td>
				<td class="col_qty">${cb.days}일분</td>
				<td class="col_total">
					1포 <fmt:formatNumber value="${cb.packPrice}" pattern="#,##0" />원 ×
					${cb.days}일 = <fmt:formatNumber value="${cb.totalPrice}" pattern="#,##0" />원
				</td>
				<td class="col_date"><fmt:formatDate value="${cb.updatedAt}" pattern="yyyy-MM-dd" /></td>
				<td class="col_action">
					<a class="btn btn_small btn_primary" href="${ctx}/custom?combo=${cb.comboId}">불러오기</a>
					<form action="${ctx}/mypage/combo/delete" method="post">
						<input type="hidden" name="comboId" value="${cb.comboId}">
						<button type="submit" class="btn btn_danger btn_small"
						        data-confirm-message="이 조합을 삭제하시겠습니까?">삭제</button>
					</form>
				</td>
			</tr>
		</c:forEach>
		<c:if test="${empty combos}">
			<tr><td colspan="6" class="empty">저장한 조합이 없습니다. <a href="${ctx}/custom">조합 맞추기</a>에서 만들어 보세요.</td></tr>
		</c:if>
	</tbody>
</table>
<%@ include file="/common/footer.jsp" %>
