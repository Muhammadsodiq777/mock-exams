package com.mockexams.backend.dto;

import java.time.OffsetDateTime;

public record PublicReviewRDO(
		Long id,
		Long testId,
		String testTitle,
		Long teacherProfileId,
		Integer rating,
		String comment,
		String studentDisplayName,
		OffsetDateTime createdAt
) {
}
