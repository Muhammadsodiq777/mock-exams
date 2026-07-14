package com.mockexams.backend.service.studyplan;

public record StudyPlanResourcePayload(
		String type,
		Long id,
		String title
) {
}
