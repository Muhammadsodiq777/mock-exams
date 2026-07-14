package com.mockexams.backend.service;

import java.math.BigDecimal;

public record TranscriptionResult(
		String transcript,
		String provider,
		String model,
		BigDecimal estimatedCost
) {
}
