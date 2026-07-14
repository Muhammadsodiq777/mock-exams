package com.mockexams.backend.dto;

import java.math.BigDecimal;

public record PromoQuoteRDO(
		Long listingId,
		String code,
		boolean valid,
		String reasonCode,
		BigDecimal originalAmount,
		BigDecimal discountAmount,
		BigDecimal finalAmount,
		String currency
) {
}
