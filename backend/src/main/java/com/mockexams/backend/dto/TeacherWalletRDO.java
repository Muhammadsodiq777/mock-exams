package com.mockexams.backend.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record TeacherWalletRDO(
		Long id,
		Long teacherProfileId,
		BigDecimal balanceAmount,
		BigDecimal pendingAmount,
		BigDecimal availableAmount,
		String currency,
		OffsetDateTime createdAt,
		OffsetDateTime updatedAt
) {
}
