package com.mockexams.backend.mapper;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import com.mockexams.backend.dto.SpeakingCriterionFeedbackRDO;
import com.mockexams.backend.dto.SpeakingFeedbackRDO;
import com.mockexams.backend.dto.SpeakingMistakeRDO;
import com.mockexams.backend.dto.SpeakingSubmissionRDO;
import com.mockexams.backend.entity.AiFeedback;
import com.mockexams.backend.entity.AiFeedbackStatuses;
import com.mockexams.backend.entity.SpeakingSubmission;
import com.mockexams.backend.service.speaking.SpeakingRubricService;

@Component
public class SpeakingSubmissionMapper {

	private static final String FAILED_MESSAGE = "Speaking feedback could not be generated. Please retry later.";

	private final ObjectMapper objectMapper;

	public SpeakingSubmissionMapper(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	public SpeakingSubmissionRDO toRDO(SpeakingSubmission submission) {
		Long studentUserId = submission.getStudentUser() == null ? null : submission.getStudentUser().getId();
		Long testAttemptId = submission.getTestAttempt() == null ? null : submission.getTestAttempt().getId();
		Long audioFileId = submission.getAudioFile() == null ? null : submission.getAudioFile().getId();
		return new SpeakingSubmissionRDO(
				submission.getId(),
				studentUserId,
				testAttemptId,
				submission.getPartType(),
				submission.getPrompt(),
				audioFileId,
				submission.getTranscript(),
				submission.getDurationSeconds(),
				submission.getTranscriptionStatus(),
				submission.getTranscriptionProvider(),
				submission.getTranscriptionModel(),
				submission.getTranscriptionFailureCode(),
				submission.getTranscriptionErrorMessage(),
				submission.getTranscriptionEstimatedCost(),
				submission.getTranscriptionStartedAt(),
				submission.getTranscriptionCompletedAt(),
				submission.getCreatedAt());
	}

	public SpeakingFeedbackRDO toFeedbackRDO(AiFeedback feedback) {
		return new SpeakingFeedbackRDO(
				feedback.getId(),
				feedback.getSpeakingSubmissionId(),
				feedback.getFeedbackType(),
				feedback.getStatus(),
				feedback.getOverallBand(),
				criteriaScores(feedback.getCriteriaScores()),
				repeatedMistakes(feedback.getMistakes()),
				feedback.getImprovedAnswer(),
				actionPlan(feedback.getActionPlan()),
				true,
				SpeakingRubricService.PRONUNCIATION_SCORING_NOTE,
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

	public Map<String, Object> criteriaToJson(Map<String, SpeakingCriterionFeedbackRDO> criteriaScores) {
		Map<String, Object> values = new LinkedHashMap<>();
		criteriaScores.forEach((key, value) -> values.put(key, objectMapper.convertValue(value, Map.class)));
		return values;
	}

	public List<Object> repeatedMistakesToJson(List<SpeakingMistakeRDO> repeatedMistakes) {
		return repeatedMistakes.stream()
				.map(mistake -> (Object) objectMapper.convertValue(mistake, Map.class))
				.toList();
	}

	public List<Object> actionPlanToJson(List<String> actionPlan) {
		return List.copyOf(actionPlan);
	}

	private Map<String, SpeakingCriterionFeedbackRDO> criteriaScores(Map<String, Object> values) {
		if (values == null || values.isEmpty()) {
			return Map.of();
		}
		Map<String, SpeakingCriterionFeedbackRDO> mapped = new LinkedHashMap<>();
		values.forEach((key, value) -> mapped.put(key, objectMapper.convertValue(value, SpeakingCriterionFeedbackRDO.class)));
		return mapped;
	}

	private List<SpeakingMistakeRDO> repeatedMistakes(List<Object> values) {
		if (values == null || values.isEmpty()) {
			return List.of();
		}
		return values.stream()
				.map(value -> objectMapper.convertValue(value, SpeakingMistakeRDO.class))
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
