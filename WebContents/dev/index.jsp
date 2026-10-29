<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
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

<h2>dev</h2>

<p>
	시연과 점검을 위해 정상 규칙을 넘어서는 조작을 모아 둔 영역입니다.
	DEV 권한만 들어올 수 있습니다 (DevFilter).
</p>

<h3>현재 전역 설정</h3>
<table class="listtable">
	<thead class="listtable_head"><tr><th>키</th><th>값</th><th>설명</th><th>변경 시각</th></tr></thead>
	<tbody>
		<c:forEach var="cfg" items="${configs}">
			<tr>
				<td><c:out value="${cfg.configKey}" /></td>
				<td><strong><c:out value="${cfg.configValue}" /></strong></td>
				<td><c:out value="${cfg.description}" /></td>
				<td><fmt:formatDate value="${cfg.updatedAt}" pattern="yyyy-MM-dd HH:mm" /></td>
			</tr>
		</c:forEach>
	</tbody>
</table>

<h3>화면 안내</h3>
<table class="listtable">
	<thead class="listtable_head"><tr><th>화면</th><th>하는 일</th></tr></thead>
	<tbody>
		<tr><td><a href="${ctx}/dev/state">상태 컨트롤러</a></td>
		    <td>배송 · 주문 · 결제 상태 강제 변경, 재고 지정, 점검 모드 토글</td></tr>
		<tr><td><a href="${ctx}/dev/data">데이터 브라우저</a></td>
		    <td>허용 목록에 있는 17 테이블 + 뷰 5개 조회</td></tr>
		<tr><td><a href="${ctx}/dev/erd">설계 보기</a></td>
		    <td>information_schema 로 실제 스키마 · 제약 조회</td></tr>
		<tr><td><a href="${ctx}/dev/seed">데이터 관리</a></td>
		    <td>테이블 비우기 · 더미 상품 생성 (member 는 USER 만 삭제)</td></tr>
		<tr><td><a href="${ctx}/dev/loginAs">계정 전환</a></td>
		    <td>다른 회원 세션으로 전환 (원래 계정으로 복귀 가능)</td></tr>
		<tr><td><a href="${ctx}/dev/log">감사 로그</a></td>
		    <td>admin_log 조회 — 전환 중 조작의 실제 행위자 추적</td></tr>
	</tbody>
</table>
<%@ include file="/common/footer.jsp" %>
