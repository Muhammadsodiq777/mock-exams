package com.mockexams.backend.dto;

import java.time.OffsetDateTime;
import java.util.Map;

public record NotificationRDO(
		Long id,
		String type,
		String title,
		String body,
		Map<String, Object> payload,
		OffsetDateTime readAt,
		OffsetDateTime createdAt
) {
}
