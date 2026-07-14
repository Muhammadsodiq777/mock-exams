package com.mockexams.backend.service;

public record AiUsageReservation(
		Long feedbackId,
		Long usageLimitId,
		Long studentUserId,
		String feedbackType
) {
}
