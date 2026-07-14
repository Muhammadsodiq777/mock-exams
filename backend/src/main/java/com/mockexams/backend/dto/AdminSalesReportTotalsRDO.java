package com.mockexams.backend.dto;

import java.math.BigDecimal;

public record AdminSalesReportTotalsRDO(
		long totalPayments,
		long paidPayments,
		long failedPayments,
		long refundedPayments,
		BigDecimal grossAmount,
		BigDecimal paidGrossAmount,
		BigDecimal failedGrossAmount,
		BigDecimal refundedGrossAmount,
		BigDecimal marketplaceGrossAmount,
		BigDecimal liveSpeakingGrossAmount,
		long disputeCount
) {
	public AdminSalesReportTotalsRDO {
		grossAmount = zeroIfNull(grossAmount);
		paidGrossAmount = zeroIfNull(paidGrossAmount);
		failedGrossAmount = zeroIfNull(failedGrossAmount);
		refundedGrossAmount = zeroIfNull(refundedGrossAmount);
		marketplaceGrossAmount = zeroIfNull(marketplaceGrossAmount);
		liveSpeakingGrossAmount = zeroIfNull(liveSpeakingGrossAmount);
	}

	private static BigDecimal zeroIfNull(BigDecimal value) {
		return value == null ? BigDecimal.ZERO : value;
	}
}
