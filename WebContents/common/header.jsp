<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%-- 모든 화면의 머리글. 화면 JSP 맨 위에서 include 한다 (c · fmt 태그도 여기서 선언) --%>
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<c:set var="me" value="${sessionScope.loginMember}" />
<!DOCTYPE html>
<html lang="ko">
<head>
<meta charset="UTF-8">
<title><c:out value="${pageTitle}" /></title>
<link rel="stylesheet" href="${ctx}/css/base/reset.css">
<link rel="stylesheet" href="${ctx}/css/base/variables.css">
<link rel="stylesheet" href="${ctx}/css/base/layout.css">
<link rel="stylesheet" href="${ctx}/css/components/button.css">
<link rel="stylesheet" href="${ctx}/css/components/form.css">
<link rel="stylesheet" href="${ctx}/css/components/card.css">
<link rel="stylesheet" href="${ctx}/css/components/table.css">
<link rel="stylesheet" href="${ctx}/css/components/pagination.css">
<link rel="stylesheet" href="${ctx}/css/components/popup.css">
<link rel="stylesheet" href="${ctx}/css/components/tab.css">
<link rel="stylesheet" href="${ctx}/css/components/warnbox.css">
<link rel="stylesheet" href="${ctx}/css/components/subnav.css">
<link rel="stylesheet" href="${ctx}/css/pages/main.css">
<link rel="stylesheet" href="${ctx}/css/pages/notice.css">
<link rel="stylesheet" href="${ctx}/css/pages/product.css">
<link rel="stylesheet" href="${ctx}/css/pages/custom.css">
<link rel="stylesheet" href="${ctx}/css/pages/cart.css">
<link rel="stylesheet" href="${ctx}/css/pages/order.css">
<link rel="stylesheet" href="${ctx}/css/pages/member.css">
<link rel="stylesheet" href="${ctx}/css/pages/admin.css">
<script src="${ctx}/js/ui.js" defer></script>
<script src="${ctx}/js/validate.js" defer></script>
</head>
<body>
<div class="wrap">
<header class="page_header">
  <h1><a href="${ctx}/">윤라댄</a></h1>
  <ul class="page_header_util">
    <c:if test="${empty me}">
      <li><a href="${ctx}/member/login">로그인</a></li>
      <li><a href="${ctx}/member/join">회원가입</a></li>
    </c:if>
    <c:if test="${not empty me}">
      <li><c:out value="${me.name}" />님<c:if test="${me.role != 'USER'}"> (${me.roleLabel})</c:if></li>
      <c:if test="${me.role == 'USER'}">
        <li><a href="${ctx}/mypage/">마이페이지</a></li>
        <li><a href="${ctx}/mypage/combo">내 조합</a></li>
        <li><a href="${ctx}/cart/list">장바구니<c:if test="${cartCount > 0}"> (${cartCount})</c:if></a></li>
      </c:if>
      <c:if test="${me.role == 'ADMIN'}"><li><a href="${ctx}/admin/dashboard">관리자</a></li></c:if>
      <c:if test="${me.role == 'DEV'}"><li><a href="${ctx}/dev/">dev</a></li></c:if>
      <li><a href="${ctx}/member/logout">로그아웃</a></li>
    </c:if>
  </ul>
  <c:if test="${not empty sessionScope.impersonatorId}">
    <p class="page_header_notice">
      [dev] 개발자 계정(#${sessionScope.impersonatorId})이 <c:out value="${me.loginId}" /> 계정으로 전환 중입니다.
      <a href="${ctx}/dev/loginAs/return">원래 계정으로</a>
    </p>
  </c:if>
  <c:if test="${maintenance}">
    <p class="page_header_notice">[점검 모드] 일반 사용자에게는 점검 안내가 보입니다.</p>
  </c:if>
  <nav class="page_nav">
    <ul>
      <li><a href="${ctx}/product/list">전체 영양제</a></li>
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
      <li class="page_nav_custom"><a href="${ctx}/custom">조합 맞추기</a></li>
    </ul>
  </nav>
  <form class="page_search" action="${ctx}/product/list" method="get">
    <label for="headerKeyword">영양제 검색</label>
    <input type="text" id="headerKeyword" name="keyword" value="<c:out value='${param.keyword}' />">
    <button type="submit" class="btn">검색</button>
  </form>
</header>
<%-- 한 번만 보여주는 알림 (서블릿의 flash · flashError) --%>
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
