<%-- 마이페이지 메뉴 (지금 화면 표시는 ui.js 가 한다) --%>
<ul class="my_nav">
  <li><a href="${ctx}/mypage/">마이페이지</a></li>
  <li><a href="${ctx}/mypage/edit">정보수정</a></li>
  <li><a href="${ctx}/mypage/address">배송지</a></li>
  <li><a href="${ctx}/mypage/wishlist">찜 목록</a></li>
  <li><a href="${ctx}/mypage/combo">내 조합</a></li>
  <li><a href="${ctx}/mypage/review">내 리뷰</a></li>
  <li><a href="${ctx}/mypage/qna">내 문의</a></li>
  <li><a class="${myTab == 'order' ? 'on' : ''}" href="${ctx}/order/list">주문 내역</a></li>
</ul>
