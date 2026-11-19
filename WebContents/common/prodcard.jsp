<%-- 상품 카드 한 장 — p(상품)와 cardType(track: 메인 트랙 / new: 메인 신상품 / list: 상품 목록)을 받는다 --%>
<li class="prodcard">
  <a href="${ctx}/product/view?productId=${p.productId}">
    <span class="prodcard_thumb">
      <c:if test="${not empty p.thumbnail}"><img src="${ctx}${p.thumbnail}" alt=""></c:if>
      <c:if test="${empty p.thumbnail}">[이미지 없음]</c:if>
    </span>
    <span class="prodcard_name"><c:out value="${p.name}" /></span>
    <span class="prodcard_price">1포 <fmt:formatNumber value="${p.price}" pattern="#,##0" />원</span>
  </a>
  <c:if test="${p.track}"><span class="prodcard_badge">구성 <c:out value="${p.itemNames}" /></span></c:if>
  <c:if test="${cardType == 'track'}"><a class="btn btn_small" href="${ctx}/custom?track=${p.productId}">불러와 고치기</a></c:if>
  <c:if test="${cardType != 'track'}">
    <span class="prodcard_badge">${p.typeLabel} · <c:if test="${cardType == 'list'}"><c:out value="${p.categoryName}" /> · </c:if>별점 <fmt:formatNumber value="${p.avgRating}" pattern="0.0" /> (${p.reviewCount})</span>
  </c:if>
</li>
