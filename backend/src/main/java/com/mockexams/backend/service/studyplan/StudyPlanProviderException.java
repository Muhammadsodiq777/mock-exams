package com.mockexams.backend.service.studyplan;

public class StudyPlanProviderException extends RuntimeException {

	public StudyPlanProviderException(String message) {
		super(message);
	}

	public StudyPlanProviderException(String message, Throwable cause) {
		super(message, cause);
	}
}
