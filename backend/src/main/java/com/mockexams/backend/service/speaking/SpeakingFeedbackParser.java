package com.mockexams.backend.service.speaking;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import com.mockexams.backend.dto.SpeakingCriterionFeedbackRDO;
import com.mockexams.backend.dto.SpeakingMistakeRDO;

@Component
public class SpeakingFeedbackParser {

	private final ObjectMapper objectMapper;
	private final SpeakingRubricService rubricService;

	SpeakingFeedbackParser(ObjectMapper objectMapper, SpeakingRubricService rubricService) {
		this.objectMapper = objectMapper;
		this.rubricService = rubricService;
	}

	public SpeakingFeedbackPayload parse(String partType, String content) {
		List<String> errors = new ArrayList<>();
		JsonNode root;
		try {
			root = objectMapper.readTree(content);
		}
		catch (JsonProcessingException ex) {
			throw new SpeakingFeedbackValidationException(List.of("Provider returned malformed JSON."));
		}
		BigDecimal overallBand = band(root, "overallBand", "overall_band", errors);
		JsonNode criteriaNode = first(root, "criteriaScores", "criteria_scores");
		Map<String, SpeakingCriterionFeedbackRDO> criteria = criteria(criteriaNode, partType, errors);
		List<SpeakingMistakeRDO> repeatedMistakes = mistakes(first(root, "repeatedMistakes", "repeated_mistakes"), errors);
		String betterAnswerExample = requiredText(root, "betterAnswerExample", "better_answer_example", errors);
		List<String> actionPlan = actionPlan(first(root, "actionPlan", "action_plan"), errors);
		if (!errors.isEmpty()) {
			throw new SpeakingFeedbackValidationException(errors);
		}
		return new SpeakingFeedbackPayload(overallBand, criteria, repeatedMistakes, betterAnswerExample, actionPlan);
	}

	private Map<String, SpeakingCriterionFeedbackRDO> criteria(JsonNode node, String partType, List<String> errors) {
		Map<String, SpeakingCriterionFeedbackRDO> criteria = new LinkedHashMap<>();
		if (node == null || !node.isObject()) {
			errors.add("criteriaScores must be an object.");
			return criteria;
		}
		for (String key : rubricService.requiredCriteria(partType)) {
			JsonNode criterionNode = node.get(key);
			if (criterionNode == null || !criterionNode.isObject()) {
				errors.add("criteriaScores." + key + " is required.");
				continue;
			}
			List<String> suggestions = stringArray(criterionNode.get("suggestions"));
			if (suggestions.isEmpty()) {
				errors.add("criteriaScores." + key + ".suggestions must contain at least one item.");
			}
			String feedback = text(criterionNode.get("feedback"));
			if (feedback == null) {
				errors.add("criteriaScores." + key + ".feedback is required.");
			}
			criteria.put(key, new SpeakingCriterionFeedbackRDO(
					band(criterionNode, "band", "score", errors),
					feedback,
					suggestions));
		}
		return criteria;
	}

	private List<SpeakingMistakeRDO> mistakes(JsonNode node, List<String> errors) {
		List<SpeakingMistakeRDO> mistakes = new ArrayList<>();
		if (node == null || !node.isArray()) {
			errors.add("repeatedMistakes must be an array.");
			return mistakes;
		}
		for (JsonNode item : node) {
			if (!item.isObject()) {
				errors.add("Each repeated mistake must be an object.");
				continue;
			}
			String type = text(item.get("type"));
			String original = text(item.get("original"));
			String correction = text(item.get("correction"));
			String explanation = text(item.get("explanation"));
			if (type == null || correction == null || explanation == null) {
				errors.add("Each repeated mistake requires type, correction, and explanation.");
			}
			mistakes.add(new SpeakingMistakeRDO(type, original, correction, explanation));
		}
		return mistakes;
	}

	private List<String> actionPlan(JsonNode node, List<String> errors) {
		List<String> actionPlan = stringArray(node);
		if (actionPlan.isEmpty()) {
			errors.add("actionPlan must contain at least one item.");
		}
		return actionPlan;
	}

	private BigDecimal band(JsonNode root, String primary, String secondary, List<String> errors) {
		JsonNode node = first(root, primary, secondary);
		if (node == null || !node.isNumber()) {
			errors.add(primary + " must be a number.");
			return null;
		}
		BigDecimal value = node.decimalValue();
		if (value.compareTo(BigDecimal.ZERO) < 0 || value.compareTo(new BigDecimal("9.0")) > 0
				|| value.multiply(new BigDecimal("2")).remainder(BigDecimal.ONE).compareTo(BigDecimal.ZERO) != 0) {
			errors.add(primary + " must be between 0 and 9 in 0.5 increments.");
		}
		return value;
	}

	private String requiredText(JsonNode root, String primary, String secondary, List<String> errors) {
		String value = text(first(root, primary, secondary));
		if (value == null) {
			errors.add(primary + " is required.");
		}
		return value;
	}

	private JsonNode first(JsonNode root, String... fieldNames) {
		if (root == null) {
			return null;
		}
		for (String fieldName : fieldNames) {
			JsonNode node = root.get(fieldName);
			if (node != null && !node.isNull()) {
				return node;
			}
		}
		return null;
	}

	private String text(JsonNode node) {
		if (node == null || node.isNull() || !node.isTextual() || node.asText().isBlank()) {
			return null;
		}
		return node.asText().trim();
	}

	private List<String> stringArray(JsonNode node) {
		if (node == null || !node.isArray()) {
			return List.of();
		}
		List<String> values = new ArrayList<>();
		for (JsonNode item : node) {
			String value = text(item);
			if (value != null) {
				values.add(value);
			}
		}
		return values;
	}
}
