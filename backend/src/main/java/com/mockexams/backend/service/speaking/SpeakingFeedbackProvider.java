package com.mockexams.backend.service.speaking;

public interface SpeakingFeedbackProvider {

	SpeakingFeedbackProviderResponse evaluate(SpeakingFeedbackPrompt prompt);
}
