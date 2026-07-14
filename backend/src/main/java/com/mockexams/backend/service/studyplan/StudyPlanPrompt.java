package com.mockexams.backend.service.studyplan;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public record StudyPlanPrompt(
		String systemPrompt,
		String userPrompt,
		String promptVersion,
		Map<String, Object> audit,
		LocalDate startsAt,
		int durationDays,
		List<String> missingSkills
) {
	public StudyPlanPrompt {
		audit = audit == null ? Map.of() : Map.copyOf(audit);
		missingSkills = missingSkills == null ? List.of() : List.copyOf(missingSkills);
	}
}
