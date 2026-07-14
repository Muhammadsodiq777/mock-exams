package com.mockexams.backend.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record SpeakingSubmissionRDO(
		Long id,
		Long studentUserId,
		Long testAttemptId,
		String partType,
		String prompt,
		Long audioFileId,
		String transcript,
		Integer durationSeconds,
		String transcriptionStatus,
		String provider,
		String model,
		String failureCode,
		String errorMessage,
		BigDecimal estimatedCost,
		OffsetDateTime transcriptionStartedAt,
		OffsetDateTime transcriptionCompletedAt,
		OffsetDateTime createdAt
) {
}
