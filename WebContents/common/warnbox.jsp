<%-- 동시 섭취 경고 박스 — 장바구니 · 주문서가 include 지시자로 정적 포함한다.
     입력: warnings (List<IntakeWarningDTO>, IntakeService 계산 결과)
     ★ 경고는 안내만 한다. 구매를 막지 않는다 (ERD § 7.1 경고 위치 · 효력).
     조합 탭(custom/index.jsp)은 같은 클래스로 따로 그린다 — 고르는 즉시 js/custom.js 가 다시 채운다. --%>
<c:if test="${not empty warnings}">
<section class="warnbox">
	<h3 class="warnbox_title">함께 드실 때 확인하세요</h3>
	<ul class="warnbox_list">
		<c:forEach var="w" items="${warnings}">
			<li class="${w.conflict ? 'warnbox_conflict' : 'warnbox_dup'}">
				<strong class="warnbox_label">${w.conflict ? '동시 섭취 주의' : '중복 섭취'}</strong>
				<c:choose>
					<c:when test="${w.conflict}">
						<c:out value="${w.nameA}" /><c:if test="${not empty w.viaA}"> (<c:out value="${w.viaA}" /> 구성품)</c:if>
						×
						<c:out value="${w.nameB}" /><c:if test="${not empty w.viaB}"> (<c:out value="${w.viaB}" /> 구성품)</c:if>
					</c:when>
					<c:otherwise>
						<c:out value="${w.nameA}" /> —
						<c:out value="${empty w.viaA ? '직접 고른 낱개' : w.viaA}" /> ·
						<c:out value="${empty w.viaB ? '직접 고른 낱개' : w.viaB}" />
					</c:otherwise>
				</c:choose>
				<span class="warnbox_msg"><c:out value="${w.message}" /></span>
			</li>
		</c:forEach>
	</ul>
	<p class="warnbox_note">경고는 안내이며 구매를 막지 않습니다. 일반 정보이며 의학적 조언이 아닙니다. 복용 중인 약이 있다면 전문가와 상담하세요.</p>
</section>
</c:if>
