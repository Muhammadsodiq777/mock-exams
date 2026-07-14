package com.mockexams.backend.dto.schedules;

import java.time.OffsetDateTime;

public record GoogleCalendarProviderFailureRDO(
		Long id,
		Long bookingId,
		Long slotId,
		Long teacherProfileId,
		String teacherDisplayName,
		Long studentUserId,
		String studentDisplayName,
		String provider,
		String providerSyncStatus,
		String operation,
		String status,
		String failureCode,
		String failureMessage,
		Integer retryCount,
		OffsetDateTime startsAt,
		OffsetDateTime createdAt,
		OffsetDateTime updatedAt
) {
}
