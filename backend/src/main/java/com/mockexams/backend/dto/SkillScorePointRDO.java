package com.mockexams.backend.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record SkillScorePointRDO(
		Long resultId,
		Long attemptId,
		Long testId,
		String testTitle,
		String attemptStatus,
		OffsetDateTime scoredAt,
		BigDecimal rawScore,
		BigDecimal maxScore,
		BigDecimal estimatedBand
) {
}
