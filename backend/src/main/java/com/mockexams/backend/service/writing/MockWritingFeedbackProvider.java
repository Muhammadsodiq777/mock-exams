package com.mockexams.backend.service.writing;

import java.math.BigDecimal;

import com.mockexams.backend.entity.TestTypes;

public class MockWritingFeedbackProvider implements WritingFeedbackProvider {

	@Override
	public WritingFeedbackProviderResponse evaluate(WritingFeedbackPrompt prompt) {
		String firstCriterion = TestTypes.WRITING_TASK_1.equals(prompt.submission().getTaskType())
				? "task_achievement"
				: "task_response";
		String content = """
				{
				  "overallBand": 6.5,
				  "criteriaScores": {
				    "%s": {"band": 6.5, "feedback": "The response addresses the task but needs sharper development.", "suggestions": ["Add more precise supporting detail."]},
				    "coherence_and_cohesion": {"band": 6.0, "feedback": "Ideas are generally clear with some linking issues.", "suggestions": ["Use clearer paragraph progression."]},
				    "lexical_resource": {"band": 6.5, "feedback": "Vocabulary is adequate with some repetition.", "suggestions": ["Replace repeated common words with accurate alternatives."]},
				    "grammatical_range_and_accuracy": {"band": 6.0, "feedback": "Grammar is understandable but errors reduce control.", "suggestions": ["Review sentence boundaries and verb forms."]}
				  },
				  "mistakes": [
				    {"type": "grammar", "original": "people is", "correction": "people are", "explanation": "Use plural verb agreement with people."}
				  ],
				  "improvedAnswer": "This revised answer keeps the student's ideas while improving organization, vocabulary, and grammar.",
				  "actionPlan": ["Write one outline before each essay.", "Check subject-verb agreement in the final review."]
				}
				""".formatted(firstCriterion);
		return new WritingFeedbackProviderResponse(
				"mock",
				"mock-ielts-writing-v1",
				content,
				estimateTokens(prompt.combinedPrompt()),
				estimateTokens(content),
				BigDecimal.ZERO);
	}

	private int estimateTokens(String value) {
		return Math.max(1, value.length() / 4);
	}
}
