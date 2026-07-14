package com.mockexams.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.mockexams.backend.service.AiUsageService;
import com.mockexams.backend.service.writing.MockWritingFeedbackProvider;
import com.mockexams.backend.service.writing.OpenAiWritingFeedbackProvider;
import com.mockexams.backend.service.writing.WritingFeedbackProvider;

@Configuration
class WritingFeedbackProviderConfig {

	@Bean
	WritingFeedbackProvider writingFeedbackProvider(
			BackendEnvironmentProperties properties,
			AiUsageService aiUsageService
	) {
		if ("openai".equalsIgnoreCase(properties.ai().writing().provider())) {
			return new OpenAiWritingFeedbackProvider(properties, aiUsageService);
		}
		return new MockWritingFeedbackProvider();
	}
}
