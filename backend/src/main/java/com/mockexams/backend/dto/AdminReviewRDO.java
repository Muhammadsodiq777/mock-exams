package com.mockexams.backend.dto;

import java.time.OffsetDateTime;

public record AdminReviewRDO(
		Long id,
		Long testId,
		String testTitle,
		Long teacherProfileId,
		Long studentUserId,
		String studentDisplayName,
		Integer rating,
		String comment,
		boolean hidden,
		OffsetDateTime hiddenAt,
		Long hiddenByUserId,
		OffsetDateTime createdAt,
		OffsetDateTime updatedAt
) {
}
