<%-- 공통 헤더 — 각 화면이 include 지시자로 정적 포함한다.
     page / taglib 지시자는 포함하는 쪽에 한 번만 둔다 (중복 선언은 번역 오류). --%>
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<!DOCTYPE html>
<html lang="ko">
<head>
<meta charset="UTF-8">
<title><c:out value="${empty pageTitle ? 'SHOP' : pageTitle}" /></title>
<!--
  CSS 로드 순서는 CLAUDE.md § 3 Stage 1 이 정한 것이다.
    reset -> variables -> layout -> components/* -> pages/*
  이 파일들은 현재 비어 있다. UI/UX 세션이 값을 채우면 JSP 수정 없이 그대로 적용된다.
  이 트랙은 CSS 를 쓰지 않는다 — 마크업의 class 이름만 계약대로 붙여 둔다
  (WebContents/css/DECISIONS.md D-08 · D-09).
-->
<link rel="stylesheet" href="${ctx}/css/base/reset.css">
<link rel="stylesheet" href="${ctx}/css/base/variables.css">
<link rel="stylesheet" href="${ctx}/css/base/layout.css">
<link rel="stylesheet" href="${ctx}/css/components/button.css">
<link rel="stylesheet" href="${ctx}/css/components/form.css">
<link rel="stylesheet" href="${ctx}/css/components/card.css">
<link rel="stylesheet" href="${ctx}/css/components/table.css">
<link rel="stylesheet" href="${ctx}/css/components/pagination.css">
<link rel="stylesheet" href="${ctx}/css/components/popup.css">
<c:if test="${not empty pageCss}">
<link rel="stylesheet" href="${ctx}/css/pages/${pageCss}.css">
</c:if>
<script src="${ctx}/js/ui.js" defer></script>
<script src="${ctx}/js/validate.js" defer></script>
</head>
<body>
<div class="wrap">

<header class="page_header">
	<h1><a href="${ctx}/">SHOP</a></h1>

	<ul class="page_header_util">
		<c:choose>
			<c:when test="${empty sessionScope.loginMember}">
				<li><a href="${ctx}/member/login">로그인</a></li>
				<li><a href="${ctx}/member/join">회원가입</a></li>
			</c:when>
			<c:otherwise>
				<li><c:out value="${sessionScope.loginMember.name}" />님 (<c:out value="${sessionScope.loginMember.role}" />)</li>
				<%-- 역할별 메뉴 — 3개 입장은 서로의 영역을 보지 않는다 (ERD § 5.3) --%>
				<c:choose>
					<c:when test="${sessionScope.loginMember.role == 'USER'}">
						<li><a href="${ctx}/mypage/">마이페이지</a></li>
						<li>
							<a href="${ctx}/cart/list">장바구니<c:if test="${not empty cartCount and cartCount > 0}"> (${cartCount})</c:if></a>
						</li>
					</c:when>
					<c:when test="${sessionScope.loginMember.role == 'ADMIN'}">
						<li><a href="${ctx}/admin/dashboard">관리자</a></li>
					</c:when>
					<c:when test="${sessionScope.loginMember.role == 'DEV'}">
						<li><a href="${ctx}/dev/">dev</a></li>
					</c:when>
				</c:choose>
				<li><a href="${ctx}/member/logout">로그아웃</a></li>
			</c:otherwise>
		</c:choose>
	</ul>

	<%-- 계정 전환 중이라는 사실을 숨기지 않는다 — 감사 추적의 일부다 (ERD § 8 쟁점 7) --%>
	<c:if test="${not empty sessionScope.impersonatorId}">
		<p class="page_header_notice">
			[dev] 개발자 계정(#${sessionScope.impersonatorId})이
			<c:out value="${sessionScope.loginMember.loginId}" /> 계정으로 전환 중입니다.
			<a href="${ctx}/dev/loginAs/return">원래 계정으로</a>
		</p>
	</c:if>

	<c:if test="${maintenance}">
		<p class="page_header_notice">[점검 모드] 일반 사용자에게는 점검 안내가 보입니다.</p>
	</c:if>

	<nav class="page_nav">
		<ul>
			<li><a href="${ctx}/product/list">전체상품</a></li>
			<c:forEach var="cat" items="${navCategories}">
				<li>
					<a href="${ctx}/product/list?categoryId=${cat.categoryId}"><c:out value="${cat.name}" /></a>
					<c:if test="${not empty cat.children}">
						<ul>
							<c:forEach var="sub" items="${cat.children}">
								<li><a href="${ctx}/product/list?categoryId=${sub.categoryId}"><c:out value="${sub.name}" /></a></li>
							</c:forEach>
						</ul>
					</c:if>
				</li>
			</c:forEach>
			<li><a href="${ctx}/notice/list">공지사항</a></li>
		</ul>
	</nav>

	<form class="page_search" action="${ctx}/product/list" method="get">
		<label for="headerKeyword">상품검색</label>
		<input type="text" id="headerKeyword" name="keyword" value="<c:out value='${param.keyword}' />">
		<button type="submit" class="btn">검색</button>
	</form>
</header>

<%-- 한 번만 보여주고 지운다 --%>
<c:if test="${not empty sessionScope.flashMessage}">
	<p class="toast"><c:out value="${sessionScope.flashMessage}" /></p>
	<c:remove var="flashMessage" scope="session" />
</c:if>
<c:if test="${not empty sessionScope.flashError}">
	<p class="toast toast_error"><c:out value="${sessionScope.flashError}" /></p>
	<c:remove var="flashError" scope="session" />
</c:if>
<c:if test="${not empty errorMessage}">
	<p class="toast toast_error"><c:out value="${errorMessage}" /></p>
</c:if>

<main class="page_content">
