package com.mockexams.backend.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record TeacherListeningSegmentCDO(
		@Size(max = 160) String title,
		@NotNull @Min(0) Integer startSeconds,
		@NotNull @Min(0) Integer endSeconds,
		String transcript
) {
}
