package com.mockexams.backend.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public record PromoCodeUDO(
		@DecimalMin(value = "0.01") @DecimalMax(value = "100.00") BigDecimal discountPercent,
		@DecimalMin(value = "0.01") BigDecimal discountAmount,
		@Size(min = 3, max = 3) String currency,
		OffsetDateTime startsAt,
		OffsetDateTime expiresAt,
		@Min(1) Integer usageLimit,
		Boolean isActive
) {
}
