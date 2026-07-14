package com.mockexams.backend.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record PaymentDisputeCDO(
		@NotNull @Min(1) Long paymentId,
		@NotBlank @Size(max = 160) String reason,
		@Size(max = 2000) String description
) {
}
