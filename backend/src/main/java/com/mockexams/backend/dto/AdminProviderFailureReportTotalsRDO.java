package com.mockexams.backend.dto;

import java.math.BigDecimal;

public record AdminProviderFailureReportTotalsRDO(
		long failureCount,
		long paymentFailures,
		long aiFailures,
		long amountMismatchPayments,
		long validationFailures,
		long providerFailures,
		BigDecimal paymentAmountTotal,
		BigDecimal aiEstimatedCostTotal
) {
	public AdminProviderFailureReportTotalsRDO {
		paymentAmountTotal = paymentAmountTotal == null ? BigDecimal.ZERO : paymentAmountTotal;
		aiEstimatedCostTotal = aiEstimatedCostTotal == null ? BigDecimal.ZERO : aiEstimatedCostTotal;
	}
}
