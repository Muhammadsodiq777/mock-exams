package com.mockexams.backend.service;

import java.math.BigDecimal;

public record PromoApplication(
		Long promoCodeId,
		String displayCode,
		BigDecimal originalAmount,
		BigDecimal discountAmount,
		BigDecimal finalAmount
) {
}
