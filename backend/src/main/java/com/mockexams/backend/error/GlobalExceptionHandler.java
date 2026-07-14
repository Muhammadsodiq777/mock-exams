package com.mockexams.backend.error;

import java.util.List;
import java.util.UUID;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
class GlobalExceptionHandler {

	@ExceptionHandler(BusinessException.class)
	ResponseEntity<ApiErrorEnvelope> handleBusinessException(BusinessException ex) {
		return error(ex.getStatus(), ex.getCode(), ex.getMessage(), null);
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	ResponseEntity<ApiErrorEnvelope> handleValidationException(MethodArgumentNotValidException ex) {
		List<ApiFieldError> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
				.map(this::toApiFieldError)
				.toList();
		return error(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", "Validation failed.", fieldErrors);
	}

	@ExceptionHandler(ConstraintViolationException.class)
	ResponseEntity<ApiErrorEnvelope> handleConstraintViolationException(ConstraintViolationException ex) {
		List<ApiFieldError> fieldErrors = ex.getConstraintViolations().stream()
				.map(this::toApiFieldError)
				.toList();
		return error(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", "Validation failed.", fieldErrors);
	}

	@ExceptionHandler(MethodArgumentTypeMismatchException.class)
	ResponseEntity<ApiErrorEnvelope> handleMethodArgumentTypeMismatchException(MethodArgumentTypeMismatchException ex) {
		List<ApiFieldError> fieldErrors = List.of(
				new ApiFieldError(ex.getName(), "TypeMismatch", "Invalid parameter value."));
		return error(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", "Validation failed.", fieldErrors);
	}

	@ExceptionHandler(DataIntegrityViolationException.class)
	ResponseEntity<ApiErrorEnvelope> handleDataIntegrityViolation() {
		return error(HttpStatus.CONFLICT, "CONFLICT", "Request conflicts with current state.", null);
	}

	@ExceptionHandler(Exception.class)
	ResponseEntity<ApiErrorEnvelope> handleUnexpectedException() {
		return error(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "Unexpected server error.", null);
	}

	private ApiFieldError toApiFieldError(FieldError fieldError) {
		return new ApiFieldError(fieldError.getField(), fieldError.getCode(), fieldError.getDefaultMessage());
	}

	private ApiFieldError toApiFieldError(ConstraintViolation<?> violation) {
		return new ApiFieldError(fieldName(violation), violation.getConstraintDescriptor()
				.getAnnotation()
				.annotationType()
				.getSimpleName(), violation.getMessage());
	}

	private String fieldName(ConstraintViolation<?> violation) {
		String path = violation.getPropertyPath().toString();
		int lastDot = path.lastIndexOf('.');
		if (lastDot >= 0 && lastDot < path.length() - 1) {
			return path.substring(lastDot + 1);
		}
		return path;
	}

	private ResponseEntity<ApiErrorEnvelope> error(
			HttpStatus status,
			String code,
			String message,
			List<ApiFieldError> fieldErrors
	) {
		ApiError apiError = new ApiError(code, message, fieldErrors, null, UUID.randomUUID().toString());
		return ResponseEntity.status(status).body(new ApiErrorEnvelope(apiError));
	}
}
