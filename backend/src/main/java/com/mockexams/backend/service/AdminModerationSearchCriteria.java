package com.mockexams.backend.service;

import java.time.OffsetDateTime;

public record AdminModerationSearchCriteria(
		String type,
		Long teacherProfileId,
		String difficulty,
		OffsetDateTime submittedFrom,
		OffsetDateTime submittedTo,
		String search
) {
}
