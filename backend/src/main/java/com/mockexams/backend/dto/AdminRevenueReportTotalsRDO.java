package com.mockexams.backend.dto;

import java.math.BigDecimal;

public record AdminRevenueReportTotalsRDO(
		long transactionCount,
		BigDecimal grossTotal,
		BigDecimal platformCommissionTotal,
		BigDecimal netTotal,
		long refundCount
) {
	public AdminRevenueReportTotalsRDO {
		grossTotal = zeroIfNull(grossTotal);
		platformCommissionTotal = zeroIfNull(platformCommissionTotal);
		netTotal = zeroIfNull(netTotal);
	}

	private static BigDecimal zeroIfNull(BigDecimal value) {
		return value == null ? BigDecimal.ZERO : value;
	}
}
