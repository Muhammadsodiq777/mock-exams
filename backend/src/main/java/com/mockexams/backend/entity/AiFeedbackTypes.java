package com.mockexams.backend.entity;

public final class AiFeedbackTypes {

	public static final String WRITING = "writing";
	public static final String SPEAKING = "speaking";
	public static final String STUDY_PLAN = "study_plan";

	private AiFeedbackTypes() {
	}

	public static boolean isSupported(String feedbackType) {
		return WRITING.equals(feedbackType) || SPEAKING.equals(feedbackType) || STUDY_PLAN.equals(feedbackType);
	}
}
