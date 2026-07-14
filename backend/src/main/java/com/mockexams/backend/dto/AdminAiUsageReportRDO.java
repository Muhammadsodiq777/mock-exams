package com.mockexams.backend.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record AdminAiUsageReportRDO(
		Long id,
		Long studentUserId,
		String feedbackType,
		String status,
		String provider,
		String model,
		int promptTokens,
		int completionTokens,
		BigDecimal estimatedCost,
		String failureCode,
		String errorMessage,
		OffsetDateTime createdAt,
		OffsetDateTime completedAt
) {
}
