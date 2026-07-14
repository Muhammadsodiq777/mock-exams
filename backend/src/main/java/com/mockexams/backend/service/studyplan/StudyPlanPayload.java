package com.mockexams.backend.service.studyplan;

import java.util.List;
import java.util.Map;

public record StudyPlanPayload(
		Map<String, Object> summary,
		List<StudyPlanTaskPayload> tasks
) {
	public StudyPlanPayload {
		summary = summary == null ? Map.of() : Map.copyOf(summary);
		tasks = tasks == null ? List.of() : List.copyOf(tasks);
	}
}
