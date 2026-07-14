package com.mockexams.backend.dto;

import java.util.List;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public record TeacherTestSectionReorderUDO(
		@NotEmpty List<@NotNull @Min(1) Long> sectionIds
) {
}
