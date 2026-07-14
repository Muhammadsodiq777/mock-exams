package com.mockexams.backend.dto;

import java.util.List;

public record MarketplaceListingActivationRDO(
		Long listingId,
		Boolean ready,
		List<String> errors
) {
}
