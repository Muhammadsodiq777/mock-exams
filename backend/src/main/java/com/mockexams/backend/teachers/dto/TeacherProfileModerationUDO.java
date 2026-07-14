package com.mockexams.backend.teachers.dto;

import jakarta.validation.constraints.Size;

public record TeacherProfileModerationUDO(
		@Size(max = 4000) String rejectionReason
) {
}
