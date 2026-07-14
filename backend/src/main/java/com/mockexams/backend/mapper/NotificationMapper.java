package com.mockexams.backend.mapper;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.mockexams.backend.dto.NotificationRDO;
import com.mockexams.backend.entity.Notification;

@Component
public class NotificationMapper {

	public NotificationRDO toRDO(Notification notification) {
		return new NotificationRDO(
				notification.getId(),
				notification.getType(),
				notification.getTitle(),
				notification.getBody(),
				copyPayload(notification.getPayload()),
				notification.getReadAt(),
				notification.getCreatedAt());
	}

	private Map<String, Object> copyPayload(Map<String, Object> payload) {
		if (payload == null || payload.isEmpty()) {
			return Map.of();
		}
		return new LinkedHashMap<>(payload);
	}
}
