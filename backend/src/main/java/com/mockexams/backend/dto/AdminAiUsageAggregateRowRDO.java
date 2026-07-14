package com.mockexams.backend.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record AdminAiUsageAggregateRowRDO(
		LocalDate activityDate,
		Long studentUserId,
		String feedbackType,
		String status,
		String provider,
		String model,
		String failureCode,
		long requestCount,
		long validationFailedRequests,
		long blockedRequests,
		long promptTokens,
		long completionTokens,
		BigDecimal estimatedCost
) {
	public AdminAiUsageAggregateRowRDO {
		if (estimatedCost == null) {
			estimatedCost = BigDecimal.ZERO;
		}
	}
}
