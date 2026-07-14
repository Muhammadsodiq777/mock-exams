package com.mockexams.backend.service.speaking;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import com.mockexams.backend.dto.SpeakingCriterionFeedbackRDO;
import com.mockexams.backend.dto.SpeakingMistakeRDO;

public record SpeakingFeedbackPayload(
		BigDecimal overallBand,
		Map<String, SpeakingCriterionFeedbackRDO> criteriaScores,
		List<SpeakingMistakeRDO> repeatedMistakes,
		String betterAnswerExample,
		List<String> actionPlan
) {
	public SpeakingFeedbackPayload {
		criteriaScores = criteriaScores == null ? Map.of() : Map.copyOf(criteriaScores);
		repeatedMistakes = repeatedMistakes == null ? List.of() : List.copyOf(repeatedMistakes);
		actionPlan = actionPlan == null ? List.of() : List.copyOf(actionPlan);
	}
}
