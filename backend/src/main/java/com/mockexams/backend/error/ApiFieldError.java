package com.mockexams.backend.error;

public record ApiFieldError(
		String field,
		String code,
		String message
) {
}
