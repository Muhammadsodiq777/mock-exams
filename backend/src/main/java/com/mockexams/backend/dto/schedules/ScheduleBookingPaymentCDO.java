package com.mockexams.backend.dto.schedules;

import jakarta.validation.constraints.Size;

public record ScheduleBookingPaymentCDO(
		@Size(max = 500) String returnUrl
) {
}
