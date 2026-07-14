package com.mockexams.backend.service;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record AccessGrantRequest(
		String sourceType,
		String sourceReference,
		String sourceMetadataJson,
		OffsetDateTime accessExpiresAt,
		Long paymentId,
		BigDecimal amountPaid
) {
	public AccessGrantRequest(
			String sourceType,
			String sourceReference,
			String sourceMetadataJson,
			OffsetDateTime accessExpiresAt
	) {
		this(sourceType, sourceReference, sourceMetadataJson, accessExpiresAt, null, null);
	}

	public AccessGrantRequest(
			String sourceType,
			String sourceReference,
			String sourceMetadataJson,
			OffsetDateTime accessExpiresAt,
			Long paymentId
	) {
		this(sourceType, sourceReference, sourceMetadataJson, accessExpiresAt, paymentId, null);
	}
}
