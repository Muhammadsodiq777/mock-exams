package com.mockexams.backend.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

public record TestCardRDO(
		Long id,
		String title,
		String description,
		String type,
		String difficulty,
		BigDecimal estimatedBandMin,
		BigDecimal estimatedBandMax,
		Integer durationMinutes,
		BigDecimal priceAmount,
		String currency,
		Boolean free,
		String language,
		OffsetDateTime publishedAt,
		TestTeacherRDO teacher,
		List<TestCategoryRDO> categories,
		List<TestTagRDO> tags
) {
}
