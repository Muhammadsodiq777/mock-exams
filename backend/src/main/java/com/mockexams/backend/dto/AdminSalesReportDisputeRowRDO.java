package com.mockexams.backend.dto;

import java.time.LocalDate;

public record AdminSalesReportDisputeRowRDO(
		LocalDate activityDate,
		Long teacherProfileId,
		Long listingId,
		String targetType,
		String provider,
		String status,
		String currency,
		long disputeCount
) {
}
