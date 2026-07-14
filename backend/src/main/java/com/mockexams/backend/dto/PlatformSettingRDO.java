package com.mockexams.backend.dto;

import java.time.OffsetDateTime;

import com.fasterxml.jackson.databind.JsonNode;

public record PlatformSettingRDO(
		String key,
		JsonNode value,
		Long updatedByUserId,
		OffsetDateTime updatedAt
) {
}
