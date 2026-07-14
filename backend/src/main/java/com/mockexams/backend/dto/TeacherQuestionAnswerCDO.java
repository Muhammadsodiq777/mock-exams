package com.mockexams.backend.dto;

import com.fasterxml.jackson.databind.JsonNode;

public record TeacherQuestionAnswerCDO(
		String answerValue,
		Boolean primary,
		String explanation,
		JsonNode explanationDocument
) {
}
