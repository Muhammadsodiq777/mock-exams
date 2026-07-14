package com.mockexams.backend.dto;

import java.time.OffsetDateTime;

public record MockExamRDO(
		Long id,
		String title,
		String subject,
		Integer durationMinutes,
		Integer questionCount,
		OffsetDateTime createdAt) {
}
