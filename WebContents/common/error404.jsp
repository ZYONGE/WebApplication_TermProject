<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" isErrorPage="true" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<!DOCTYPE html>
<html lang="ko">
<head>
<meta charset="UTF-8">
<title>404 — 페이지를 찾을 수 없습니다</title>
</head>
<body>
<h1>404 — 페이지를 찾을 수 없습니다</h1>
<p><a href="${ctx}/">메인으로</a></p>
</body>
</html>
