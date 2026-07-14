package com.mockexams.backend.service.speaking;

import java.util.Map;

import com.mockexams.backend.entity.SpeakingSubmission;

public record SpeakingFeedbackPrompt(
		SpeakingSubmission submission,
		String rubricVersion,
		String systemPrompt,
		String userPrompt,
		Map<String, Object> audit
) {
	public String combinedPrompt() {
		return systemPrompt + "\n\n" + userPrompt;
	}
}
