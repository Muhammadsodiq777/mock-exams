package com.mockexams.backend.service.speaking;

import java.math.BigDecimal;

public record SpeakingFeedbackProviderResponse(
		String provider,
		String model,
		String content,
		int promptTokens,
		int completionTokens,
		BigDecimal estimatedCost
) {
}
