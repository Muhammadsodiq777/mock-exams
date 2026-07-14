package com.mockexams.backend.dto;

import java.time.LocalDate;
import java.time.OffsetDateTime;

import com.fasterxml.jackson.annotation.JsonInclude;

public record StudyPlanTaskRDO(
		Long id,
		LocalDate taskDate,
		int taskOrder,
		String skill,
		String title,
		String description,
		int estimatedMinutes,
		StudyPlanResourceRDO recommendedResource,
		String rationale,
		boolean completed,
		@JsonInclude(JsonInclude.Include.NON_NULL) OffsetDateTime completedAt
) {
}
