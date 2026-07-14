package com.mockexams.backend.service.speaking;

import java.util.List;

public class SpeakingFeedbackValidationException extends RuntimeException {

	private final List<String> validationErrors;

	public SpeakingFeedbackValidationException(List<String> validationErrors) {
		super(String.join("; ", validationErrors));
		this.validationErrors = List.copyOf(validationErrors);
	}

	public List<String> getValidationErrors() {
		return validationErrors;
	}
}
