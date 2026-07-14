package com.mockexams.backend.service.writing;

import java.util.List;

public class WritingFeedbackValidationException extends RuntimeException {

	private final List<String> validationErrors;

	public WritingFeedbackValidationException(List<String> validationErrors) {
		super(String.join("; ", validationErrors));
		this.validationErrors = List.copyOf(validationErrors);
	}

	public List<String> getValidationErrors() {
		return validationErrors;
	}
}
