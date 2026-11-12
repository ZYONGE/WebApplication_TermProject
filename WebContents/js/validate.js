/*
  폼 검증 — JSP 배선 트랙 소유 (CLAUDE.md § 4.1)

  ★ 규칙은 서버(shop.service.MemberService) 와 같아야 한다.
    여기서 막는 것은 왕복을 줄이기 위해서일 뿐, 판정의 근거는 언제나 서버다.
    규칙을 고치면 두 곳을 같이 고친다.
*/
(function () {
	"use strict";

	var RULES = {
		loginId: {
			test: function (v) { return /^[A-Za-z0-9_]{4,30}$/.test(v); },
			message: "아이디는 영문 · 숫자 · 밑줄 4~30자로 입력하세요."
		},
		password: {
			test: function (v) { return v.length >= 8 && v.length <= 100; },
			message: "비밀번호는 8자 이상으로 입력하세요."
		},
		name: {
			test: function (v) { return v.length <= 30; },
			message: "이름은 30자 이내로 입력하세요."
		},
		email: {
			test: function (v) { return /^[^@\s]+@[^@\s]+\.[A-Za-z]{2,}$/.test(v); },
			message: "이메일 형식이 올바르지 않습니다."
		},
		phone: {
			test: function (v) { return /^0\d{1,2}-?\d{3,4}-?\d{4}$/.test(v); },
			message: "연락처 형식이 올바르지 않습니다. (예: 010-1234-5678)"
		},
		required: {
			test: function (v) { return v.length > 0; },
			message: "필수 입력 항목입니다."
		},
		positiveInt: {
			test: function (v) { return /^[1-9]\d*$/.test(v); },
			message: "1 이상의 숫자를 입력하세요."
		},
		/*
		  섭취 일수 — 1 ~ data-max (MAX_INTAKE_DAYS 와 재고 중 작은 값).
		  서버 CartService.checkDays 가 같은 범위를 다시 검사한다.
		*/
		intakeDays: {
			test: function (v, field) {
				var max = parseInt(field.getAttribute("data-max"), 10) || 90;
				return /^[1-9]\d*$/.test(v) && parseInt(v, 10) <= max;
			},
			message: "섭취 일수를 확인하세요. 1일 이상, 담을 수 있는 일수 이하로 입력합니다."
		}
	};

	/* 알림에 붙일 칸 이름 — 연결된 label 글자(* 표시 제외) → placeholder → 없으면 빈 값 */
	function fieldName(field) {
		var label = field.id ? document.querySelector("label[for='" + field.id + "']") : null;
		var text = label ? label.textContent : (field.getAttribute("placeholder") || "");
		return text.replace(/\*/g, "").replace(/\s+/g, " ").trim();
	}

	function fail(field, message) {
		var name = fieldName(field);
		alert(name && message.indexOf(name) < 0 ? "[" + name + "] " + message : message);
		field.focus();
		return false;
	}

	/* data-rule 속성이 붙은 입력만 검사한다. */
	function validateForm(form) {
		var fields = form.querySelectorAll("[data-rule]");
		for (var i = 0; i < fields.length; i++) {
			var field = fields[i];
			var rule = RULES[field.getAttribute("data-rule")];
			if (!rule) {
				continue;
			}
			var value = (field.value || "").trim();
			/* 빈 칸은 규칙 문구("30자 이내" 등)가 아니라 "입력하세요" 로 알린다 */
			if (value === "") {
				return fail(field, field.getAttribute("data-message") || "필수 입력 항목입니다. 값을 입력하세요.");
			}
			if (!rule.test(value, field)) {
				return fail(field, field.getAttribute("data-message") || rule.message);
			}
		}

		/*
		  확인이 필요한 칸 — 아이디 중복확인처럼 버튼으로 확인한 값과 지금 값이 같아야 한다.
		    data-check-required="안내 문구"  data-checked-value="확인을 통과한 값"
		  값을 바꾸면 확인을 다시 해야 한다. (판정은 서버가 다시 한다)
		*/
		var checks = form.querySelectorAll("[data-check-required]");
		for (var k = 0; k < checks.length; k++) {
			if ((checks[k].value || "").trim() !== checks[k].getAttribute("data-checked-value")) {
				return fail(checks[k], checks[k].getAttribute("data-check-required"));
			}
		}

		/* 비밀번호 확인 — 짝이 되는 입력을 data-confirm 으로 가리킨다. */
		var confirmField = form.querySelector("[data-confirm]");
		if (confirmField) {
			var target = form.querySelector("[name='" + confirmField.getAttribute("data-confirm") + "']");
			if (target && target.value !== confirmField.value) {
				alert("비밀번호 확인이 일치하지 않습니다.");
				confirmField.focus();
				return false;
			}
		}
		return true;
	}

	document.addEventListener("submit", function (event) {
		var form = event.target;
		if (form.hasAttribute("data-validate") && !validateForm(form)) {
			event.preventDefault();
		}
	});

	/*
	  삭제처럼 되돌릴 수 없는 동작은 한 번 묻는다.
	  검증할 폼의 제출 버튼이면 입력 검사를 먼저 한다 — 빈 칸이 있는데 "결제하시겠습니까?" 부터 묻지 않는다.
	*/
	document.addEventListener("click", function (event) {
		var el = event.target.closest("[data-confirm-message]");
		if (!el) {
			return;
		}
		var form = el.form || el.closest("form");
		var isSubmit = el.tagName === "BUTTON" && (el.type || "submit") === "submit";
		if (isSubmit && form && form.hasAttribute("data-validate") && !validateForm(form)) {
			event.preventDefault();
			return;
		}
		if (!window.confirm(el.getAttribute("data-confirm-message"))) {
			event.preventDefault();
		}
	});

	window.validateForm = validateForm;
})();
