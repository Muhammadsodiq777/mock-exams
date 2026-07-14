package com.mockexams.backend.service.studyplan;

import java.util.List;

public class StudyPlanValidationException extends RuntimeException {

	private final List<String> validationErrors;

	public StudyPlanValidationException(List<String> validationErrors) {
		super("Study plan provider response failed validation.");
		this.validationErrors = validationErrors == null ? List.of() : List.copyOf(validationErrors);
	}

	public List<String> getValidationErrors() {
		return validationErrors;
	}
}
