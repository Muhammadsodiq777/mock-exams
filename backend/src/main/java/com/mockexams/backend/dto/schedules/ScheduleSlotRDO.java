package com.mockexams.backend.dto.schedules;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record ScheduleSlotRDO(
		Long id,
		Long teacherProfileId,
		String teacherDisplayName,
		String teacherHeadline,
		OffsetDateTime startsAt,
		OffsetDateTime endsAt,
		String timezone,
		BigDecimal priceAmount,
		String currency,
		String status,
		boolean announced,
		OffsetDateTime holdExpiresAt,
		OffsetDateTime createdAt,
		OffsetDateTime updatedAt
) {
}
