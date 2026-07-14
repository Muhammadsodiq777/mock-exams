package com.mockexams.backend.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AdminUserStatusUDO(
		@NotBlank
		@Pattern(regexp = "active|suspended|banned|deleted")
		String status,
		@Size(max = 2000)
		String reason
) {
}
