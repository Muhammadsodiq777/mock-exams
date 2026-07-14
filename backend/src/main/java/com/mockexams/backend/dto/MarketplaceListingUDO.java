package com.mockexams.backend.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;

public record MarketplaceListingUDO(
		@Size(max = 180) String title,
		@Size(max = 5000) String description,
		@Size(max = 40) String listingType,
		Long testId,
		@DecimalMin("0.00") BigDecimal priceAmount,
		@Size(min = 3, max = 3) String currency,
		@Size(max = 20) String language
) {
}
