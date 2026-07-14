package com.mockexams.backend.service.payments;

import java.math.BigDecimal;

public record PaymentProviderCreateRequest(
		Long paymentId,
		Long studentUserId,
		BigDecimal amount,
		String currency,
		String description,
		String returnUrl
) {
}
