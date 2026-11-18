<%@ include file="../common/header.jsp" %>
<h2>조합 맞추기</h2>
<p class="field_help">
	필요한 영양제만 고르고 며칠치를 살지 정하세요. 고른 영양제마다 같은 일수로 장바구니에 한 줄씩 담깁니다.
	가격은 1포(하루 섭취분) 기준입니다.
</p>
<c:if test="${not empty loadedTrack}">
	<p class="custom_loaded" data-loaded>'<c:out value="${loadedTrack.name}" />' 구성을 불러왔습니다. 빼거나 더해서 나만의 조합으로 고쳐 보세요.</p>
</c:if>
<c:if test="${not empty loadedCombo and empty param.saved}">
	<p class="custom_loaded" data-loaded>내 조합 '<c:out value="${loadedCombo.name}" />' 을(를) 불러왔습니다. 가격 · 재고 · 경고는 지금 기준으로 다시 계산했습니다.</p>
</c:if>
<c:if test="${not empty unavailable}">
	<p class="custom_loaded">
		지금 담을 수 없어 뺀 영양제:
		<c:out value="${unavailable}" />
		(판매 중지 또는 재고 없음)
	</p>
</c:if>
<form id="customForm" class="custom" action="${ctx}/cart/addAll" method="post"
      data-fee="${deliveryFeeBase}" data-free-min="${freeShippingMin}" data-max-days="${maxDays}"
      data-ctx="${ctx}">
	<nav class="custom_tabs tab" data-tab>
		<ul class="tab_list">
			<c:forEach var="t" items="${tabs}" varStatus="st">
				<li><button type="button" class="tab_btn ${st.first ? 'on' : ''}" data-tab-open="customTab${t.categoryId}"><c:out value="${t.name}" /></button></li>
			</c:forEach>
			<li><button type="button" class="tab_btn" data-tab-open="customTabTrack">트랙 불러오기</button></li>
		</ul>
	</nav>
	<div class="custom_list">
		<c:if test="${empty tabs}">
			<p class="empty">지금 고를 수 있는 영양제가 없습니다.</p>
		</c:if>
		<c:forEach var="t" items="${tabs}" varStatus="st">
			<section id="customTab${t.categoryId}" class="tab_panel ${st.first ? 'on' : ''}" ${st.first ? '' : 'hidden'}>
				<h3><c:out value="${t.name}" /></h3>
				<table class="listtable custom_table">
					<thead class="listtable_head">
						<tr><th class="col_check">선택</th><th class="col_name">영양제</th><th class="col_price">1포 가격</th>
						    <th class="col_rating">별점</th><th class="col_state">재고</th></tr>
					</thead>
					<tbody>
						<c:forEach var="p" items="${tabProducts[t.categoryId]}">
								<tr>
									<td class="col_check">
										<input type="checkbox" name="productId" value="${p.productId}" id="pick${p.productId}"
										       data-name="<c:out value='${p.name}' />" data-price="${p.price}" data-stock="${p.stock}"
										       ${pickedIds.contains(p.productId) ? 'checked' : ''}>
									</td>
									<td class="col_name">
										<label for="pick${p.productId}"><c:out value="${p.name}" /></label>
										<a class="custom_more" href="${ctx}/product/view?productId=${p.productId}">상세</a>
									</td>
									<td class="col_price"><fmt:formatNumber value="${p.price}" pattern="#,##0" />원</td>
									<td class="col_rating"><fmt:formatNumber value="${p.avgRating}" pattern="0.0" /> (${p.reviewCount})</td>
									<td class="col_state">${p.stock}포</td>
								</tr>
						</c:forEach>
					</tbody>
				</table>
			</section>
		</c:forEach>
		<section id="customTabTrack" class="tab_panel" hidden>
			<h3>트랙 패키지에서 시작하기</h3>
			<p class="field_help">트랙 구성을 불러와 빼거나 더해 고칠 수 있습니다. 지금 고른 목록은 트랙 구성으로 바뀝니다.</p>
			<table class="listtable custom_table">
				<thead class="listtable_head">
					<tr><th class="col_name">트랙</th><th class="col_content">구성</th><th class="col_price">트랙 1포</th><th class="col_action"></th></tr>
				</thead>
				<tbody>
					<c:forEach var="tr" items="${tracks}">
						<tr>
							<td class="col_name"><a href="${ctx}/product/view?productId=${tr.productId}"><c:out value="${tr.name}" /></a></td>
							<td class="col_content">
								<c:out value="${tr.itemNames}" />
							</td>
							<td class="col_price"><fmt:formatNumber value="${tr.price}" pattern="#,##0" />원</td>
							<td class="col_action">
								<a class="btn btn_small" href="${ctx}/custom?track=${tr.productId}&amp;days=${days}"
								   data-confirm-message="지금 고른 목록을 이 트랙 구성으로 바꿀까요?">불러오기</a>
							</td>
						</tr>
					</c:forEach>
					<c:if test="${empty tracks}">
						<tr><td colspan="4" class="empty">판매 중인 트랙이 없습니다.</td></tr>
					</c:if>
				</tbody>
			</table>
		</section>
	</div>
	<aside class="custom_summary">
		<h3>내 조합 <span class="custom_count" id="customCount">${picked.size()}</span>종</h3>
		<ul class="custom_picked" id="customPicked">
			<c:forEach var="p" items="${picked}">
				<li data-id="${p.productId}">
					<span class="custom_picked_name"><c:out value="${p.name}" /></span>
					<span class="custom_picked_price">1포 <fmt:formatNumber value="${p.price}" pattern="#,##0" />원</span>
					<c:if test="${p.stock < days}"><span class="custom_picked_warn">재고 ${p.stock}포 — ${days}일분은 담을 수 없음</span></c:if>
					<button type="button" class="btn btn_small btn_sub" data-unpick="${p.productId}">빼기</button>
				</li>
			</c:forEach>
		</ul>
		<p class="empty" id="customEmpty" ${empty picked ? '' : 'hidden'}>왼쪽 목록에서 영양제를 고르세요.</p>
		<div class="custom_days">
			<span class="custom_days_label">섭취 일수</span>
			<c:forEach var="d" items="${quickDays}">
				<button type="button" class="btn btn_small ${d == days ? 'on' : ''}"
				        data-days-value="${d}" data-days-target="customDays">${d}일</button>
			</c:forEach>
			<label for="customDays">직접 입력</label>
			<input type="number" id="customDays" name="days" value="${days}" min="1" max="${maxDays}">일
		</div>
		<table class="formtable custom_sum">
			<tr><th>1포 합계</th><td><span id="sumPack"><fmt:formatNumber value="${packPrice}" pattern="#,##0" /></span>원</td></tr>
			<tr><th>섭취 일수</th><td><span id="sumDays">${days}</span>일 (<span id="sumPacks">${days}</span>포씩)</td></tr>
			<tr><th>상품 합계</th><td><strong id="sumTotal"><fmt:formatNumber value="${totalPrice}" pattern="#,##0" /></strong>원</td></tr>
			<tr>
				<th>배송비 (예상)</th>
				<td>
					<span id="sumFee"><fmt:formatNumber value="${deliveryFee}" pattern="#,##0" /></span>원
					<span class="field_help"><fmt:formatNumber value="${freeShippingMin}" pattern="#,##0" />원 이상 무료 · 장바구니의 다른 상품과 합산됩니다</span>
				</td>
			</tr>
		</table>
		<section class="warnbox" id="customWarn" ${empty warnings ? 'hidden' : ''}>
			<h3 class="warnbox_title">함께 드실 때 확인하세요</h3>
			<ul class="warnbox_list" id="customWarnList">
				<c:forEach var="w" items="${warnings}">
					<li class="warnbox_conflict">
						<strong class="warnbox_label">동시 섭취 주의</strong>
						<c:out value="${w.productAName}" /> × <c:out value="${w.productBName}" />
						<span class="warnbox_msg"><c:out value="${w.message}" /></span>
					</li>
				</c:forEach>
			</ul>
			<p class="warnbox_note">경고는 안내이며 담기를 막지 않습니다. 일반 정보이며 의학적 조언이 아닙니다.</p>
		</section>
		<p class="warnbox_ok" id="customWarnOk" ${empty warnings and not empty picked ? '' : 'hidden'}>함께 먹기를 권하지 않는 조합이 없습니다.</p>
		<c:choose>
			<c:when test="${sessionScope.loginMember.role == 'USER'}">
				<div class="custom_actions">
					<button type="submit" class="btn btn_primary" id="customAdd">장바구니에 담기</button>
				</div>
				<div class="custom_save">
					<label for="comboName">조합 이름</label>
					<input type="text" id="comboName" name="comboName" class="field" maxlength="50"
					       value="<c:out value='${loadedCombo.name}' />" placeholder="예: 출근 전 루틴">
					<input type="hidden" name="overwrite" id="comboOverwrite" value="">
					<button type="submit" class="btn" id="customSave" formaction="${ctx}/mypage/combo/save">조합 저장</button>
					<span class="field_help">같은 이름이 있으면 덮어쓸지 묻습니다.</span>
				</div>
			</c:when>
			<c:when test="${empty sessionScope.loginMember}">
				<c:url var="loginUrl" value="/member/login">
					<c:param name="returnUrl" value="${ctx}/custom?pick=${pickParam}&days=${days}" />
				</c:url>
				<div class="custom_actions">
					<a class="btn btn_primary" id="customLogin" href="${loginUrl}">로그인하고 담기</a>
					<span class="field_help">고른 조합은 로그인 후에도 그대로 남습니다.</span>
				</div>
			</c:when>
			<c:otherwise>
				<p class="field_help">담기 · 조합 저장은 일반 회원 계정으로만 가능합니다.</p>
			</c:otherwise>
		</c:choose>
		<p class="custom_share">
			이 조합 링크:
			<a id="customShare" href="${ctx}/custom?pick=${pickParam}&amp;days=${days}">${ctx}/custom?pick=${pickParam}&amp;days=${days}</a>
		</p>
	</aside>
</form>
<c:if test="${sessionScope.loginMember.role == 'USER'}">
	<section class="custom_mycombo">
		<h3>내 조합 불러오기</h3>
		<ul>
			<c:forEach var="cb" items="${myCombos}">
				<li>
					<a href="${ctx}/custom?combo=${cb.comboId}"
					   data-combo-name="<c:out value='${cb.name}' />"><c:out value="${cb.name}" /></a>
					<span class="field_help">${cb.itemCount}종 · ${cb.days}일분</span>
				</li>
			</c:forEach>
			<c:if test="${empty myCombos}"><li class="empty">저장한 조합이 없습니다.</li></c:if>
		</ul>
		<p><a href="${ctx}/mypage/combo">내 조합 관리</a></p>
	</section>
</c:if>
<ul id="customConflicts" hidden>
	<c:forEach var="cf" items="${conflicts}">
		<li data-a="${cf.productA}" data-b="${cf.productB}"
		    data-name-a="<c:out value='${cf.productAName}' />" data-name-b="<c:out value='${cf.productBName}' />"
		    data-message="<c:out value='${cf.message}' />"></li>
	</c:forEach>
</ul>
<script src="${ctx}/js/custom.js" defer></script>
<%@ include file="../common/footer.jsp" %>
