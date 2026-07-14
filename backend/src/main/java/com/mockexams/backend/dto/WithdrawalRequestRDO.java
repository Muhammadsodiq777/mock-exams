package com.mockexams.backend.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Map;

public record WithdrawalRequestRDO(
		Long id,
		Long teacherProfileId,
		BigDecimal requestedAmount,
		String currency,
		String status,
		Map<String, Object> payoutDetails,
		Long reviewedByUserId,
		OffsetDateTime reviewedAt,
		String rejectionReason,
		OffsetDateTime paidAt,
		OffsetDateTime createdAt,
		OffsetDateTime updatedAt
) {
}
