package com.mockexams.backend.dto;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public record TeacherTestSectionUDO(
		@Size(max = 180) String title,
		String instructions,
		JsonNode instructionsDocument,
		String passageText,
		JsonNode passageDocument,
		Long audioFileId,
		@Min(1) Integer durationMinutes
) {
}
