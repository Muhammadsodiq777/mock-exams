package com.mockexams.backend.dto;

import java.math.BigDecimal;
import java.util.Map;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record WithdrawalRequestCDO(
		@NotNull(message = "Withdrawal amount is required.")
		@DecimalMin(value = "0.01", message = "Withdrawal amount must be greater than zero.")
		@Digits(integer = 12, fraction = 2, message = "Withdrawal amount must use at most 2 decimal places.")
		BigDecimal amount,
		@Size(min = 3, max = 3, message = "Currency must be a 3-letter code.")
		String currency,
		@NotNull(message = "Payout details are required.")
		Map<String, Object> payoutDetails
) {
}
