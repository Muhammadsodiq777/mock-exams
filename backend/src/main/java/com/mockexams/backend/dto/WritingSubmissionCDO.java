package com.mockexams.backend.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record WritingSubmissionCDO(
		@NotBlank @Size(max = 32) String taskType,
		@Min(1) Long testAttemptId,
		@NotBlank @Size(max = 20000) String prompt,
		@NotBlank @Size(max = 40000) String answerText
) {
}
