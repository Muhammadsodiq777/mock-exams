package com.mockexams.backend.dto;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TeacherQuestionOptionCDO(
		@NotBlank @Size(max = 20) String optionLabel,
		String optionText,
		JsonNode optionDocument
) {
}
