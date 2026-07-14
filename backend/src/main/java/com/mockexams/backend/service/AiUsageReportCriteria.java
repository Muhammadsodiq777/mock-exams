package com.mockexams.backend.service;

import java.time.OffsetDateTime;

public record AiUsageReportCriteria(
		Long studentUserId,
		String feedbackType,
		String status,
		String provider,
		OffsetDateTime from,
		OffsetDateTime to
) {
}
