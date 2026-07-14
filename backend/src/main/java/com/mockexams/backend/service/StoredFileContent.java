package com.mockexams.backend.service;

public record StoredFileContent(
		String filename,
		String contentType,
		byte[] bytes
) {
}
