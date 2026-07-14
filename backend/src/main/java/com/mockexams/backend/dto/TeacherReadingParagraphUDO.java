package com.mockexams.backend.dto;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.Size;

public record TeacherReadingParagraphUDO(
		@Size(max = 20) String label,
		String content,
		JsonNode contentDocument
) {
}
