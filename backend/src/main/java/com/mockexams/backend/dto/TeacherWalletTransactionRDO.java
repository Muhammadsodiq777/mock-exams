package com.mockexams.backend.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record TeacherWalletTransactionRDO(
		Long id,
		Long walletId,
		Long teacherProfileId,
		Long purchaseId,
		Long paymentId,
		Long withdrawalRequestId,
		String transactionType,
		String sourceType,
		String sourceReference,
		BigDecimal grossAmount,
		BigDecimal platformCommissionAmount,
		BigDecimal netAmount,
		String currency,
		String description,
		OffsetDateTime createdAt
) {
}
