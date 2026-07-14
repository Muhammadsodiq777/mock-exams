package com.mockexams.backend.mapper;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import com.mockexams.backend.dto.WritingCriterionFeedbackRDO;
import com.mockexams.backend.dto.WritingFeedbackRDO;
import com.mockexams.backend.dto.WritingMistakeRDO;
import com.mockexams.backend.dto.WritingSubmissionRDO;
import com.mockexams.backend.entity.AiFeedback;
import com.mockexams.backend.entity.AiFeedbackStatuses;
import com.mockexams.backend.entity.WritingSubmission;

@Component
public class WritingSubmissionMapper {

	private static final String FAILED_MESSAGE = "Writing feedback could not be generated. Please retry later.";

	private final ObjectMapper objectMapper;

	public WritingSubmissionMapper(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	public WritingSubmissionRDO toSubmissionRDO(WritingSubmission submission, AiFeedback feedback) {
		return new WritingSubmissionRDO(
				submission.getId(),
				submission.getStudentUser().getId(),
				submission.getTestAttempt() == null ? null : submission.getTestAttempt().getId(),
				submission.getTaskType(),
				submission.getPrompt(),
				submission.getAnswerText(),
				submission.getWordCount(),
				submission.getCreatedAt(),
				feedback == null ? null : toFeedbackRDO(feedback));
	}

	public WritingFeedbackRDO toFeedbackRDO(AiFeedback feedback) {
		return new WritingFeedbackRDO(
				feedback.getId(),
				feedback.getWritingSubmissionId(),
				feedback.getFeedbackType(),
				feedback.getStatus(),
				feedback.getOverallBand(),
				criteriaScores(feedback.getCriteriaScores()),
				mistakes(feedback.getMistakes()),
				feedback.getImprovedAnswer(),
				actionPlan(feedback.getActionPlan()),
				feedback.getFailureCode(),
				AiFeedbackStatuses.FAILED.equals(feedback.getStatus()) ? FAILED_MESSAGE : null,
				feedback.getProvider(),
				feedback.getModel(),
				feedback.getPromptTokens(),
				feedback.getCompletionTokens(),
				feedback.getEstimatedCost(),
				feedback.getRubricVersion(),
				feedback.getRetryCount(),
				feedback.getRetryOfFeedbackId(),
				feedback.getCreatedAt(),
				feedback.getCompletedAt());
	}

	public Map<String, Object> criteriaToJson(Map<String, WritingCriterionFeedbackRDO> criteriaScores) {
		Map<String, Object> values = new LinkedHashMap<>();
		criteriaScores.forEach((key, value) -> values.put(key, objectMapper.convertValue(value, Map.class)));
		return values;
	}

	public List<Object> mistakesToJson(List<WritingMistakeRDO> mistakes) {
		return mistakes.stream()
				.map(mistake -> (Object) objectMapper.convertValue(mistake, Map.class))
				.toList();
	}

	public List<Object> actionPlanToJson(List<String> actionPlan) {
		return List.copyOf(actionPlan);
	}

	private Map<String, WritingCriterionFeedbackRDO> criteriaScores(Map<String, Object> values) {
		if (values == null || values.isEmpty()) {
			return Map.of();
		}
		Map<String, WritingCriterionFeedbackRDO> mapped = new LinkedHashMap<>();
		values.forEach((key, value) -> mapped.put(key, objectMapper.convertValue(value, WritingCriterionFeedbackRDO.class)));
		return mapped;
	}

	private List<WritingMistakeRDO> mistakes(List<Object> values) {
		if (values == null || values.isEmpty()) {
			return List.of();
		}
		return values.stream()
				.map(value -> objectMapper.convertValue(value, WritingMistakeRDO.class))
				.toList();
	}

	private List<String> actionPlan(List<Object> values) {
		if (values == null || values.isEmpty()) {
			return List.of();
		}
		return values.stream()
				.map(String::valueOf)
				.toList();
	}
}
