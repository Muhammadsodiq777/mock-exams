package com.mockexams.backend.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;

public record StudyPlanRDO(
		Long id,
		Long studentUserId,
		BigDecimal targetBand,
		@JsonInclude(JsonInclude.Include.NON_NULL) BigDecimal currentBand,
		int durationDays,
		List<String> weakestSkills,
		Map<String, Object> planSummary,
		LocalDate startsAt,
		LocalDate endsAt,
		Long createdByFeedbackId,
		List<StudyPlanTaskRDO> tasks,
		OffsetDateTime createdAt,
		OffsetDateTime updatedAt
) {
	public StudyPlanRDO {
		weakestSkills = weakestSkills == null ? List.of() : List.copyOf(weakestSkills);
		planSummary = planSummary == null ? Map.of() : Map.copyOf(planSummary);
		tasks = tasks == null ? List.of() : List.copyOf(tasks);
	}
}
