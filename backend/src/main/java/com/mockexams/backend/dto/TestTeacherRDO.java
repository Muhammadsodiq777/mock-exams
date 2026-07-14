package com.mockexams.backend.dto;

import java.math.BigDecimal;

public record TestTeacherRDO(
		Long id,
		String displayName,
		String headline,
		BigDecimal ratingAverage,
		Integer ratingCount
) {
}
