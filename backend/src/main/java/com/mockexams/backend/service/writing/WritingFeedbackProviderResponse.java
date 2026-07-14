package com.mockexams.backend.service.writing;

import java.math.BigDecimal;

public record WritingFeedbackProviderResponse(
		String provider,
		String model,
		String content,
		int promptTokens,
		int completionTokens,
		BigDecimal estimatedCost
) {
}
