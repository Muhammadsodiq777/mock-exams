package com.mockexams.backend.dto;

import java.time.LocalDate;

public record AdminTestActivityReportRowRDO(
		LocalDate activityDate,
		Long teacherProfileId,
		Long testId,
		String testType,
		String status,
		long attemptCount,
		long submittedCount,
		long expiredCount,
		long inProgressCount,
		long totalTimeSpentSeconds,
		double averageTimeSpentSeconds
) {
}
