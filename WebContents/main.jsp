<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<c:set var="pageCss" value="main" />
<%@ include file="/common/header.jsp" %>

<%-- 조합 탭 진입 — 필요한 영양소만 골라 1포 단위로 담는다 (CONCEPT § 2) --%>
<section class="main_custom">
	<h2>나에게 필요한 영양소만, 하루 1포로</h2>
	<p>영양제를 직접 골라 며칠치를 살지 정하세요. 함께 먹기를 권하지 않는 조합은 고르는 즉시 알려 드립니다.</p>
	<p>
		<a class="btn btn_primary" href="${ctx}/custom">조합 맞추기</a>
		<a class="btn btn_sub" href="${ctx}/product/list?type=TRACK">트랙 패키지 보기</a>
	</p>
</section>

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

<section class="main_track">
	<h2>트랙 패키지</h2>
	<p class="field_help">여러 영양제를 1포에 미리 묶었습니다. 조합 맞추기에서 불러와 고쳐 쓸 수도 있습니다.</p>
	<ul class="prodlist">
		<c:forEach var="p" items="${tracks}">
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
					<span class="prodcard_price">1포 <fmt:formatNumber value="${p.price}" pattern="#,##0" />원</span>
				</a>
				<span class="prodcard_badge">
					구성 <c:forEach var="item" items="${p.trackItems}" varStatus="st"><c:if test="${not st.first}"> + </c:if><c:out value="${item.name}" /></c:forEach>
				</span>
				<a class="btn btn_small" href="${ctx}/custom?track=${p.productId}">불러와 고치기</a>
			</li>
		</c:forEach>
		<c:if test="${empty tracks}"><li class="empty">판매 중인 트랙 패키지가 없습니다.</li></c:if>
	</ul>
</section>

<section class="main_new">
	<h2>새로 들어온 영양제</h2>
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
					<span class="prodcard_price">1포 <fmt:formatNumber value="${p.price}" pattern="#,##0" />원</span>
				</a>
				<span class="prodcard_badge">
					${p.typeLabel} ·
					별점 <fmt:formatNumber value="${p.avgRating}" pattern="0.0" /> (${p.reviewCount})
				</span>
			</li>
		</c:forEach>
		<c:if test="${empty newProducts}"><li class="empty">판매 중인 낱개 영양제가 없습니다.</li></c:if>
	</ul>
</section>
<%@ include file="/common/footer.jsp" %>
