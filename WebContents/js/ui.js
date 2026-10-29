/*
  표현 인터랙션 — UI/UX 트랙 소유 (CLAUDE.md § 4.1)

  ★ 이 파일은 샌드박스 임시본이다.
    배선을 확인하려면 팝업을 여닫는 수단이 하나는 있어야 해서 최소한만 넣었다.
    디자인 · 애니메이션 · 포커스 트랩 등은 UI/UX 세션의 몫이다.

  팝업은 HTML 표준 <dialog> 를 쓴다. CSS 없이도 모달로 동작하므로
  "디자인 요소 없이 배선만" 이라는 이번 작업 조건에 맞는 유일한 선택이었다.
  css/DECISIONS.md O-02(별창 vs 인페이지 모달) 가 확정되면 그에 맞춰 교체한다.
*/
(function () {
	"use strict";

	window.openPopup = function (id) {
		var popup = document.getElementById(id);
		if (popup && typeof popup.showModal === "function") {
			popup.showModal();
		}
	};

	window.closePopup = function (id) {
		var popup = document.getElementById(id);
		if (popup && typeof popup.close === "function") {
			popup.close();
		}
	};

	/* data-popup-open="id" / data-popup-close="id" 로도 쓸 수 있게 해 둔다. */
	document.addEventListener("click", function (event) {
		var opener = event.target.closest("[data-popup-open]");
		if (opener) {
			event.preventDefault();
			window.openPopup(opener.getAttribute("data-popup-open"));
			return;
		}
		var closer = event.target.closest("[data-popup-close]");
		if (closer) {
			event.preventDefault();
			window.closePopup(closer.getAttribute("data-popup-close"));
		}
	});
})();
