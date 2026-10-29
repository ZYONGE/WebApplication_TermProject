<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ include file="/common/header.jsp" %>

<ul class="admin_nav">
	<li><a href="${ctx}/dev/">dev 허브</a></li>
	<li><a href="${ctx}/dev/state">상태 컨트롤러</a></li>
	<li><a href="${ctx}/dev/data">데이터 브라우저</a></li>
	<li><a href="${ctx}/dev/erd">설계 보기</a></li>
	<li><a href="${ctx}/dev/seed">데이터 관리</a></li>
	<li><a href="${ctx}/dev/loginAs">계정 전환</a></li>
	<li><a href="${ctx}/dev/log">감사 로그</a></li>
	<li><a href="${ctx}/admin/dashboard">관리자로</a></li>
</ul>

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
		<c:forEach var="t" items="${tables}">
			<%-- member 와 뷰(v_*)는 비울 수 없다. 서버에서도 같은 판정을 한다. --%>
			<c:if test="${t != 'member' and not fn:startsWith(t, 'v_')}">
				<option value="${t}">${t}</option>
			</c:if>
		</c:forEach>
	</select>
	<button type="submit" class="btn btn_danger"
	        data-confirm-message="이 테이블을 비우시겠습니까? 되돌릴 수 없습니다.">비우기</button>
</form>

<h3>전체 초기화</h3>
<%--
  ★ member 를 통째로 비우지 않는다. DEV 계정이 사라지면 /dev 재진입 경로가 없어지고,
    ADMIN 은 DEV 권한을 줄 수 없어서 복구 수단이 사라진다 (ERD § 6.7).
    그래서 전체 초기화는 role = 'USER' 인 회원만 지운다.
--%>
<p class="field_help">
	업무 테이블 14개를 TRUNCATE 하고, 회원은 <strong>role = 'USER'</strong> 인 행만 삭제합니다.
	ADMIN · DEV 계정은 남습니다. AUTO_INCREMENT 는 1로 되돌아갑니다.
</p>
<form action="${ctx}/dev/seed" method="post">
	<input type="hidden" name="mode" value="truncateAll">
	<button type="submit" class="btn btn_danger"
	        data-confirm-message="전체 초기화합니다. 되돌릴 수 없습니다. 진행할까요?">전체 초기화</button>
</form>
<%@ include file="/common/footer.jsp" %>
