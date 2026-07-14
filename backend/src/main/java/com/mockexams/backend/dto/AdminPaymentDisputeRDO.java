package com.mockexams.backend.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record AdminPaymentDisputeRDO(
		Long id,
		Long paymentId,
		String paymentStatus,
		BigDecimal paymentAmount,
		String paymentCurrency,
		String paymentTargetType,
		Long paymentTargetListingId,
		Long paymentTargetLiveSpeakingBookingId,
		Long studentUserId,
		String studentDisplayName,
		Long teacherProfileId,
		String teacherDisplayName,
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
