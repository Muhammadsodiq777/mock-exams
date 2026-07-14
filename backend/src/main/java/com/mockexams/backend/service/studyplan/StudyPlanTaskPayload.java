package com.mockexams.backend.service.studyplan;

import java.time.LocalDate;

public record StudyPlanTaskPayload(
		LocalDate date,
		String skill,
		String title,
		String description,
		int estimatedMinutes,
		StudyPlanResourcePayload recommendedResource,
		String rationale
) {
}
