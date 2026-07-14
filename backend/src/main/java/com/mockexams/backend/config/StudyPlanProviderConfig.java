package com.mockexams.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.mockexams.backend.service.AiUsageService;
import com.mockexams.backend.service.studyplan.MockStudyPlanProvider;
import com.mockexams.backend.service.studyplan.OpenAiStudyPlanProvider;
import com.mockexams.backend.service.studyplan.StudyPlanProvider;

@Configuration
class StudyPlanProviderConfig {

	@Bean
	StudyPlanProvider studyPlanProvider(
			BackendEnvironmentProperties properties,
			AiUsageService aiUsageService
	) {
		if ("openai".equalsIgnoreCase(properties.ai().studyPlan().provider())) {
			return new OpenAiStudyPlanProvider(properties, aiUsageService);
		}
		return new MockStudyPlanProvider();
	}
}
