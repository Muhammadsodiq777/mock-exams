package com.mockexams.backend.teachers.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TeacherProfileUDO(
		@NotBlank @Size(max = 160) String displayName,
		@Size(max = 180) String headline,
		@Size(max = 4000) String bio,
		Long avatarFileId
) {
}
