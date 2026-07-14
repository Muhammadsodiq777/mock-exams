package com.mockexams.backend.dto;

import java.math.BigDecimal;

public record AdminDashboardWithdrawalsRDO(
		long total,
		long requested,
		long approved,
		long rejected,
		long paid,
		long cancelled,
		BigDecimal requestedAmountTotal
) {
	public AdminDashboardWithdrawalsRDO {
		if (requestedAmountTotal == null) {
			requestedAmountTotal = BigDecimal.ZERO;
		}
	}
}
