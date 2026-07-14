package com.mockexams.backend.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record PurchaseRDO(
		Long id,
		Long studentUserId,
		Long listingId,
		String listingTitle,
		String listingType,
		BigDecimal amount,
		String currency,
		String status,
		String sourceType,
		String sourceReference,
		Long paymentId,
		String paymentStatus,
		OffsetDateTime purchasedAt,
		OffsetDateTime accessExpiresAt,
		AccessStatusRDO accessStatus
) {
}
