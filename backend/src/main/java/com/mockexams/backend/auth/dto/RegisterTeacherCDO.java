package com.mockexams.backend.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterTeacherCDO(
		@NotBlank @Email @Size(max = 255) String email,
		@NotBlank @Size(min = 8, max = 120) String password,
		@NotBlank @Size(max = 160) String fullName,
		@Size(max = 40) String phone,
		@Size(max = 20) String preferredLanguage,
		@NotBlank @Size(max = 160) String displayName,
		@Size(max = 180) String headline,
		@Size(max = 4000) String bio
) {
}
