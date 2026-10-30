<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<c:set var="pageCss" value="main" />
<%@ include file="/common/header.jsp" %>

<section class="main_notice">
	<h2>공지사항</h2>
	<ul>
		<c:forEach var="notice" items="${notices}">
			<li>
				<a href="${ctx}/notice/view?noticeId=${notice.noticeId}"><c:out value="${notice.title}" /></a>
				<span><fmt:formatDate value="${notice.createdAt}" pattern="yyyy-MM-dd" /></span>
			</li>
		</c:forEach>
		<c:if test="${empty notices}"><li class="empty">등록된 공지가 없습니다.</li></c:if>
	</ul>
	<p><a href="${ctx}/notice/list">공지 전체보기</a></p>
</section>

<section class="main_new">
	<h2>신상품</h2>
	<%--
	  노출 조건(status='판매중' AND stock>0)을 통과한 상품만 내려온다.
	  조건은 ProductDAO 가 걸고 화면은 결과만 본다 (ERD § 2.4).
	--%>
	<ul class="prodlist">
		<c:forEach var="p" items="${newProducts}">
			<li class="prodcard">
				<a href="${ctx}/product/view?productId=${p.productId}">
					<span class="prodcard_thumb">
						<c:choose>
							<c:when test="${not empty p.thumbnail}">
								<img src="${ctx}${p.thumbnail}" alt="<c:out value='${p.name}' />">
							</c:when>
							<c:otherwise>[이미지 없음]</c:otherwise>
						</c:choose>
					</span>
					<span class="prodcard_name"><c:out value="${p.name}" /></span>
					<span class="prodcard_price"><fmt:formatNumber value="${p.price}" pattern="#,##0" />원</span>
				</a>
				<span class="prodcard_badge">
					별점 <fmt:formatNumber value="${p.avgRating}" pattern="0.0" /> (${p.reviewCount})
				</span>
			</li>
		</c:forEach>
		<c:if test="${empty newProducts}"><li class="empty">판매 중인 상품이 없습니다.</li></c:if>
	</ul>
</section>
<%@ include file="/common/footer.jsp" %>
