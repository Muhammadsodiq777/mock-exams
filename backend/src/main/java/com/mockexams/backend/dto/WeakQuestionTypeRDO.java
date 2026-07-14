package com.mockexams.backend.dto;

import java.time.OffsetDateTime;

public record WeakQuestionTypeRDO(
		String skill,
		String questionType,
		int incorrectCount,
		int attemptCount,
		OffsetDateTime latestSeenAt
) {
}
