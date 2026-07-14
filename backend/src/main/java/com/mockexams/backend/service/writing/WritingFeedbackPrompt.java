package com.mockexams.backend.service.writing;

import java.util.Map;

import com.mockexams.backend.entity.WritingSubmission;

public record WritingFeedbackPrompt(
		WritingSubmission submission,
		String rubricVersion,
		String systemPrompt,
		String userPrompt,
		Map<String, Object> audit
) {
	public String combinedPrompt() {
		return systemPrompt + "\n\n" + userPrompt;
	}
}
