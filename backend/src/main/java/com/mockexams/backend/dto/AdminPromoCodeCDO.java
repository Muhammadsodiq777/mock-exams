package com.mockexams.backend.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AdminPromoCodeCDO(
		@Min(1) Long teacherProfileId,
		@NotBlank @Size(min = 3, max = 60) @Pattern(regexp = "[A-Za-z0-9_-]+",
				message = "Promo code may contain letters, digits, dashes, and underscores only.") String code,
		@DecimalMin(value = "0.01") @DecimalMax(value = "100.00") BigDecimal discountPercent,
		@DecimalMin(value = "0.01") BigDecimal discountAmount,
		@Size(min = 3, max = 3) String currency,
		OffsetDateTime startsAt,
		OffsetDateTime expiresAt,
		@Min(1) Integer usageLimit,
		Boolean isActive
) {
}
