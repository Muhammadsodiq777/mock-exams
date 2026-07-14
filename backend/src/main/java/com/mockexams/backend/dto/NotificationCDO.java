package com.mockexams.backend.dto;

import java.util.LinkedHashMap;
import java.util.Map;

public record NotificationCDO(
		Long userId,
		String type,
		String title,
		String body,
		Map<String, Object> payload
) {

	public NotificationCDO {
		if (payload == null || payload.isEmpty()) {
			payload = Map.of();
		}
		else {
			payload = new LinkedHashMap<>(payload);
		}
	}
}
