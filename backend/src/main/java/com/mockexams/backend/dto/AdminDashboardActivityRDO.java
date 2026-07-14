package com.mockexams.backend.dto;

import java.time.OffsetDateTime;

public record AdminDashboardActivityRDO(
		String type,
		Long referenceId,
		String summary,
		OffsetDateTime occurredAt
) {
}
