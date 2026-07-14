package com.mockexams.backend.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

public record StudyPlanResourceRDO(
		String type,
		@JsonInclude(JsonInclude.Include.NON_NULL) Long id,
		@JsonInclude(JsonInclude.Include.NON_NULL) String title
) {
}
