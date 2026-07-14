package com.mockexams.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record WithdrawalRejectionCDO(
		@NotBlank(message = "Rejection reason is required.")
		@Size(max = 500, message = "Rejection reason must be at most 500 characters.")
		String reason
) {
}
