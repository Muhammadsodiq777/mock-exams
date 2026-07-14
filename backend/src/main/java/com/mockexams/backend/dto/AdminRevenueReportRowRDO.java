package com.mockexams.backend.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record AdminRevenueReportRowRDO(
		LocalDate activityDate,
		Long teacherProfileId,
		Long listingId,
		String transactionType,
		String sourceType,
		String currency,
		long transactionCount,
		BigDecimal grossTotal,
		BigDecimal platformCommissionTotal,
		BigDecimal netTotal,
		long refundCount
) {
	public AdminRevenueReportRowRDO {
		grossTotal = zeroIfNull(grossTotal);
		platformCommissionTotal = zeroIfNull(platformCommissionTotal);
		netTotal = zeroIfNull(netTotal);
	}

	private static BigDecimal zeroIfNull(BigDecimal value) {
		return value == null ? BigDecimal.ZERO : value;
	}
}
