package com.mockexams.backend.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SpeakingSubmissionCDO(
		@NotBlank String partType,
		@NotBlank String prompt,
		@NotNull @Min(1) Long audioFileId,
		@Min(1) Long testAttemptId,
		@NotNull @Min(1) Integer durationSeconds
) {
}
