<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" isErrorPage="true" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<!DOCTYPE html>
<html lang="ko">
<head>
<meta charset="UTF-8">
<title>500 — 처리 중 오류</title>
</head>
<body>
<h1>500 — 처리 중 오류가 발생했습니다</h1>
<%--
  샌드박스라 원인을 화면에 그대로 띄운다. 배선을 고치는 데 이 정보가 필요하다.
  실제 제출물에서는 지우고 로그로만 남겨야 한다 — 트랙 문서 § 8 에 올려 둔다.
--%>
<pre><c:out value="${pageContext.exception}" /></pre>
<p><a href="${ctx}/">메인으로</a></p>
</body>
</html>
