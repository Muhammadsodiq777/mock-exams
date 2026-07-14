package com.mockexams.backend.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginCDO(
		@NotBlank @Email @Size(max = 255) String email,
		@NotBlank @Size(max = 120) String password
) {
}
