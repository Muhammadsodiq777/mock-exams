package com.mockexams.backend.service;

import java.time.OffsetDateTime;

public record AdminPaymentDisputeSearchCriteria(
		String status,
		String paymentStatus,
		OffsetDateTime from,
		OffsetDateTime to,
		Long teacherProfileId,
		Long studentUserId,
		String reason
) {
}
