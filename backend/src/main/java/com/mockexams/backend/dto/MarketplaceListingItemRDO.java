package com.mockexams.backend.dto;

public record MarketplaceListingItemRDO(
		Long id,
		Long testId,
		Long fileId,
		String title,
		String description,
		Integer itemOrder
) {
}
