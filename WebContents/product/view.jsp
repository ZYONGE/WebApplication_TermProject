<%@ include file="../common/header.jsp" %>
<article class="prod_detail">
  <div class="prod_gallery">
    <c:if test="${not empty product.thumbnail}"><img src="${ctx}${product.thumbnail}" alt="<c:out value='${product.name}' />"></c:if>
    <c:if test="${empty product.thumbnail}"><p>[대표 이미지 없음]</p></c:if>
    <c:forEach var="img" items="${product.images}">
      <img src="${ctx}${img.imgPath}" alt="상품 이미지 ${img.sortOrder}">
    </c:forEach>
  </div>
  <div class="prod_info">
    <h2><c:out value="${product.name}" /></h2>
    <p class="prod_price">1포 <strong><fmt:formatNumber value="${product.price}" pattern="#,##0" /></strong>원</p>
    <table class="formtable prod_spec">
      <tr><th>구분</th><td>${product.track ? '트랙 패키지 — 여러 영양제를 1포에 묶은 상품' : '낱개 영양제 — 1포에 한 가지 영양소 1회분'}</td></tr>
      <tr><th>재고</th><td>${product.stock}포</td></tr>
      <tr><th>별점</th><td><fmt:formatNumber value="${product.avgRating}" pattern="0.0" /> / 5 (리뷰 ${product.reviewCount}건)</td></tr>
    </table>
    <c:if test="${not empty me and me.role != 'USER'}">
      <p class="field_help">구매는 일반 회원 계정으로만 가능합니다.</p>
    </c:if>
    <c:if test="${empty me or me.role == 'USER'}">
      <%-- 섭취 일수 고르기 — 빠른 선택 버튼을 누르면 custom.js 가 입력칸과 합계를 바꾼다 --%>
      <form class="prod_buy" action="${ctx}/cart/add" method="post" data-validate>
        <input type="hidden" name="productId" value="${product.productId}">
        <input type="hidden" name="back" value="/product/view?productId=${product.productId}">
        <div class="prod_days">
          <span class="prod_days_label">섭취 일수</span>
          <c:forEach var="d" items="${quickDays}">
            <button type="button" class="btn btn_small ${d == dayInit ? 'on' : ''}" data-days-value="${d}" data-days-target="quantity">${d}일</button>
          </c:forEach>
          <label for="quantity">직접 입력</label>
          <input type="number" id="quantity" name="quantity" value="${dayInit}" min="1" max="${dayLimit}" data-rule="intakeDays" data-max="${dayLimit}">일
        </div>
        <p class="prod_days_total" data-days-total="quantity" data-price="${product.price}">
          1포 <fmt:formatNumber value="${product.price}" pattern="#,##0" />원 × <span data-days-count>${dayInit}</span>일
          = <strong data-days-sum><fmt:formatNumber value="${product.price * dayInit}" pattern="#,##0" /></strong>원
        </p>
        <div class="prod_actions">
          <button type="submit" class="btn btn_primary">장바구니 담기</button>
          <button type="submit" class="btn btn_sub" form="prodWishForm">${wished ? '찜 해제' : '찜하기'}</button>
        </div>
        <span class="field_help">같은 상품을 다시 담으면 일수를 새 값으로 바꿉니다.</span>
      </form>
      <form class="prod_wish" id="prodWishForm" action="${ctx}/wish/toggle" method="post">
        <input type="hidden" name="productId" value="${product.productId}">
        <input type="hidden" name="back" value="/product/view?productId=${product.productId}">
      </form>
    </c:if>
    <c:if test="${not empty conflicts}">
      <section class="warnbox">
        <h3 class="warnbox_title">${product.track ? '이 트랙과 함께 드시지 마세요' : '이 영양제와 함께 드시지 마세요'}</h3>
        <ul class="warnbox_list">
          <c:forEach var="cf" items="${conflicts}">
            <li class="warnbox_conflict">
              <strong class="warnbox_label">동시 섭취 주의</strong>
              <c:out value="${cf.productAName}" /> × <c:out value="${cf.productBName}" />
              <span class="warnbox_msg"><c:out value="${cf.message}" /></span>
            </li>
          </c:forEach>
        </ul>
        <p class="warnbox_note">일반 정보이며 의학적 조언이 아닙니다. 복용 중인 약이 있다면 전문가와 상담하세요.</p>
      </section>
    </c:if>
  </div>
</article>
<section class="prod_desc">
  <h3>상품 설명</h3>
  <p><c:out value="${product.description}" /></p>
</section>
<c:if test="${product.track}">
  <section class="prod_track">
    <h3>트랙 구성 (1포 기준)</h3>
    <table class="listtable">
      <thead class="listtable_head">
        <tr><th class="col_no">순서</th><th class="col_name">구성 영양제</th><th class="col_price">낱개 1포 가격</th></tr>
      </thead>
      <tbody>
        <c:forEach var="item" items="${product.trackItems}" varStatus="st">
          <tr>
            <td class="col_no">${st.count}</td>
            <td class="col_name"><c:out value="${item.name}" /></td>
            <td class="col_price"><fmt:formatNumber value="${item.price}" pattern="#,##0" />원</td>
          </tr>
        </c:forEach>
        <c:if test="${empty product.trackItems}"><tr><td colspan="3" class="empty">등록된 구성이 없습니다.</td></tr></c:if>
      </tbody>
    </table>
    <c:if test="${not empty product.trackItems}">
      <p class="field_help">
        낱개로 따로 담으면 1포 <fmt:formatNumber value="${product.itemPriceSum}" pattern="#,##0" />원,
        트랙은 1포 <fmt:formatNumber value="${product.price}" pattern="#,##0" />원입니다.
        <a href="${ctx}/custom?track=${product.productId}">조합 맞추기에서 불러와 고치기</a>
      </p>
    </c:if>
  </section>
</c:if>
<section class="prod_review">
  <h3>리뷰 (${product.reviewCount}건)</h3>
  <table class="listtable">
    <thead class="listtable_head">
      <tr><th class="col_rating">별점</th><th class="col_content">내용</th><th class="col_writer">작성자</th><th class="col_date">작성일</th></tr>
    </thead>
    <tbody>
      <c:forEach var="r" items="${reviews}">
        <tr>
          <td class="col_rating">${r.rating} / 5</td>
          <td class="col_content"><c:out value="${r.content}" /> <span class="prodcard_badge">구매자 작성</span></td>
          <td class="col_writer"><c:out value="${r.memberName}" /></td>
          <td class="col_date"><fmt:formatDate value="${r.createdAt}" pattern="yyyy-MM-dd" /></td>
        </tr>
      </c:forEach>
      <c:if test="${empty reviews}"><tr><td colspan="4" class="empty">등록된 리뷰가 없습니다.</td></tr></c:if>
    </tbody>
  </table>
  <p class="field_help">리뷰는 배송완료된 주문 건에서만 쓸 수 있습니다. (마이페이지 &gt; 내 리뷰)</p>
</section>
<section class="prod_qna">
  <h3>상품 문의</h3>
  <c:if test="${me.role == 'USER'}"><button type="button" class="btn" data-popup-open="qnaPopup">문의하기</button></c:if>
  <c:if test="${empty me}">
    <c:url var="qnaLogin" value="/member/login"><c:param name="returnUrl" value="${ctx}/product/view?productId=${product.productId}" /></c:url>
    <a class="btn" href="${qnaLogin}">로그인하고 문의하기</a>
  </c:if>
  <table class="listtable">
    <thead class="listtable_head">
      <tr><th class="col_content">문의</th><th class="col_writer">작성자</th><th class="col_date">작성일</th><th class="col_state">답변</th></tr>
    </thead>
    <tbody>
      <c:forEach var="q" items="${qnaList}">
        <tr>
          <td class="col_content">
            <c:if test="${q.secretPost and empty q.question}">비밀글입니다.</c:if>
            <c:if test="${not empty q.question}">
              <c:out value="${q.question}" />
              <c:if test="${q.answered}"><div class="qna_answer">답변: <c:out value="${q.answer}" /></div></c:if>
            </c:if>
          </td>
          <td class="col_writer"><c:out value="${q.memberName}" /></td>
          <td class="col_date"><fmt:formatDate value="${q.createdAt}" pattern="yyyy-MM-dd" /></td>
          <td class="col_state">${q.answered ? '답변완료' : '미답변'}</td>
        </tr>
      </c:forEach>
      <c:if test="${empty qnaList}"><tr><td colspan="4" class="empty">등록된 문의가 없습니다.</td></tr></c:if>
    </tbody>
  </table>
</section>
<dialog id="qnaPopup" class="popup">
  <form class="popup_form" action="${ctx}/mypage/qna" method="post" data-validate>
    <input type="hidden" name="mode" value="write">
    <input type="hidden" name="productId" value="${product.productId}">
    <input type="hidden" name="back" value="/product/view?productId=${product.productId}">
    <h3 class="popup_head">상품 문의 작성</h3>
    <div class="popup_body">
      <p><c:out value="${product.name}" /></p>
      <label for="question">문의 내용</label>
      <textarea id="question" name="question" rows="5" class="field" data-rule="required"></textarea>
      <label><input type="checkbox" name="isSecret" value="Y"> 비밀글로 작성</label>
    </div>
    <div class="popup_foot">
      <button type="submit" class="btn btn_primary">등록</button>
      <button type="button" class="btn btn_sub popup_close" data-popup-close="qnaPopup">닫기</button>
    </div>
  </form>
</dialog>
<script src="${ctx}/js/custom.js" defer></script>
<%@ include file="../common/footer.jsp" %>
