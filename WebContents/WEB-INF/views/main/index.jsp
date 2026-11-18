<%@ include file="../common/header.jsp" %>
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
    <c:set var="cardType" value="track" />
    <c:forEach var="p" items="${tracks}"><%@ include file="../common/prodcard.jsp" %></c:forEach>
    <c:if test="${empty tracks}"><li class="empty">판매 중인 트랙 패키지가 없습니다.</li></c:if>
  </ul>
</section>
<section class="main_new">
  <h2>새로 들어온 영양제</h2>
  <ul class="prodlist">
    <c:set var="cardType" value="new" />
    <c:forEach var="p" items="${newProducts}"><%@ include file="../common/prodcard.jsp" %></c:forEach>
    <c:if test="${empty newProducts}"><li class="empty">판매 중인 낱개 영양제가 없습니다.</li></c:if>
  </ul>
</section>
<%@ include file="../common/footer.jsp" %>
