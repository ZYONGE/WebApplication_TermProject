<%@ include file="../common/header.jsp" %>
<%@ include file="../common/devnav.jsp" %>
<h2>dev · 데이터 관리</h2>
<h3>더미 상품 생성</h3>
<form class="formtable_form" action="${ctx}/dev/seed" method="post">
	<input type="hidden" name="mode" value="generate">
	<label for="count">생성 개수</label>
	<input type="number" id="count" name="count" value="10" min="1" max="100">
	<button type="submit" class="btn btn_primary">생성</button>
	<span class="field_help">목록 · 페이징을 채워 보기 위한 상품을 만듭니다.</span>
</form>
<h3>테이블 비우기</h3>
<form class="formtable_form" action="${ctx}/dev/seed" method="post">
	<input type="hidden" name="mode" value="truncateTable">
	<select name="table">
		<c:forEach var="t" items="${truncatable}"><option value="${t}">${t}</option></c:forEach>
	</select>
	<button type="submit" class="btn btn_danger"
	        data-confirm-message="이 테이블을 비우시겠습니까? 되돌릴 수 없습니다.">비우기</button>
</form>
<h3>전체 초기화</h3>
<p class="field_help">
	업무 테이블 ${truncateCount}개를 TRUNCATE 하고, 회원은 <strong>role = 'USER'</strong> 인 행만 삭제합니다.
	ADMIN · DEV 계정은 남습니다. AUTO_INCREMENT 는 1로 되돌아갑니다.
</p>
<form action="${ctx}/dev/seed" method="post">
	<input type="hidden" name="mode" value="truncateAll">
	<button type="submit" class="btn btn_danger"
	        data-confirm-message="전체 초기화합니다. 되돌릴 수 없습니다. 진행할까요?">전체 초기화</button>
</form>
<%@ include file="../common/footer.jsp" %>
