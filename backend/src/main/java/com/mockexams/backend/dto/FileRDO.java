package com.mockexams.backend.dto;

import java.time.OffsetDateTime;

public record FileRDO(
		Long id,
		Long ownerUserId,
		String purpose,
		String originalName,
		String contentType,
		long sizeBytes,
		Integer durationSeconds,
		String checksum,
		OffsetDateTime createdAt
) {
}
