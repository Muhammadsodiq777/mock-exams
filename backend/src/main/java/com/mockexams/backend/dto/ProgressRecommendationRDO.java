package com.mockexams.backend.dto;

import java.time.OffsetDateTime;

public record ProgressRecommendationRDO(
		String sourceType,
		Long testId,
		String title,
		String testType,
		String reason,
		boolean directlyAvailable,
		boolean free,
		OffsetDateTime publishedAt
) {
}
