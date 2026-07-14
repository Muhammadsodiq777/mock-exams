package com.mockexams.backend.dto;

import java.time.OffsetDateTime;

public record TeacherReviewRDO(
		Long id,
		Long testId,
		String testTitle,
		Integer rating,
		String comment,
		String studentDisplayName,
		OffsetDateTime createdAt
) {
}
