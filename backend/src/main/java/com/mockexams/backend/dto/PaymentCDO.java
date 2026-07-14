package com.mockexams.backend.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PaymentCDO(
		@NotBlank String targetType,
		@Min(1) Long listingId,
		@Min(1) Long subscriptionId,
		@Min(1) Long liveSpeakingBookingId,
		@Size(max = 60) String promoCode,
		@Size(max = 500) String returnUrl
) {
}
