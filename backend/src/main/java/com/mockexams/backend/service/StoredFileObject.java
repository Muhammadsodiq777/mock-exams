package com.mockexams.backend.service;

public record StoredFileObject(
		String bucket,
		String objectKey,
		String checksum
) {
}
