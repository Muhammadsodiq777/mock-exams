package com.mockexams.backend.dto;

import java.math.BigDecimal;
import java.util.List;

public record SpeakingCriterionFeedbackRDO(
		BigDecimal band,
		String feedback,
		List<String> suggestions
) {
	public SpeakingCriterionFeedbackRDO {
		suggestions = suggestions == null ? List.of() : List.copyOf(suggestions);
	}
}
