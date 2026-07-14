package com.mockexams.backend.service.writing;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import com.mockexams.backend.dto.WritingCriterionFeedbackRDO;
import com.mockexams.backend.dto.WritingMistakeRDO;

@Component
public class WritingFeedbackParser {

	private final ObjectMapper objectMapper;
	private final WritingRubricService rubricService;

	WritingFeedbackParser(ObjectMapper objectMapper, WritingRubricService rubricService) {
		this.objectMapper = objectMapper;
		this.rubricService = rubricService;
	}

	public WritingFeedbackPayload parse(String taskType, String content) {
		List<String> errors = new ArrayList<>();
		JsonNode root;
		try {
			root = objectMapper.readTree(content);
		}
		catch (JsonProcessingException ex) {
			throw new WritingFeedbackValidationException(List.of("Provider returned malformed JSON."));
		}
		BigDecimal overallBand = band(root, "overallBand", "overall_band", errors);
		JsonNode criteriaNode = first(root, "criteriaScores", "criteria_scores");
		Map<String, WritingCriterionFeedbackRDO> criteria = criteria(criteriaNode, taskType, errors);
		List<WritingMistakeRDO> mistakes = mistakes(first(root, "mistakes"), errors);
		String improvedAnswer = requiredText(root, "improvedAnswer", "improved_answer", errors);
		List<String> actionPlan = actionPlan(first(root, "actionPlan", "action_plan"), errors);
		if (!errors.isEmpty()) {
			throw new WritingFeedbackValidationException(errors);
		}
		return new WritingFeedbackPayload(overallBand, criteria, mistakes, improvedAnswer, actionPlan);
	}

	private Map<String, WritingCriterionFeedbackRDO> criteria(JsonNode node, String taskType, List<String> errors) {
		Map<String, WritingCriterionFeedbackRDO> criteria = new LinkedHashMap<>();
		if (node == null || !node.isObject()) {
			errors.add("criteriaScores must be an object.");
			return criteria;
		}
		for (String key : rubricService.requiredCriteria(taskType)) {
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
			criteria.put(key, new WritingCriterionFeedbackRDO(
					band(criterionNode, "band", "score", errors),
					feedback,
					suggestions));
		}
		return criteria;
	}

	private List<WritingMistakeRDO> mistakes(JsonNode node, List<String> errors) {
		List<WritingMistakeRDO> mistakes = new ArrayList<>();
		if (node == null || !node.isArray()) {
			errors.add("mistakes must be an array.");
			return mistakes;
		}
		for (JsonNode item : node) {
			if (!item.isObject()) {
				errors.add("Each mistake must be an object.");
				continue;
			}
			String type = text(item.get("type"));
			String original = text(item.get("original"));
			String correction = text(item.get("correction"));
			String explanation = text(item.get("explanation"));
			if (type == null || correction == null || explanation == null) {
				errors.add("Each mistake requires type, correction, and explanation.");
			}
			mistakes.add(new WritingMistakeRDO(type, original, correction, explanation));
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
