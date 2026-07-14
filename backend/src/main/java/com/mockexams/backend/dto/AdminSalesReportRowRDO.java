package com.mockexams.backend.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record AdminSalesReportRowRDO(
		LocalDate activityDate,
		Long teacherProfileId,
		Long listingId,
		String targetType,
		String provider,
		String status,
		String currency,
		long totalPayments,
		BigDecimal grossAmount,
		BigDecimal paidGrossAmount,
		BigDecimal failedGrossAmount,
		BigDecimal refundedGrossAmount,
		long disputeCount
) {
	public AdminSalesReportRowRDO {
		grossAmount = zeroIfNull(grossAmount);
		paidGrossAmount = zeroIfNull(paidGrossAmount);
		failedGrossAmount = zeroIfNull(failedGrossAmount);
		refundedGrossAmount = zeroIfNull(refundedGrossAmount);
	}

	private static BigDecimal zeroIfNull(BigDecimal value) {
		return value == null ? BigDecimal.ZERO : value;
	}
}
