package com.mockexams.backend.service;

import java.math.BigDecimal;
import java.util.List;

public record MarketplaceSearchCriteria(
		String listingType,
		Long teacherId,
		String price,
		BigDecimal minPrice,
		BigDecimal maxPrice,
		List<String> tags,
		List<String> categories,
		String language,
		String query
) {

	public MarketplaceSearchCriteria {
		tags = tags == null ? List.of() : List.copyOf(tags);
		categories = categories == null ? List.of() : List.copyOf(categories);
	}
}
