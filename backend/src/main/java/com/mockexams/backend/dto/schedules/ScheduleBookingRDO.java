package com.mockexams.backend.dto.schedules;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record ScheduleBookingRDO(
		Long id,
		Long slotId,
		Long studentUserId,
		String studentDisplayName,
		Long teacherProfileId,
		String teacherDisplayName,
		String status,
		OffsetDateTime startsAt,
		OffsetDateTime endsAt,
		String timezone,
		BigDecimal priceAmount,
		String currency,
		Long paymentId,
		String paymentStatus,
		String meetUrl,
		OffsetDateTime holdExpiresAt,
		String providerSyncStatus,
		String providerFailureCode,
		String providerFailureMessage,
		OffsetDateTime createdAt,
		OffsetDateTime updatedAt
) {
}
