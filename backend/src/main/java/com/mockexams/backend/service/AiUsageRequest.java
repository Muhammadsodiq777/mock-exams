package com.mockexams.backend.service;

import com.mockexams.backend.entity.AiFeedbackTypes;

public record AiUsageRequest(
		String feedbackType,
		Long writingSubmissionId,
		Long speakingSubmissionId
) {
	public static AiUsageRequest studyPlan() {
		return new AiUsageRequest(AiFeedbackTypes.STUDY_PLAN, null, null);
	}
}
