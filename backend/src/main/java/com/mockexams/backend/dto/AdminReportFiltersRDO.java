package com.mockexams.backend.dto;

import java.time.OffsetDateTime;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record AdminReportFiltersRDO(
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
