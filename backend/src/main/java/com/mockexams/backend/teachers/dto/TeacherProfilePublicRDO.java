package com.mockexams.backend.teachers.dto;

import java.math.BigDecimal;

public record TeacherProfilePublicRDO(
		Long id,
		String displayName,
		String headline,
		String bio,
		Long avatarFileId,
		String status,
		BigDecimal ratingAverage,
		Integer ratingCount
) {
}
