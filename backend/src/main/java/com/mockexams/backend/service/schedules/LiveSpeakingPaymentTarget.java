package com.mockexams.backend.service.schedules;

import java.math.BigDecimal;

public record LiveSpeakingPaymentTarget(
		Long bookingId,
		BigDecimal amount,
		String currency,
		String description
) {
}
