package com.mockexams.backend.dto.schedules;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;

public record ScheduleSlotUDO(
		OffsetDateTime startsAt,
		OffsetDateTime endsAt,
		@Size(max = 80) String timezone,
		@DecimalMin("0.00") BigDecimal priceAmount,
		@Size(min = 3, max = 3) String currency
) {
}
