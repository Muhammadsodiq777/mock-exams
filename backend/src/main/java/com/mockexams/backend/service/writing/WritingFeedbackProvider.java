package com.mockexams.backend.service.writing;

public interface WritingFeedbackProvider {

	WritingFeedbackProviderResponse evaluate(WritingFeedbackPrompt prompt);
}
