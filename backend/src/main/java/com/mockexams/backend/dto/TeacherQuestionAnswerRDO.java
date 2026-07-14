package com.mockexams.backend.dto;

public record TeacherQuestionAnswerRDO(
		Long id,
		String answerValue,
		String normalizedValue,
		Boolean primary,
		Integer answerOrder,
		String explanation,
		RichTextContentRDO explanationContent
) {
}
