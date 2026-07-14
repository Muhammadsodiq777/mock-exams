package com.mockexams.backend.error;

import org.springframework.http.HttpStatus;

public class BusinessException extends RuntimeException {

	private final String code;
	private final HttpStatus status;

	public BusinessException(String code, String message, HttpStatus status) {
		super(message);
		this.code = code;
		this.status = status;
	}

	public String getCode() {
		return code;
	}

	public HttpStatus getStatus() {
		return status;
	}

	public static BusinessException authenticationRequired(String message) {
		return new BusinessException("AUTHENTICATION_REQUIRED", message, HttpStatus.UNAUTHORIZED);
	}

	public static BusinessException tokenInvalid(String message) {
		return new BusinessException("TOKEN_INVALID", message, HttpStatus.UNAUTHORIZED);
	}

	public static BusinessException forbidden(String message) {
		return new BusinessException("FORBIDDEN", message, HttpStatus.FORBIDDEN);
	}

	public static BusinessException accountInactive(String message) {
		return new BusinessException("ACCOUNT_INACTIVE", message, HttpStatus.FORBIDDEN);
	}
}
