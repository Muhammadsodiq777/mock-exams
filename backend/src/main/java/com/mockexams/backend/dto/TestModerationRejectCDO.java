package com.mockexams.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TestModerationRejectCDO(
		@NotBlank(message = "Rejection reason is required.")
		@Size(max = 1000, message = "Rejection reason must be at most 1000 characters.")
		String reason
) {
}
