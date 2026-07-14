package com.mockexams.backend.service;

import java.time.OffsetDateTime;

public record AdminReviewSearchCriteria(
		Long teacherProfileId,
		Long testId,
		Integer rating,
		Boolean hidden,
		OffsetDateTime from,
		OffsetDateTime to
) {
}
