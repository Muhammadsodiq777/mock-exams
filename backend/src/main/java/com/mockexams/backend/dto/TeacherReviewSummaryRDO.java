package com.mockexams.backend.dto;

import java.math.BigDecimal;

public record TeacherReviewSummaryRDO(
		BigDecimal ratingAverage,
		Integer ratingCount
) {
}
