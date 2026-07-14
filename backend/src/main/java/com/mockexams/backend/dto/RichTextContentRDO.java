package com.mockexams.backend.dto;

import com.fasterxml.jackson.databind.JsonNode;

public record RichTextContentRDO(
		JsonNode contentDocument,
		String contentText,
		String renderedHtml
) {
}
