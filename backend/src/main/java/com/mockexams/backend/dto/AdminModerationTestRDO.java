package com.mockexams.backend.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record AdminModerationTestRDO(
		Long id,
		Long teacherProfileId,
		String teacherDisplayName,
		String title,
		String type,
		String difficulty,
		Integer durationMinutes,
		BigDecimal priceAmount,
		String currency,
		Boolean free,
		String visibility,
		OffsetDateTime submittedAt,
		OffsetDateTime createdAt
) {
}
