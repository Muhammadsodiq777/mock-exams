package com.mockexams.backend.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record StudyPlanCDO(
		@DecimalMin("1.0") @DecimalMax("9.0") BigDecimal targetBand,
		@Min(1) @Max(90) Integer durationDays,
		@Min(15) @Max(240) Integer availableMinutesPerDay,
		LocalDate startsAt
) {
}
