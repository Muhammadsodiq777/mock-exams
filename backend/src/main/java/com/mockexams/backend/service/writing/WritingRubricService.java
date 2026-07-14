package com.mockexams.backend.service.writing;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import com.mockexams.backend.entity.TestTypes;
import com.mockexams.backend.entity.WritingSubmission;
import com.mockexams.backend.error.BusinessException;

@Component
public class WritingRubricService {

	public static final String TASK_1_RUBRIC_VERSION = "ielts-writing-task-1-v1";
	public static final String TASK_2_RUBRIC_VERSION = "ielts-writing-task-2-v1";
	public static final String RESPONSE_SCHEMA_VERSION = "writing-feedback-json-v1";

	public List<String> requiredCriteria(String taskType) {
		return switch (taskType) {
			case TestTypes.WRITING_TASK_1 -> List.of(
					"task_achievement",
					"coherence_and_cohesion",
					"lexical_resource",
					"grammatical_range_and_accuracy");
			case TestTypes.WRITING_TASK_2 -> List.of(
					"task_response",
					"coherence_and_cohesion",
					"lexical_resource",
					"grammatical_range_and_accuracy");
			default -> throw unsupportedTaskType();
		};
	}

	public WritingFeedbackPrompt buildPrompt(WritingSubmission submission) {
		String rubricVersion = rubricVersion(submission.getTaskType());
		List<String> criteria = requiredCriteria(submission.getTaskType());
		String systemPrompt = """
				You are an IELTS writing examiner. Return only strict JSON matching the requested schema.
				Do not claim official IELTS certification. Scores are practice estimates only.
				""";
		String userPrompt = """
				Task type: %s
				Rubric version: %s
				Required criteria: %s

				Response JSON schema:
				{
				  "overallBand": number from 0 to 9 in 0.5 increments,
				  "criteriaScores": {
				    "<criterion_key>": {
				      "band": number from 0 to 9 in 0.5 increments,
				      "feedback": "criterion-specific feedback",
				      "suggestions": ["concrete next step"]
				    }
				  },
				  "mistakes": [
				    {"type": "grammar|vocabulary|coherence|task_response|task_achievement", "original": "...", "correction": "...", "explanation": "..."}
				  ],
				  "improvedAnswer": "A stronger version of the student's answer.",
				  "actionPlan": ["short actionable practice step"]
				}

				Writing prompt:
				%s

				Student answer:
				%s
				""".formatted(
				submission.getTaskType(),
				rubricVersion,
				criteria,
				submission.getPrompt(),
				submission.getAnswerText());
		Map<String, Object> audit = new LinkedHashMap<>();
		audit.put("rubricVersion", rubricVersion);
		audit.put("responseSchemaVersion", RESPONSE_SCHEMA_VERSION);
		audit.put("taskType", submission.getTaskType());
		audit.put("requiredCriteria", criteria);
		audit.put("promptHash", sha256(systemPrompt + "\n" + userPrompt));
		return new WritingFeedbackPrompt(submission, rubricVersion, systemPrompt, userPrompt, audit);
	}

	public String rubricVersion(String taskType) {
		return switch (taskType) {
			case TestTypes.WRITING_TASK_1 -> TASK_1_RUBRIC_VERSION;
			case TestTypes.WRITING_TASK_2 -> TASK_2_RUBRIC_VERSION;
			default -> throw unsupportedTaskType();
		};
	}

	private BusinessException unsupportedTaskType() {
		return new BusinessException("UNSUPPORTED_WRITING_TASK_TYPE", "Unsupported writing task type.",
				HttpStatus.BAD_REQUEST);
	}

	private String sha256(String value) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
		}
		catch (NoSuchAlgorithmException ex) {
			throw new IllegalStateException("SHA-256 digest is unavailable.", ex);
		}
	}
}
