package com.mockexams.backend.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record RecentAttemptRDO(
		Long attemptId,
		Long resultId,
		Long testId,
		String testTitle,
		String testType,
		String attemptStatus,
		OffsetDateTime startedAt,
		OffsetDateTime submittedAt,
		Integer timeSpentSeconds,
		BigDecimal rawScore,
		BigDecimal maxScore,
		BigDecimal estimatedBand
) {
}
