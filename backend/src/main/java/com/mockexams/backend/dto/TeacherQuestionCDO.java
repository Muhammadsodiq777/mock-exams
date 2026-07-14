package com.mockexams.backend.dto;

import java.math.BigDecimal;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TeacherQuestionCDO(
		@NotBlank @Size(max = 40) String questionType,
		String questionText,
		JsonNode questionDocument,
		String instructions,
		JsonNode instructionsDocument,
		@DecimalMin("0.00") BigDecimal score,
		@Size(max = 20) String relatedParagraphLabel,
		Long listeningSegmentId,
		List<@Valid TeacherQuestionOptionCDO> options,
		List<@Valid TeacherQuestionAnswerCDO> answers
) {
}
