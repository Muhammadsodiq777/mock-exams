package com.mockexams.backend.teachers.dto;

import jakarta.validation.constraints.Size;

public record TeacherProfileSuspensionUDO(
		@Size(max = 4000) String reason
) {
}
