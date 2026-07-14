package com.mockexams.backend.dto;

import java.time.OffsetDateTime;

public record AccessStatusRDO(
		Long listingId,
		Boolean accessible,
		String status,
		String reasonCode,
		Long purchaseId,
		OffsetDateTime accessExpiresAt
) {
}
