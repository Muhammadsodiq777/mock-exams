package com.mockexams.backend.dto;

import java.time.OffsetDateTime;

public record PaymentDisputeRDO(
		Long id,
		Long paymentId,
		String paymentStatus,
		String paymentTargetType,
		Long paymentTargetLiveSpeakingBookingId,
		String status,
		String reason,
		String description,
		String resolutionNote,
		Long reviewedByUserId,
		OffsetDateTime reviewedAt,
		OffsetDateTime createdAt,
		OffsetDateTime updatedAt
) {
}
