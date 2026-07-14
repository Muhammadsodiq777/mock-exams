package com.mockexams.backend.service;

import java.text.Normalizer;
import java.util.Locale;

import org.springframework.stereotype.Service;

@Service
public class AnswerNormalizationService {

	public String normalize(String value) {
		if (value == null) {
			return null;
		}
		String normalized = Normalizer.normalize(value, Normalizer.Form.NFKC)
				.toLowerCase(Locale.ROOT)
				.replaceAll("\\p{Punct}+", " ")
				.replaceAll("\\s+", " ")
				.trim();
		return normalized.isBlank() ? null : normalized;
	}
}
