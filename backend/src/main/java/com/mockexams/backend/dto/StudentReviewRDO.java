package com.mockexams.backend.dto;

import java.time.OffsetDateTime;

public record StudentReviewRDO(
		Long id,
		Long testId,
		Integer rating,
		String comment,
		OffsetDateTime createdAt,
		OffsetDateTime updatedAt
) {
}
