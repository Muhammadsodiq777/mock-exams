package com.mockexams.backend.service.studyplan;

import java.math.BigDecimal;

public record StudyPlanProviderResponse(
		String provider,
		String model,
		String content,
		int promptTokens,
		int completionTokens,
		BigDecimal estimatedCost
) {
}
