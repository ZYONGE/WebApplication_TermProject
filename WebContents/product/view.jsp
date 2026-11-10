<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<c:set var="pageCss" value="product" />
<%@ include file="/common/header.jsp" %>

<%--
  상단 2단 — 왼쪽 이미지 · 오른쪽 정보와 구매 (prod_detail 격자).
  설명 · 트랙 구성 · 리뷰 · 문의는 그 아래 전체 폭.
--%>
<article class="prod_detail">
	<div class="prod_gallery">
		<c:choose>
			<c:when test="${not empty product.thumbnail}">
				<img src="${ctx}${product.thumbnail}" alt="<c:out value='${product.name}' />">
			</c:when>
			<c:otherwise><p>[대표 이미지 없음]</p></c:otherwise>
		</c:choose>
		<c:forEach var="img" items="${product.images}">
			<img src="${ctx}${img.imgPath}" alt="상품 이미지 ${img.sortOrder}">
		</c:forEach>
	</div>

	<div class="prod_info">
		<h2><c:out value="${product.name}" /></h2>
		<p class="prod_price">
			1포 <strong><fmt:formatNumber value="${product.price}" pattern="#,##0" /></strong>원
			<span class="field_help">1포 = 하루 섭취분</span>
		</p>

		<table class="formtable prod_spec">
			<tr><th>구분</th><td>${product.track ? '트랙 패키지 — 여러 영양제를 1포에 묶은 상품' : '낱개 영양제 — 1포에 한 가지 영양소 1회분'}</td></tr>
			<tr><th>재고</th><td>${product.stock}포 (${product.stock}일분)</td></tr>
			<tr>
				<th>별점</th>
				<td><fmt:formatNumber value="${product.avgRating}" pattern="0.0" /> / 5 (리뷰 ${product.reviewCount}건)</td>
			</tr>
		</table>

		<%--
		  구매 컨트롤은 USER 와 비로그인(누르면 로그인으로)에게만 보인다.
		  ADMIN · DEV 에게 숨기는 것은 화면 처리일 뿐이고, 실제 방어는 UserFilter 다 (ERD § 5.3).
		--%>
		<c:choose>
		<c:when test="${not empty sessionScope.loginMember and sessionScope.loginMember.role != 'USER'}">
			<p class="field_help">구매는 일반 회원 계정으로만 가능합니다.</p>
		</c:when>
		<c:otherwise>
		<form class="prod_buy" action="${ctx}/cart/add" method="post" data-validate>
			<input type="hidden" name="productId" value="${product.productId}">
			<input type="hidden" name="back" value="/product/view?productId=${product.productId}">
			<%-- 수량 = 섭취 일수(= 포 수). 빠른 선택 + 직접 입력. 상한은 MAX_INTAKE_DAYS 와 재고 중 작은 값 --%>
			<c:set var="dayLimit" value="${product.stock < maxDays ? product.stock : maxDays}" />
			<c:set var="dayInit" value="${dayLimit < defaultDays ? dayLimit : defaultDays}" />
			<div class="prod_days">
				<span class="prod_days_label">섭취 일수</span>
				<c:forEach var="d" items="${quickDays}">
					<c:if test="${d <= dayLimit}">
						<button type="button" class="btn btn_small ${d == dayInit ? 'on' : ''}"
						        data-days-value="${d}" data-days-target="quantity">${d}일</button>
					</c:if>
				</c:forEach>
				<label for="quantity">직접 입력</label>
				<input type="number" id="quantity" name="quantity" value="${dayInit}" min="1" max="${dayLimit}"
				       data-rule="intakeDays" data-max="${dayLimit}">일
			</div>
			<p class="prod_days_total" data-days-total="quantity" data-price="${product.price}">
				1포 <fmt:formatNumber value="${product.price}" pattern="#,##0" />원 × <span data-days-count>${dayInit}</span>일
				= <strong data-days-sum><fmt:formatNumber value="${product.price * dayInit}" pattern="#,##0" /></strong>원
			</p>
			<%--
			  담기 · 찜을 한 줄 5:5 로. 폼 안에 폼을 둘 수 없어 찜 버튼은 form 속성으로 아래 찜 폼(prodWishForm)을 보낸다.
			--%>
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
		</c:otherwise>
		</c:choose>

		<%-- 함께 드시지 말 것 — 담기 상자 아래에 둔다(담기 버튼이 첫 화면에 보이게). 트랙이면 구성품 전체 기준. 안내만 하고 구매를 막지 않는다 --%>
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

<%-- 트랙 구성 — 1포에 구성 낱개가 1회분씩 들어 있다 (ERD § 7.1) --%>
<c:if test="${product.track}">
	<section class="prod_track">
		<h3>트랙 구성 (1포 기준)</h3>
		<table class="listtable">
			<thead class="listtable_head">
				<tr><th class="col_no">순서</th><th class="col_name">구성 영양제</th><th class="col_price">낱개 1포 가격</th></tr>
			</thead>
			<tbody>
				<c:set var="singleSum" value="0" />
				<c:forEach var="item" items="${product.trackItems}" varStatus="st">
					<c:set var="singleSum" value="${singleSum + item.price}" />
					<tr>
						<td class="col_no">${st.count}</td>
						<td class="col_name"><c:out value="${item.name}" /></td>
						<td class="col_price"><fmt:formatNumber value="${item.price}" pattern="#,##0" />원</td>
					</tr>
				</c:forEach>
				<c:if test="${empty product.trackItems}">
					<tr><td colspan="3" class="empty">등록된 구성이 없습니다.</td></tr>
				</c:if>
			</tbody>
		</table>
		<c:if test="${not empty product.trackItems}">
			<p class="field_help">
				낱개로 따로 담으면 1포 <fmt:formatNumber value="${singleSum}" pattern="#,##0" />원,
				트랙은 1포 <fmt:formatNumber value="${product.price}" pattern="#,##0" />원입니다.
				<a href="${ctx}/custom?track=${product.productId}">조합 맞추기에서 불러와 고치기</a>
			</p>
		</c:if>
	</section>
</c:if>



<section class="prod_review">
	<h3>리뷰 (${product.reviewCount}건)</h3>
	<%--
	  모든 리뷰는 구매 확인된 글이다 — review.item_id 가 NOT NULL 이라
	  구매하지 않은 사람이 쓸 수 없다 (CRUD.md § 6-3).
	  작성자명은 서버에서 마스킹해서 내려온다.
	--%>
	<table class="listtable">
		<thead class="listtable_head">
			<tr><th class="col_rating">별점</th><th class="col_content">내용</th>
			    <th class="col_writer">작성자</th><th class="col_date">작성일</th></tr>
		</thead>
		<tbody>
			<c:forEach var="r" items="${reviews}">
				<tr>
					<td class="col_rating">${r.rating} / 5</td>
					<td class="col_content">
						<c:out value="${r.content}" />
						<span class="prodcard_badge">구매자 작성</span>
					</td>
					<td class="col_writer"><c:out value="${r.memberName}" /></td>
					<td class="col_date"><fmt:formatDate value="${r.createdAt}" pattern="yyyy-MM-dd" /></td>
				</tr>
			</c:forEach>
			<c:if test="${empty reviews}">
				<tr><td colspan="4" class="empty">등록된 리뷰가 없습니다.</td></tr>
			</c:if>
		</tbody>
	</table>
	<p class="field_help">리뷰는 배송완료된 주문 건에서만 쓸 수 있습니다. (마이페이지 &gt; 내 리뷰)</p>
</section>

<section class="prod_qna">
	<h3>상품 문의</h3>
	<c:if test="${sessionScope.loginMember.role == 'USER'}">
		<button type="button" class="btn" data-popup-open="qnaPopup">문의하기</button>
	</c:if>

	<table class="listtable">
		<thead class="listtable_head">
			<tr><th class="col_content">문의</th><th class="col_writer">작성자</th>
			    <th class="col_date">작성일</th><th class="col_state">답변</th></tr>
		</thead>
		<tbody>
			<c:forEach var="q" items="${qnaList}">
				<tr>
					<td class="col_content">
						<%-- 비밀글은 서버가 본문을 비워서 내려보낸다. 화면은 가리는 일을 하지 않는다. --%>
						<c:choose>
							<c:when test="${q.secretPost and empty q.question}">비밀글입니다.</c:when>
							<c:otherwise>
								<c:out value="${q.question}" />
								<c:if test="${q.answered}">
									<div class="qna_answer">답변: <c:out value="${q.answer}" /></div>
								</c:if>
							</c:otherwise>
						</c:choose>
					</td>
					<td class="col_writer"><c:out value="${q.memberName}" /></td>
					<td class="col_date"><fmt:formatDate value="${q.createdAt}" pattern="yyyy-MM-dd" /></td>
					<td class="col_state">${q.answered ? '답변완료' : '미답변'}</td>
				</tr>
			</c:forEach>
			<c:if test="${empty qnaList}">
				<tr><td colspan="4" class="empty">등록된 문의가 없습니다.</td></tr>
			</c:if>
		</tbody>
	</table>
</section>

<%--
  팝업 — HTML 표준 <dialog> 를 쓴다.

  css/DECISIONS.md O-02(별창 window.open vs 인페이지 모달)가 아직 미결이라
  "CSS 없이도 모달로 동작하는" 쪽을 골랐다. 확정되면 그에 맞춰 바꾼다.
  클래스 이름은 부록 B 의 popup.css 목록을 그대로 쓴다.
--%>
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
<%@ include file="/common/footer.jsp" %>
