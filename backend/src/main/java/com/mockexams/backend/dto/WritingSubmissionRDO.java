package com.mockexams.backend.dto;

import java.time.OffsetDateTime;

import com.fasterxml.jackson.annotation.JsonInclude;

public record WritingSubmissionRDO(
		Long id,
		Long studentUserId,
		@JsonInclude(JsonInclude.Include.NON_NULL) Long testAttemptId,
		String taskType,
		String prompt,
		String answerText,
		Integer wordCount,
		OffsetDateTime createdAt,
		@JsonInclude(JsonInclude.Include.NON_NULL) WritingFeedbackRDO feedback
) {
}
