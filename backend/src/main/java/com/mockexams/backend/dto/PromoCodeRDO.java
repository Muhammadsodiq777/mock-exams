package com.mockexams.backend.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record PromoCodeRDO(
		Long id,
		Long teacherProfileId,
		String code,
		BigDecimal discountPercent,
		BigDecimal discountAmount,
		String currency,
		OffsetDateTime startsAt,
		OffsetDateTime expiresAt,
		Integer usageLimit,
		Integer usedCount,
		Boolean isActive,
		OffsetDateTime createdAt,
		OffsetDateTime updatedAt
) {
}
