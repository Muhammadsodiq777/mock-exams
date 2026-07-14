package com.mockexams.backend.service.speaking;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import com.mockexams.backend.entity.SpeakingSubmission;
import com.mockexams.backend.entity.TestTypes;
import com.mockexams.backend.error.BusinessException;

@Component
public class SpeakingRubricService {

	public static final String PART_1_RUBRIC_VERSION = "ielts-speaking-part-1-v1";
	public static final String PART_2_RUBRIC_VERSION = "ielts-speaking-part-2-v1";
	public static final String PART_3_RUBRIC_VERSION = "ielts-speaking-part-3-v1";
	public static final String RESPONSE_SCHEMA_VERSION = "speaking-feedback-json-v1";
	public static final String PRONUNCIATION_SCORING_NOTE = "Pronunciation is a transcript-only proxy based on wording, "
			+ "fluency markers, and phrasing. It is not acoustic pronunciation scoring.";

	public List<String> requiredCriteria(String partType) {
		return switch (partType) {
			case TestTypes.SPEAKING_PART_1, TestTypes.SPEAKING_PART_2, TestTypes.SPEAKING_PART_3 -> List.of(
					"fluency_and_coherence",
					"lexical_resource",
					"grammatical_range_and_accuracy",
					"pronunciation_proxy");
			default -> throw unsupportedPartType();
		};
	}

	public SpeakingFeedbackPrompt buildPrompt(SpeakingSubmission submission) {
		String rubricVersion = rubricVersion(submission.getPartType());
		List<String> criteria = requiredCriteria(submission.getPartType());
		String systemPrompt = """
				You are an IELTS speaking examiner for recorded practice answers. Return only strict JSON matching the requested schema.
				Do not claim official IELTS certification. Scores are practice estimates only.
				Only a transcript is available, so pronunciation must be labeled as pronunciation_proxy and must not claim acoustic analysis.
				""";
		String userPrompt = """
				Speaking part: %s
				Rubric version: %s
				Required criteria: %s
				Part expectations: %s
				Pronunciation limitation: %s

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
				  "repeatedMistakes": [
				    {"type": "grammar|vocabulary|coherence|fluency|pronunciation_proxy", "original": "...", "correction": "...", "explanation": "..."}
				  ],
				  "betterAnswerExample": "A stronger spoken answer example using the student's ideas.",
				  "actionPlan": ["short actionable speaking practice step"]
				}

				Speaking prompt:
				%s

				Student transcript:
				%s
				""".formatted(
				submission.getPartType(),
				rubricVersion,
				criteria,
				partExpectations(submission.getPartType()),
				PRONUNCIATION_SCORING_NOTE,
				submission.getPrompt(),
				submission.getTranscript());
		Map<String, Object> audit = new LinkedHashMap<>();
		audit.put("rubricVersion", rubricVersion);
		audit.put("responseSchemaVersion", RESPONSE_SCHEMA_VERSION);
		audit.put("partType", submission.getPartType());
		audit.put("requiredCriteria", criteria);
		audit.put("transcriptOnlyPronunciation", true);
		audit.put("pronunciationScoringNote", PRONUNCIATION_SCORING_NOTE);
		audit.put("promptHash", sha256(systemPrompt + "\n" + userPrompt));
		return new SpeakingFeedbackPrompt(submission, rubricVersion, systemPrompt, userPrompt, audit);
	}

	public String rubricVersion(String partType) {
		return switch (partType) {
			case TestTypes.SPEAKING_PART_1 -> PART_1_RUBRIC_VERSION;
			case TestTypes.SPEAKING_PART_2 -> PART_2_RUBRIC_VERSION;
			case TestTypes.SPEAKING_PART_3 -> PART_3_RUBRIC_VERSION;
			default -> throw unsupportedPartType();
		};
	}

	private String partExpectations(String partType) {
		return switch (partType) {
			case TestTypes.SPEAKING_PART_1 -> "Short personal answers that are direct, natural, and extended with brief reasons or examples.";
			case TestTypes.SPEAKING_PART_2 -> "A sustained long-turn answer with clear organization, relevant details, and enough development for a cue-card response.";
			case TestTypes.SPEAKING_PART_3 -> "More abstract discussion with explanations, comparisons, examples, and supported opinions.";
			default -> throw unsupportedPartType();
		};
	}

	private BusinessException unsupportedPartType() {
		return new BusinessException("UNSUPPORTED_SPEAKING_PART_TYPE", "Unsupported speaking part type.",
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
