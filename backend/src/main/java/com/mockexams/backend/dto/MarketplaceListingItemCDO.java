package com.mockexams.backend.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record MarketplaceListingItemCDO(
		Long testId,
		Long fileId,
		@Size(max = 180) String title,
		@Size(max = 5000) String description,
		@NotNull @Min(1) Integer itemOrder
) {
}
