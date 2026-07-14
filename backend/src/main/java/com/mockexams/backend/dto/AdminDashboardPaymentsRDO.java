package com.mockexams.backend.dto;

import java.math.BigDecimal;

public record AdminDashboardPaymentsRDO(
		long total,
		long pending,
		long paid,
		long failed,
		long cancelled,
		long refunded,
		long amountMismatch,
		BigDecimal paidAmountTotal
) {
	public AdminDashboardPaymentsRDO {
		if (paidAmountTotal == null) {
			paidAmountTotal = BigDecimal.ZERO;
		}
	}
}
