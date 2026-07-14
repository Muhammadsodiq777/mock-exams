package com.mockexams.backend.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record MarketplaceListingCDO(
		@NotBlank @Size(max = 180) String title,
		@Size(max = 5000) String description,
		@NotBlank @Size(max = 40) String listingType,
		Long testId,
		@NotNull @DecimalMin("0.00") BigDecimal priceAmount,
		@NotBlank @Size(min = 3, max = 3) String currency,
		@NotBlank @Size(max = 20) String language
) {
}
