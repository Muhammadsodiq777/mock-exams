package com.mockexams.backend.service;

import java.math.BigDecimal;
import java.util.List;

public record TestCatalogSearchCriteria(
		String type,
		String difficulty,
		BigDecimal bandMin,
		BigDecimal bandMax,
		List<String> tags,
		List<String> categories,
		String language,
		String price,
		Long teacherId
) {

	public TestCatalogSearchCriteria {
		tags = tags == null ? List.of() : List.copyOf(tags);
		categories = categories == null ? List.of() : List.copyOf(categories);
	}
}
