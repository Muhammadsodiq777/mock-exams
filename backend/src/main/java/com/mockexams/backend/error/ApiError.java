package com.mockexams.backend.error;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiError(
		String code,
		String message,
		List<ApiFieldError> fieldErrors,
		Map<String, Object> details,
		String requestId
) {
}
