package com.mockexams.backend.dto;

import java.time.OffsetDateTime;

public record StudentTestAttemptRDO(
		Long id,
		Long testId,
		Long studentUserId,
		String status,
		OffsetDateTime startedAt,
		OffsetDateTime submittedAt,
		OffsetDateTime expiresAt,
		Integer durationMinutes,
		Long remainingSeconds,
		Integer timeSpentSeconds
) {
}
