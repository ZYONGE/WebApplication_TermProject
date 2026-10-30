<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<c:set var="pageCss" value="main" />
<%@ include file="/common/header.jsp" %>

<section class="main_maintenance">
	<h2>서비스 점검 중입니다</h2>
	<p>이용에 불편을 드려 죄송합니다. 점검이 끝나면 정상 이용하실 수 있습니다.</p>
	<p><a href="${ctx}/member/login">관리자 로그인</a></p>
</section>
<%@ include file="/common/footer.jsp" %>
