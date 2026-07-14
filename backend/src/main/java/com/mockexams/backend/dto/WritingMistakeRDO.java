package com.mockexams.backend.dto;

public record WritingMistakeRDO(
		String type,
		String original,
		String correction,
		String explanation
) {
}
