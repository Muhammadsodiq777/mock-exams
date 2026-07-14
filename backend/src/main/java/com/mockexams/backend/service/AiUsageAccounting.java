package com.mockexams.backend.service;

import java.math.BigDecimal;

public record AiUsageAccounting(
		String provider,
		String model,
		int promptTokens,
		int completionTokens,
		BigDecimal estimatedCost
) {
	public AiUsageAccounting {
		if (promptTokens < 0 || completionTokens < 0) {
			throw new IllegalArgumentException("AI token counts cannot be negative.");
		}
		if (estimatedCost != null && estimatedCost.signum() < 0) {
			throw new IllegalArgumentException("AI estimated cost cannot be negative.");
		}
	}

	public static AiUsageAccounting zeroCost(String provider, String model) {
		return new AiUsageAccounting(provider, model, 0, 0, BigDecimal.ZERO);
	}
}
