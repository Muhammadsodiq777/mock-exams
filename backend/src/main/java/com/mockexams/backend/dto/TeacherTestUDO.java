package com.mockexams.backend.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public record TeacherTestUDO(
		@Size(max = 180) String title,
		String description,
		@Size(max = 32) String type,
		@Size(max = 40) String difficulty,
		@DecimalMin("0.0") @DecimalMax("9.0") BigDecimal estimatedBandMin,
		@DecimalMin("0.0") @DecimalMax("9.0") BigDecimal estimatedBandMax,
		@Min(1) Integer durationMinutes,
		@DecimalMin("0.00") BigDecimal priceAmount,
		@Size(min = 3, max = 3) String currency,
		Boolean free,
		@Size(max = 20) String language
) {
}
