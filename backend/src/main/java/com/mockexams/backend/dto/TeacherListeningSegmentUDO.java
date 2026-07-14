package com.mockexams.backend.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public record TeacherListeningSegmentUDO(
		@Size(max = 160) String title,
		@Min(0) Integer startSeconds,
		@Min(0) Integer endSeconds,
		String transcript
) {
}
