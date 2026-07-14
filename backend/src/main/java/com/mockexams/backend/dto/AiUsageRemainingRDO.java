package com.mockexams.backend.dto;

import java.time.LocalDate;

public record AiUsageRemainingRDO(
		LocalDate periodStart,
		LocalDate periodEnd,
		int freeRequestsLimit,
		int usedRequests,
		Integer hardLimitRequests,
		int freeRequestsRemaining,
		Integer hardRequestsRemaining,
		boolean hardLimitReached
) {
}
