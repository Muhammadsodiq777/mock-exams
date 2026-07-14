package com.mockexams.backend.service;

public record TranscriptionRequest(
		String filename,
		String contentType,
		byte[] audioBytes,
		String prompt
) {
}
