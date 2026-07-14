package com.mockexams.backend.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

public record MarketplaceListingDetailRDO(
		Long id,
		String title,
		String description,
		String listingType,
		Long testId,
		BigDecimal priceAmount,
		String currency,
		Boolean free,
		Boolean active,
		String language,
		OffsetDateTime createdAt,
		OffsetDateTime updatedAt,
		TestTeacherRDO teacher,
		List<TestCategoryRDO> categories,
		List<TestTagRDO> tags,
		List<MarketplaceListingItemRDO> items,
		AccessStatusRDO accessStatus
) {
}
