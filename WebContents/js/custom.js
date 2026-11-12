/*
  섭취 일수 선택 · 조합 탭 즉시 계산 — JSP 배선 트랙 소유 (IMPACT K11)

  ★ 여기서 하는 계산은 편의일 뿐이다. 합계 · 경고 · 담기 가능 여부의 판정은
    담을 때 서버(CartService.addAll · IntakeService)가 다시 한다.
  ★ 금지 조합 목록은 서버가 페이지에 데이터(#customConflicts)로 심어 준다. 외부 요청은 없다.

  1. 섭취 일수 빠른 선택 — 상품상세 · 조합 탭 공통
       <button data-days-value="30" data-days-target="입력id">       누르면 입력에 값을 넣는다
       <p data-days-total="입력id" data-price="1포가격">              [data-days-count] · [data-days-sum] 을 다시 쓴다
  2. 조합 탭 (#customForm) — 고르는 즉시 내 조합 · 합계 · 배송비 · 경고 · 공유 링크를 다시 그린다
*/
(function () {
	"use strict";

	function fmt(n) {
		return String(n).replace(/\B(?=(\d{3})+(?!\d))/g, ",");
	}

	function toInt(v) {
		var n = parseInt(v, 10);
		return isNaN(n) ? 0 : n;
	}

	/* ---------------- 1. 섭취 일수 ---------------- */

	function refreshDays(input) {
		var value = String(toInt(input.value));
		var buttons = document.querySelectorAll('[data-days-target="' + input.id + '"]');
		for (var i = 0; i < buttons.length; i++) {
			buttons[i].classList.toggle("on", buttons[i].getAttribute("data-days-value") === value);
		}
		var totals = document.querySelectorAll('[data-days-total="' + input.id + '"]');
		var max = toInt(input.getAttribute("max")) || 0;
		for (var j = 0; j < totals.length; j++) {
			var days = toInt(input.value);
			var price = toInt(totals[j].getAttribute("data-price"));
			var count = totals[j].querySelector("[data-days-count]");
			var sum = totals[j].querySelector("[data-days-sum]");
			/* 담을 수 없는 일수(0 이하 · 상한 초과)는 합계를 내지 않고 범위를 알린다 */
			var ok = days >= 1 && (max === 0 || days <= max);
			if (count) { count.textContent = days; }
			if (sum) { sum.textContent = ok ? fmt(price * days) : "— (1~" + max + "일로 입력)"; }
		}
	}

	document.addEventListener("click", function (event) {
		var button = event.target.closest("[data-days-value]");
		if (!button) {
			return;
		}
		event.preventDefault();
		var input = document.getElementById(button.getAttribute("data-days-target"));
		if (!input) {
			return;
		}
		input.value = button.getAttribute("data-days-value");
		input.dispatchEvent(new Event("input", { bubbles: true }));
	});

	document.addEventListener("input", function (event) {
		if (event.target.id && document.querySelector('[data-days-target="' + event.target.id + '"]')) {
			refreshDays(event.target);
		}
	});

	/* ---------------- 2. 조합 탭 ---------------- */

	var form = document.getElementById("customForm");
	if (!form) {
		return;
	}

	var ctx = form.getAttribute("data-ctx") || "";
	var baseFee = toInt(form.getAttribute("data-fee"));
	var freeMin = toInt(form.getAttribute("data-free-min"));
	var maxDays = toInt(form.getAttribute("data-max-days")) || 90;
	var daysInput = document.getElementById("customDays");
	var boxes = form.querySelectorAll('input[type="checkbox"][name="productId"]');
	var lastSubmitter = null;

	/* 서버가 심어 둔 금지 조합 */
	var conflicts = [];
	var rows = document.querySelectorAll("#customConflicts li");
	for (var r = 0; r < rows.length; r++) {
		conflicts.push({
			a: rows[r].getAttribute("data-a"),
			b: rows[r].getAttribute("data-b"),
			nameA: rows[r].getAttribute("data-name-a"),
			nameB: rows[r].getAttribute("data-name-b"),
			message: rows[r].getAttribute("data-message")
		});
	}

	function pickedBoxes() {
		var list = [];
		for (var i = 0; i < boxes.length; i++) {
			if (boxes[i].checked) {
				list.push(boxes[i]);
			}
		}
		return list;
	}

	function currentDays() {
		var days = toInt(daysInput.value);
		return Math.min(Math.max(days, 1), maxDays);
	}

	function el(tag, className, text) {
		var node = document.createElement(tag);
		if (className) { node.className = className; }
		if (text !== undefined) { node.textContent = text; }
		return node;
	}

	function render() {
		var picked = pickedBoxes();
		var ids = {};
		var idList = [];
		var pack = 0;

		/* 내 조합 목록 — textContent 로만 넣는다 (상품명 이스케이프) */
		var ul = document.getElementById("customPicked");
		var days = currentDays();
		ul.innerHTML = "";
		for (var i = 0; i < picked.length; i++) {
			var box = picked[i];
			var price = toInt(box.getAttribute("data-price"));
			ids[box.value] = true;
			idList.push(box.value);
			pack += price;

			var li = el("li");
			li.setAttribute("data-id", box.value);
			li.appendChild(el("span", "custom_picked_name", box.getAttribute("data-name")));
			li.appendChild(el("span", "custom_picked_price", "1포 " + fmt(price) + "원"));
			/* 재고보다 많은 일수 — 담을 때 서버가 이 상품을 뺀다. 고르는 즉시 알린다 */
			var stock = toInt(box.getAttribute("data-stock"));
			if (stock < days) {
				li.appendChild(el("span", "custom_picked_warn", "재고 " + stock + "포 — " + days + "일분은 담을 수 없음"));
			}
			var remove = el("button", "btn btn_small btn_sub", "빼기");
			remove.type = "button";
			remove.setAttribute("data-unpick", box.value);
			li.appendChild(remove);
			ul.appendChild(li);
		}
		document.getElementById("customCount").textContent = picked.length;
		document.getElementById("customEmpty").hidden = picked.length > 0;

		/* 합계 · 배송비 미리보기 — 서버 ConfigService.calculateDeliveryFee 와 같은 식 */
		var total = pack * days;
		var fee = total <= 0 ? 0 : (total >= freeMin ? 0 : baseFee);
		document.getElementById("sumPack").textContent = fmt(pack);
		document.getElementById("sumDays").textContent = days;
		document.getElementById("sumPacks").textContent = days;
		document.getElementById("sumTotal").textContent = fmt(total);
		document.getElementById("sumFee").textContent = fmt(fee);

		/* 경고 — 고른 낱개 쌍 중 금지 조합 (조합 탭은 낱개만 고르므로 중복 섭취가 생기지 않는다) */
		var warnList = document.getElementById("customWarnList");
		warnList.innerHTML = "";
		var hits = 0;
		for (var c = 0; c < conflicts.length; c++) {
			var cf = conflicts[c];
			if (ids[cf.a] && ids[cf.b]) {
				var item = el("li", "warnbox_conflict");
				item.appendChild(el("strong", "warnbox_label", "동시 섭취 주의"));
				item.appendChild(document.createTextNode(" " + cf.nameA + " × " + cf.nameB + " "));
				item.appendChild(el("span", "warnbox_msg", cf.message));
				warnList.appendChild(item);
				hits++;
			}
		}
		document.getElementById("customWarn").hidden = hits === 0;
		document.getElementById("customWarnOk").hidden = !(hits === 0 && picked.length > 0);

		/* 조합 상태를 URL 에 둔다 — 새로고침 · 링크 공유 (IMPACT K3) */
		var query = "pick=" + idList.join(",") + "&days=" + days;
		var share = document.getElementById("customShare");
		share.href = ctx + "/custom?" + query;
		share.textContent = ctx + "/custom?" + query;
		var login = document.getElementById("customLogin");
		if (login) {
			login.href = ctx + "/member/login?returnUrl=" + encodeURIComponent(ctx + "/custom?" + query);
		}
		if (window.history && window.history.replaceState) {
			window.history.replaceState(null, "", ctx + "/custom?" + query);
		}
	}

	/* 불러온 뒤 고르기를 바꾸면 "…을(를) 불러왔습니다" 배너는 더 이상 맞지 않는다 — 내린다 */
	function dropLoadedBanner() {
		var banners = document.querySelectorAll("[data-loaded]");
		for (var b = 0; b < banners.length; b++) {
			banners[b].hidden = true;
		}
	}

	form.addEventListener("change", function (event) {
		if (event.target.name === "productId") {
			dropLoadedBanner();
		}
		render();
	});
	daysInput.addEventListener("input", render);

	form.addEventListener("click", function (event) {
		var unpick = event.target.closest("[data-unpick]");
		if (unpick) {
			var box = document.getElementById("pick" + unpick.getAttribute("data-unpick"));
			if (box) {
				box.checked = false;
			}
			dropLoadedBanner();
			render();
			return;
		}
		var submit = event.target.closest("button[type='submit']");
		if (submit) {
			lastSubmitter = submit;
		}
	});

	/* 담기 · 저장 전 확인 — 서버가 같은 검사를 다시 한다 */
	form.addEventListener("submit", function (event) {
		var submitter = event.submitter || lastSubmitter;
		if (pickedBoxes().length === 0) {
			alert("영양제를 하나 이상 고르세요.");
			event.preventDefault();
			return;
		}
		var days = toInt(daysInput.value);
		if (days < 1 || days > maxDays) {
			alert("섭취 일수는 1~" + maxDays + "일로 입력하세요.");
			daysInput.focus();
			event.preventDefault();
			return;
		}
		if (submitter && submitter.id === "customSave") {
			var nameField = document.getElementById("comboName");
			var name = (nameField.value || "").trim();
			if (!name) {
				alert("조합 이름을 입력하세요.");
				nameField.focus();
				event.preventDefault();
				return;
			}
			/* 같은 이름이 있으면 덮어쓸지 묻는다 (ERD § 7.1 조합 저장) */
			var overwrite = document.getElementById("comboOverwrite");
			overwrite.value = "";
			var names = document.querySelectorAll("[data-combo-name]");
			for (var i = 0; i < names.length; i++) {
				if (names[i].getAttribute("data-combo-name") === name) {
					if (!window.confirm("'" + name + "' 조합이 이미 있습니다. 덮어쓸까요?")) {
						event.preventDefault();
						return;
					}
					overwrite.value = "Y";
				}
			}
		}
	});

	render();
})();
