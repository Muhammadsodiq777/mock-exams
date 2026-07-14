package com.mockexams.backend.dto;

import java.time.OffsetDateTime;

public record StudentAnswerRDO(
		Long id,
		Long attemptId,
		Long questionId,
		String answerText,
		Long selectedOptionId,
		Long audioFileId,
		OffsetDateTime answeredAt
) {
}
