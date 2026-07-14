package com.mockexams.backend.dto;

public record SpeakingMistakeRDO(
		String type,
		String original,
		String correction,
		String explanation
) {
}
