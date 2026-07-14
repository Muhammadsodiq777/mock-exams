package com.mockexams.backend.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record PaymentWebhookCDO(
		@Size(max = 160) String eventId,
		@NotBlank @Size(max = 160) String providerPaymentId,
		@NotBlank @Size(max = 24) String status,
		@NotNull @DecimalMin("0.00") BigDecimal amount,
		@NotBlank @Size(min = 3, max = 3) String currency,
		OffsetDateTime occurredAt,
		@Size(max = 80) String failureCode,
		@Size(max = 500) String failureMessage
) {
}
