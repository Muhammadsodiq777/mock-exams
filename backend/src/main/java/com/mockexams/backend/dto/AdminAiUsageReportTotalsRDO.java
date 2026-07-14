package com.mockexams.backend.dto;

import java.math.BigDecimal;

public record AdminAiUsageReportTotalsRDO(
		long totalRequests,
		long completedRequests,
		long failedRequests,
		long validationFailedRequests,
		long blockedRequests,
		long promptTokens,
		long completionTokens,
		BigDecimal estimatedCost
) {
	public AdminAiUsageReportTotalsRDO {
		if (estimatedCost == null) {
			estimatedCost = BigDecimal.ZERO;
		}
	}
}
