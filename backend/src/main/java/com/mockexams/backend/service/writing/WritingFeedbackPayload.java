package com.mockexams.backend.service.writing;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import com.mockexams.backend.dto.WritingCriterionFeedbackRDO;
import com.mockexams.backend.dto.WritingMistakeRDO;

public record WritingFeedbackPayload(
		BigDecimal overallBand,
		Map<String, WritingCriterionFeedbackRDO> criteriaScores,
		List<WritingMistakeRDO> mistakes,
		String improvedAnswer,
		List<String> actionPlan
) {
	public WritingFeedbackPayload {
		criteriaScores = criteriaScores == null ? Map.of() : Map.copyOf(criteriaScores);
		mistakes = mistakes == null ? List.of() : List.copyOf(mistakes);
		actionPlan = actionPlan == null ? List.of() : List.copyOf(actionPlan);
	}
}
