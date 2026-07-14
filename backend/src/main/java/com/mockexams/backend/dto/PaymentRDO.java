package com.mockexams.backend.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record PaymentRDO(
		Long id,
		Long studentUserId,
		String provider,
		String providerPaymentId,
		String providerCheckoutUrl,
		String status,
		BigDecimal amount,
		String currency,
		String targetType,
		Long targetListingId,
		String targetListingTitle,
		Long targetSubscriptionId,
		Long targetLiveSpeakingBookingId,
		String promoCode,
		BigDecimal promoOriginalAmount,
		BigDecimal promoDiscountAmount,
		BigDecimal providerAmount,
		String providerCurrency,
		String failureCode,
		String failureMessage,
		OffsetDateTime createdAt,
		OffsetDateTime paidAt,
		OffsetDateTime confirmationDispatchedAt,
		OffsetDateTime updatedAt
) {
}
