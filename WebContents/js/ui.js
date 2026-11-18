/*
  화면 동작 (모든 화면에서 읽는다)
    1. 팝업 — data-popup-open="팝업id" 버튼으로 열고 data-popup-close 로 닫는다.
       버튼의 data-fill-칸id="값" 은 팝업을 열기 전에 그 칸에 넣는다. (예: data-fill-review-id → id="reviewId")
       data-popup-auto 가 붙은 팝업은 화면이 열리자마자 연다.
    2. 탭 — data-tab-open="패널id"
    3. 메뉴의 지금 화면에 on 표시
    4. 없는 이미지는 [이미지 없음] 글자로
    5. 관리자 상품 팝업 — 구분(낱개/트랙)에 맞는 카테고리만 보여 준다
    6. 회원가입 — 아이디 중복확인
*/
(function () {
	"use strict";

	window.openPopup = function (id) {
		document.getElementById(id).showModal();
	};

	window.closePopup = function (id) {
		document.getElementById(id).close();
	};

	/* 탭 — 누른 버튼과 그 패널만 켠다 (같은 data-tab 묶음 안에서) */
	window.openTab = function (opener) {
		var buttons = (opener.closest("[data-tab]") || document).querySelectorAll("[data-tab-open]");
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

	/* data-fill-칸id="값" 들을 같은 id 의 칸에 넣는다 */
	function fill(source) {
		for (var i = 0; i < source.attributes.length; i++) {
			var attr = source.attributes[i];
			if (attr.name.indexOf("data-fill-") !== 0) {
				continue;
			}
			var id = attr.name.substring(10).replace(/-(\w)/g, function (m, c) { return c.toUpperCase(); });
			var field = document.getElementById(id);
			if (!field) {
				continue;
			}
			if (field.type === "checkbox") {
				field.checked = attr.value === "true";
			} else if (/^(INPUT|SELECT|TEXTAREA)$/.test(field.tagName)) {
				field.value = attr.value;
			} else {
				field.textContent = attr.value;
			}
		}
	}

	document.addEventListener("click", function (event) {
		var tab = event.target.closest("[data-tab-open]");
		var opener = event.target.closest("[data-popup-open]");
		var closer = event.target.closest("[data-popup-close]");
		var idcheck = event.target.closest("[data-idcheck]");
		if (tab) {
			event.preventDefault();
			window.openTab(tab);
		} else if (opener) {
			event.preventDefault();
			fill(opener);
			window.openPopup(opener.getAttribute("data-popup-open"));
		} else if (closer) {
			event.preventDefault();
			window.closePopup(closer.getAttribute("data-popup-close"));
		} else if (idcheck) {
			checkLoginId(idcheck.getAttribute("data-idcheck"));
		}
	});

	/* 저장된 배송지 고르기 — 고른 option 의 data-fill-* 를 입력칸에 넣는다 ("직접 입력" 은 그대로) */
	document.addEventListener("change", function (event) {
		var select = event.target;
		if (select.hasAttribute("data-fill-on-change") && select.value) {
			fill(select.options[select.selectedIndex]);
		}
	});

	/* 없는 이미지 → [이미지 없음] (error 는 버블링하지 않아 capture 로 받는다) */
	function replaceBroken(img) {
		if (img.tagName === "IMG" && img.parentNode) {
			var label = document.createElement("span");
			label.textContent = "[이미지 없음]";
			label.title = img.getAttribute("alt") || "";
			img.parentNode.replaceChild(label, img);
		}
	}
	document.addEventListener("error", function (event) {
		replaceBroken(event.target);
	}, true);
	document.querySelectorAll("img").forEach(function (img) {
		if (img.complete && img.naturalWidth === 0) {
			replaceBroken(img);
		}
	});

	/* 지금 화면 표시 — 위 메뉴는 주소 + 쿼리까지, 마이페이지 · 관리자 메뉴는 주소만 같으면 on */
	function markCurrent(selector, withQuery) {
		document.querySelectorAll(selector).forEach(function (a) {
			var samePath = a.pathname.replace(/\/$/, "") === location.pathname.replace(/\/$/, "");
			if (samePath && (!withQuery || a.search === location.search)) {
				a.classList.add("on");
			}
		});
	}
	markCurrent(".page_nav > ul > li > a", true);
	markCurrent(".admin_nav a", false);
	markCurrent(".my_nav a", false);

	/*
	  관리자 상품 팝업 — 구분을 먼저 고르게 한다.
	  구분을 고르기 전엔 카테고리를 잠그고, 고른 뒤엔 그 구분의 대분류(optgroup data-type)만 다시 꽂는다.
	  트랙일 때만 트랙 구성 칸을 보인다. (판정은 서버가 다시 한다)
	*/
	var productType = document.getElementById("productType");
	if (productType) {
		var category = document.getElementById("pCategoryId");
		var groups = Array.prototype.slice.call(category.querySelectorAll("optgroup"));
		var applyType = function () {
			var type = productType.value;
			var keep = category.value;
			groups.forEach(function (g) {
				if (g.parentNode) {
					g.parentNode.removeChild(g);
				}
				if (type !== "" && (g.getAttribute("data-type") === "" || g.getAttribute("data-type") === type)) {
					category.appendChild(g);
				}
			});
			category.value = keep;
			if (category.value !== keep) {
				category.value = "";
			}
			category.disabled = type === "";
			document.getElementById("trackItemsRow").hidden = type !== "TRACK";
		};
		productType.addEventListener("change", applyType);
		applyType();
	}

	document.querySelectorAll("dialog[data-popup-auto]").forEach(function (popup) {
		popup.showModal();
	});

	/* 아이디 중복확인 — 서버 결과(OK / NO)를 받아 보여 준다. 확인한 값은 data-checked-value 에 둔다 */
	var loginId = document.getElementById("loginId");
	function showIdResult(text, state) {
		var result = document.getElementById("idCheckResult");
		result.textContent = text;
		result.className = "field_help" + (state ? " member_" + state : "");
	}
	function checkLoginId(url) {
		var value = loginId.value.trim();
		loginId.setAttribute("data-checked-value", "");
		if (!/^[A-Za-z0-9_]{4,30}$/.test(value)) {
			showIdResult("영문 · 숫자 · 밑줄 4~30자로 입력한 뒤 확인하세요.", "ng");
			return;
		}
		var xhr = new XMLHttpRequest();
		xhr.open("GET", url + "?loginId=" + encodeURIComponent(value), true);
		xhr.onload = function () {
			if (xhr.responseText === "OK") {
				loginId.setAttribute("data-checked-value", value);
				showIdResult("사용할 수 있는 아이디입니다.", "ok");
			} else {
				showIdResult("이미 사용 중인 아이디입니다.", "ng");
			}
		};
		xhr.send();
	}
	/* 확인한 뒤 아이디를 고치면 결과를 지운다 — 다시 확인해야 가입할 수 있다 */
	if (loginId && loginId.hasAttribute("data-check-required")) {
		loginId.addEventListener("input", function () {
			if (loginId.value.trim() !== loginId.getAttribute("data-checked-value")) {
				loginId.setAttribute("data-checked-value", "");
				showIdResult("", null);
			}
		});
	}
})();
