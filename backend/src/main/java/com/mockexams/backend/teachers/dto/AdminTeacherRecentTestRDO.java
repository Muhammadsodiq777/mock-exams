package com.mockexams.backend.teachers.dto;

import java.time.OffsetDateTime;

public record AdminTeacherRecentTestRDO(
		Long id,
		String title,
		String visibility,
		OffsetDateTime updatedAt
) {
}
