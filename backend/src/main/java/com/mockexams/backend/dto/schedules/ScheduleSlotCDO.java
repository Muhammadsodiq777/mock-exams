package com.mockexams.backend.dto.schedules;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ScheduleSlotCDO(
		@NotNull OffsetDateTime startsAt,
		@NotNull OffsetDateTime endsAt,
		@NotBlank @Size(max = 80) String timezone,
		@NotNull @DecimalMin("0.00") BigDecimal priceAmount,
		@NotBlank @Size(min = 3, max = 3) String currency
) {
}
