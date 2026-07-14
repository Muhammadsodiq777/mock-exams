package com.mockexams.backend.service;

import com.fasterxml.jackson.databind.JsonNode;

public interface RichTextService {

	SanitizedRichText fromDocument(JsonNode document, boolean required, String fieldName);

	SanitizedRichText fromPlainText(String value, boolean required, String fieldName);

	SanitizedRichText fromStored(String document, String text, String renderedHtml, String legacyText);

	record SanitizedRichText(
			String document,
			String text,
			String renderedHtml
	) {

		public boolean isBlank() {
			return text == null || text.isBlank();
		}
	}
}
