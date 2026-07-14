package com.mockexams.backend.service.studyplan;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import com.mockexams.backend.entity.TestTypes;

@Component
public class StudyPlanParser {

	private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
	};
	private static final Set<String> SUPPORTED_SKILLS = Set.of(
			TestTypes.LISTENING,
			TestTypes.READING,
			"writing",
			"speaking");

	private final ObjectMapper objectMapper;

	StudyPlanParser(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	public StudyPlanPayload parse(String content) {
		List<String> errors = new ArrayList<>();
		JsonNode root;
		try {
			root = objectMapper.readTree(content);
		}
		catch (JsonProcessingException ex) {
			throw new StudyPlanValidationException(List.of("Provider returned malformed JSON."));
		}
		Map<String, Object> summary = summary(root.get("summary"), errors);
		List<StudyPlanTaskPayload> tasks = tasks(root.get("tasks"), errors);
		if (!errors.isEmpty()) {
			throw new StudyPlanValidationException(errors);
		}
		return new StudyPlanPayload(summary, tasks);
	}

	private Map<String, Object> summary(JsonNode node, List<String> errors) {
		if (node == null || !node.isObject()) {
			errors.add("summary must be an object.");
			return Map.of();
		}
		return objectMapper.convertValue(node, MAP_TYPE);
	}

	private List<StudyPlanTaskPayload> tasks(JsonNode node, List<String> errors) {
		List<StudyPlanTaskPayload> tasks = new ArrayList<>();
		if (node == null || !node.isArray() || node.isEmpty()) {
			errors.add("tasks must contain at least one task.");
			return tasks;
		}
		for (int i = 0; i < node.size(); i++) {
			JsonNode item = node.get(i);
			if (!item.isObject()) {
				errors.add("tasks[" + i + "] must be an object.");
				continue;
			}
			StudyPlanResourcePayload resource = resource(item.get("recommendedResource"), i, errors);
			String skill = skill(item.get("skill"), i, errors);
			tasks.add(new StudyPlanTaskPayload(
					date(item.get("date"), i, errors),
					skill,
					requiredText(item.get("title"), "tasks[" + i + "].title", errors),
					requiredText(item.get("description"), "tasks[" + i + "].description", errors),
					positiveInt(item.get("estimatedMinutes"), "tasks[" + i + "].estimatedMinutes", errors),
					resource,
					requiredText(item.get("rationale"), "tasks[" + i + "].rationale", errors)));
		}
		return tasks;
	}

	private StudyPlanResourcePayload resource(JsonNode node, int index, List<String> errors) {
		if (node == null || !node.isObject()) {
			errors.add("tasks[" + index + "].recommendedResource must be an object.");
			return new StudyPlanResourcePayload(null, null, null);
		}
		String type = requiredText(node.get("type"), "tasks[" + index + "].recommendedResource.type", errors);
		Long id = node.hasNonNull("id") && node.get("id").canConvertToLong() ? node.get("id").asLong() : null;
		String title = text(node.get("title"));
		if ("test".equals(type) || "listing".equals(type)) {
			if (id == null) {
				errors.add("tasks[" + index + "].recommendedResource.id is required for " + type + " resources.");
			}
		}
		else if (!"diagnostic".equals(type) && !"free_practice".equals(type)) {
			errors.add("tasks[" + index + "].recommendedResource.type is unsupported.");
		}
		return new StudyPlanResourcePayload(type, id, title);
	}

	private String skill(JsonNode node, int index, List<String> errors) {
		String value = requiredText(node, "tasks[" + index + "].skill", errors);
		if (value != null && !SUPPORTED_SKILLS.contains(value)) {
			errors.add("tasks[" + index + "].skill is unsupported.");
		}
		return value;
	}

	private LocalDate date(JsonNode node, int index, List<String> errors) {
		String value = text(node);
		if (value == null) {
			errors.add("tasks[" + index + "].date is required.");
			return null;
		}
		try {
			return LocalDate.parse(value);
		}
		catch (DateTimeParseException ex) {
			errors.add("tasks[" + index + "].date must be an ISO date.");
			return null;
		}
	}

	private int positiveInt(JsonNode node, String path, List<String> errors) {
		if (node == null || !node.canConvertToInt() || node.asInt() <= 0) {
			errors.add(path + " must be a positive integer.");
			return 0;
		}
		return node.asInt();
	}

	private String requiredText(JsonNode node, String path, List<String> errors) {
		String value = text(node);
		if (value == null) {
			errors.add(path + " is required.");
		}
		return value;
	}

	private String text(JsonNode node) {
		if (node == null || node.isNull() || !node.isTextual() || node.asText().isBlank()) {
			return null;
		}
		return node.asText().trim();
	}
}
