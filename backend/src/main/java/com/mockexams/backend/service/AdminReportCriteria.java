package com.mockexams.backend.service;

import java.time.OffsetDateTime;

public record AdminReportCriteria(
		OffsetDateTime from,
		OffsetDateTime to,
		Long studentUserId,
		Long teacherProfileId,
		Long listingId,
		String testType,
		String feedbackType,
		String status,
		String model,
		String provider,
		String paymentStatus
) {
}
