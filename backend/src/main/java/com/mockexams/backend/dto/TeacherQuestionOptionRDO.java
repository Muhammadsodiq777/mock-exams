package com.mockexams.backend.dto;

public record TeacherQuestionOptionRDO(
		Long id,
		String optionLabel,
		String optionText,
		RichTextContentRDO optionTextContent,
		Integer optionOrder
) {
}
