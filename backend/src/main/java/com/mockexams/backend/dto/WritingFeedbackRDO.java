package com.mockexams.backend.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;

public record WritingFeedbackRDO(
		Long id,
		Long submissionId,
		String feedbackType,
		String status,
		@JsonInclude(JsonInclude.Include.NON_NULL) BigDecimal overallBand,
		Map<String, WritingCriterionFeedbackRDO> criteriaScores,
		List<WritingMistakeRDO> mistakes,
		@JsonInclude(JsonInclude.Include.NON_NULL) String improvedAnswer,
		List<String> actionPlan,
		@JsonInclude(JsonInclude.Include.NON_NULL) String failureCode,
		@JsonInclude(JsonInclude.Include.NON_NULL) String failureMessage,
		@JsonInclude(JsonInclude.Include.NON_NULL) String provider,
		@JsonInclude(JsonInclude.Include.NON_NULL) String model,
		int promptTokens,
		int completionTokens,
		BigDecimal estimatedCost,
		@JsonInclude(JsonInclude.Include.NON_NULL) String rubricVersion,
		int retryCount,
		@JsonInclude(JsonInclude.Include.NON_NULL) Long retryOfFeedbackId,
		OffsetDateTime createdAt,
		@JsonInclude(JsonInclude.Include.NON_NULL) OffsetDateTime completedAt
) {
	public WritingFeedbackRDO {
		criteriaScores = criteriaScores == null ? Map.of() : Map.copyOf(criteriaScores);
		mistakes = mistakes == null ? List.of() : List.copyOf(mistakes);
		actionPlan = actionPlan == null ? List.of() : List.copyOf(actionPlan);
	}
}
