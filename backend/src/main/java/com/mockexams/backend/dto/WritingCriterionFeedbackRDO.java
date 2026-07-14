package com.mockexams.backend.dto;

import java.math.BigDecimal;
import java.util.List;

public record WritingCriterionFeedbackRDO(
		BigDecimal band,
		String feedback,
		List<String> suggestions
) {
	public WritingCriterionFeedbackRDO {
		suggestions = suggestions == null ? List.of() : List.copyOf(suggestions);
	}
}
