package com.mockexams.backend.service.studyplan;

public interface StudyPlanProvider {

	StudyPlanProviderResponse generate(StudyPlanPrompt prompt);
}
