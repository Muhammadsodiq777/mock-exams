package com.mockexams.backend.support;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import org.springframework.test.web.servlet.ResultMatcher;

public final class ApiContractAssertions {

	private ApiContractAssertions() {
	}

	public static ResultMatcher successEnvelope() {
		return result -> {
			jsonPath("$.data").hasJsonPath().match(result);
			jsonPath("$.meta.requestId").isNotEmpty().match(result);
			jsonPath("$.error").doesNotExist().match(result);
		};
	}

	public static ResultMatcher pageEnvelope() {
		return result -> {
			successEnvelope().match(result);
			jsonPath("$.data").isArray().match(result);
			jsonPath("$.meta.page.number").isNumber().match(result);
			jsonPath("$.meta.page.size").isNumber().match(result);
			jsonPath("$.meta.page.totalElements").isNumber().match(result);
			jsonPath("$.meta.page.totalPages").isNumber().match(result);
			jsonPath("$.meta.page.first").isBoolean().match(result);
			jsonPath("$.meta.page.last").isBoolean().match(result);
		};
	}

	public static ResultMatcher errorEnvelope(String code) {
		return result -> {
			jsonPath("$.error.code").value(code).match(result);
			jsonPath("$.error.message").isNotEmpty().match(result);
			jsonPath("$.error.requestId").isNotEmpty().match(result);
			jsonPath("$.data").doesNotExist().match(result);
			jsonPath("$.meta").doesNotExist().match(result);
		};
	}

	public static ResultMatcher validationFieldError(String field, String code) {
		return result -> {
			errorEnvelope("VALIDATION_FAILED").match(result);
			jsonPath("$.error.fieldErrors[?(@.field == '%s' && @.code == '%s')]".formatted(field, code))
					.isNotEmpty()
					.match(result);
		};
	}
}
