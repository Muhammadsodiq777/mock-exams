package com.mockexams.backend.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record TeacherTestSummaryRDO(
		Long id,
		Long teacherProfileId,
		Long createdByUserId,
		String title,
		String description,
		String type,
		String difficulty,
		BigDecimal estimatedBandMin,
		BigDecimal estimatedBandMax,
		Integer durationMinutes,
		BigDecimal priceAmount,
		String currency,
		Boolean free,
		String language,
		String visibility,
		String moderationNote,
		OffsetDateTime publishedAt,
		OffsetDateTime createdAt,
		OffsetDateTime updatedAt
) {
}
