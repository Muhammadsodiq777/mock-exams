package com.mockexams.backend.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record AdminProviderFailureReportRowRDO(
		LocalDate activityDate,
		String sourceType,
		String provider,
		String status,
		String failureCode,
		String model,
		String targetType,
		String currency,
		long failureCount,
		BigDecimal paymentAmountTotal,
		BigDecimal aiEstimatedCostTotal
) {
	public AdminProviderFailureReportRowRDO(
			LocalDate activityDate,
			String sourceType,
			String provider,
			String status,
			String failureCode,
			String model,
			String targetType,
			String currency,
			long failureCount,
			BigDecimal paymentAmountTotal,
			Integer aiEstimatedCostTotal
	) {
		this(activityDate, sourceType, provider, status, failureCode, model, targetType, currency, failureCount,
				paymentAmountTotal, BigDecimal.valueOf(aiEstimatedCostTotal == null ? 0 : aiEstimatedCostTotal));
	}

	public AdminProviderFailureReportRowRDO(
			LocalDate activityDate,
			String sourceType,
			String provider,
			String status,
			String failureCode,
			String model,
			String targetType,
			String currency,
			long failureCount,
			Integer paymentAmountTotal,
			BigDecimal aiEstimatedCostTotal
	) {
		this(activityDate, sourceType, provider, status, failureCode, model, targetType, currency, failureCount,
				BigDecimal.valueOf(paymentAmountTotal == null ? 0 : paymentAmountTotal), aiEstimatedCostTotal);
	}

	public AdminProviderFailureReportRowRDO {
		paymentAmountTotal = paymentAmountTotal == null ? BigDecimal.ZERO : paymentAmountTotal;
		aiEstimatedCostTotal = aiEstimatedCostTotal == null ? BigDecimal.ZERO : aiEstimatedCostTotal;
	}
}
