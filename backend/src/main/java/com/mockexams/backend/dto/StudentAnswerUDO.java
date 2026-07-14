package com.mockexams.backend.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record StudentAnswerUDO(
		@NotNull @Min(1) Long questionId,
		String answerText,
		@Min(1) Long selectedOptionId,
		@Min(1) Long audioFileId
) {
}
