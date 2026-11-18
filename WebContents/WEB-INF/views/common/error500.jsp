<%@ page isErrorPage="true" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html>
<html lang="ko">
<head>
<meta charset="UTF-8">
<title>500 — 처리 중 오류</title>
</head>
<body>
<h1>500 — 처리 중 오류가 발생했습니다</h1>
<pre><c:out value="${pageContext.exception}" /></pre>
<p><a href="${pageContext.request.contextPath}/">메인으로</a></p>
</body>
</html>
