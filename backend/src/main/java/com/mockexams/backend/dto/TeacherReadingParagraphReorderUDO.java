package com.mockexams.backend.dto;

import java.util.List;

import jakarta.validation.constraints.NotEmpty;

public record TeacherReadingParagraphReorderUDO(
		@NotEmpty List<Long> paragraphIds
) {
}
