package com.mockexams.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.mockexams.backend.service.AiUsageService;
import com.mockexams.backend.service.speaking.MockSpeakingFeedbackProvider;
import com.mockexams.backend.service.speaking.OpenAiSpeakingFeedbackProvider;
import com.mockexams.backend.service.speaking.SpeakingFeedbackProvider;

@Configuration
class SpeakingFeedbackProviderConfig {

	@Bean
	SpeakingFeedbackProvider speakingFeedbackProvider(
			BackendEnvironmentProperties properties,
			AiUsageService aiUsageService
	) {
		if ("openai".equalsIgnoreCase(properties.ai().speaking().provider())) {
			return new OpenAiSpeakingFeedbackProvider(properties, aiUsageService);
		}
		return new MockSpeakingFeedbackProvider();
	}
}
