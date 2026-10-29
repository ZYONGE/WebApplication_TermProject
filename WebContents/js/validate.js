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
			test: function (v) { return v.length > 0 && v.length <= 30; },
			message: "이름을 30자 이내로 입력하세요."
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
		}
	};

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
			if (!rule.test(value)) {
				alert(field.getAttribute("data-message") || rule.message);
				field.focus();
				return false;
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

	/* 삭제처럼 되돌릴 수 없는 동작은 한 번 묻는다. */
	document.addEventListener("click", function (event) {
		var el = event.target.closest("[data-confirm-message]");
		if (el && !window.confirm(el.getAttribute("data-confirm-message"))) {
			event.preventDefault();
		}
	});
})();
