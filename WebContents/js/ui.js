/*
  표현 인터랙션 — UI/UX 트랙 소유 (CLAUDE.md § 4.1)

  ★ 이 파일은 샌드박스 임시본이다.
    배선을 확인하려면 팝업을 여닫는 수단이 하나는 있어야 해서 최소한만 넣었다.
    디자인 · 애니메이션 · 포커스 트랩 등은 UI/UX 세션의 몫이다.

  팝업은 HTML 표준 <dialog> 를 쓴다. CSS 없이도 모달로 동작하므로
  "디자인 요소 없이 배선만" 이라는 이번 작업 조건에 맞는 유일한 선택이었다.
  css/DECISIONS.md O-02(별창 vs 인페이지 모달) 가 확정되면 그에 맞춰 교체한다.

  탭(data-tab-open)도 같은 방식으로 건다 — 조합 탭(/custom)의 분류 탭이 쓴다.
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

	/*
	  탭 — 팝업과 같은 속성 패턴이다 (IMPACT § 5).
	    <div data-tab> … <button data-tab-open="panelId"> … </div>   묶음 안의 버튼
	    <section id="panelId" class="tab_panel">                     열릴 패널
	  같은 묶음의 버튼과, 그 버튼들이 가리키는 패널만 바꾼다. 패널에는 hidden 을 함께 걸어
	  CSS 가 없어도 한 패널만 보이게 한다.
	*/
	window.openTab = function (opener) {
		var group = opener.closest("[data-tab]") || document;
		var buttons = group.querySelectorAll("[data-tab-open]");
		for (var i = 0; i < buttons.length; i++) {
			var panel = document.getElementById(buttons[i].getAttribute("data-tab-open"));
			var on = buttons[i] === opener;
			buttons[i].classList.toggle("on", on);
			if (panel) {
				panel.classList.toggle("on", on);
				panel.hidden = !on;
			}
		}
	};

	/*
	  이미지가 없을 때 — 깨진 그림 대신 "[이미지 없음]" 글자로 바꾼다.
	  DB 의 thumbnail · img_path 는 있는데 파일이 아직 없는 경우다 (STATUS P5).
	  파일이 준비되면 이 처리는 저절로 동작하지 않는다. error 는 버블링하지 않아 capture 로 받는다.
	*/
	function replaceBroken(img) {
		if (!img || img.tagName !== "IMG" || !img.parentNode) {
			return;
		}
		var label = document.createElement("span");
		label.textContent = "[이미지 없음]";
		label.title = img.getAttribute("alt") || "";
		img.parentNode.replaceChild(label, img);
	}

	document.addEventListener("error", function (event) {
		replaceBroken(event.target);
	}, true);

	/*
	  지금 화면 표시 — 내비 링크 중 현재 주소와 같은 것에 on 을 붙인다.
	    page_nav           경로 + 쿼리까지 같을 때 (카테고리마다 쿼리가 다르다)
	    admin_nav · my_nav 경로만 같으면 (?editId= 같은 쿼리가 붙어도 같은 화면)
	*/
	function markCurrent(selector, withQuery) {
		var links = document.querySelectorAll(selector);
		for (var i = 0; i < links.length; i++) {
			var a = links[i];
			var samePath = a.pathname.replace(/\/$/, "") === location.pathname.replace(/\/$/, "");
			if (samePath && (!withQuery || a.search === location.search)) {
				a.classList.add("on");
			}
		}
	}
	markCurrent(".page_nav > ul > li > a", true);
	markCurrent(".admin_nav a", false);
	markCurrent(".my_nav a", false);

	/* 이 스크립트는 defer 라, 그 전에 이미 실패한 이미지는 이벤트를 놓친다 — 한 번 훑는다. */
	var images = document.querySelectorAll("img");
	for (var k = 0; k < images.length; k++) {
		if (images[k].complete && images[k].naturalWidth === 0) {
			replaceBroken(images[k]);
		}
	}

	/* data-popup-open="id" / data-popup-close="id" 로도 쓸 수 있게 해 둔다. */
	document.addEventListener("click", function (event) {
		var tab = event.target.closest("[data-tab-open]");
		if (tab) {
			event.preventDefault();
			window.openTab(tab);
			return;
		}
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
