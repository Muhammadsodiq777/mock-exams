package com.mockexams.backend.service;

import java.time.OffsetDateTime;

public record ReviewSearchCriteria(
		Long testId,
		Integer rating,
		OffsetDateTime from,
		OffsetDateTime to
) {
}
