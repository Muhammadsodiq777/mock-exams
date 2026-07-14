package com.mockexams.backend.dto;

import com.fasterxml.jackson.databind.JsonNode;

public record TeacherReadingParagraphRDO(
		Long id,
		String label,
		String content,
		JsonNode contentDocument,
		String contentText,
		String renderedHtml,
		Integer paragraphOrder
) {
}
