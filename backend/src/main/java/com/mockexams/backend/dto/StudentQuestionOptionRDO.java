package com.mockexams.backend.dto;

public record StudentQuestionOptionRDO(
		Long id,
		String optionLabel,
		String optionText,
		RichTextContentRDO optionTextContent,
		Integer optionOrder
) {
}
