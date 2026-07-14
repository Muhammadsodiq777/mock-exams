package com.mockexams.backend.teachers.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import com.fasterxml.jackson.annotation.JsonInclude;

public record TeacherProfileRDO(
		Long id,
		Long userId,
		String displayName,
		String headline,
		String bio,
		Long avatarFileId,
		String status,
		@JsonInclude(JsonInclude.Include.NON_NULL)
		Long approvedByUserId,
		@JsonInclude(JsonInclude.Include.NON_NULL)
		OffsetDateTime approvedAt,
		@JsonInclude(JsonInclude.Include.NON_NULL)
		String rejectionReason,
		BigDecimal ratingAverage,
		Integer ratingCount,
		OffsetDateTime createdAt,
		OffsetDateTime updatedAt
) {
}
