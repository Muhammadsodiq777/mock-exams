package com.mockexams.backend.dto;

public record AdminTestActivityReportTotalsRDO(
		long totalAttempts,
		long submittedAttempts,
		long expiredAttempts,
		long inProgressAttempts,
		long totalTimeSpentSeconds,
		long averageTimeSpentSeconds
) {
}
