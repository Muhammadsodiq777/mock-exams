package com.mockexams.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PaymentDisputeDecisionCDO(
		@NotBlank String status,
		@Size(max = 2000) String resolutionNote
) {
}
