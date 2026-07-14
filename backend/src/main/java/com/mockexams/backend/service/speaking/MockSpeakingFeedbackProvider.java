package com.mockexams.backend.service.speaking;

import java.math.BigDecimal;

public class MockSpeakingFeedbackProvider implements SpeakingFeedbackProvider {

	@Override
	public SpeakingFeedbackProviderResponse evaluate(SpeakingFeedbackPrompt prompt) {
		String content = """
				{
				  "overallBand": 6.5,
				  "criteriaScores": {
				    "fluency_and_coherence": {"band": 6.5, "feedback": "The answer is understandable and mostly connected.", "suggestions": ["Extend answers with clearer linking and examples."]},
				    "lexical_resource": {"band": 6.0, "feedback": "Vocabulary is suitable but sometimes repeated.", "suggestions": ["Use more precise topic vocabulary."]},
				    "grammatical_range_and_accuracy": {"band": 6.0, "feedback": "Meaning is clear despite recurring grammar slips.", "suggestions": ["Practice accurate complex sentences."]},
				    "pronunciation_proxy": {"band": 6.5, "feedback": "Transcript-only phrasing suggests generally clear delivery, but acoustic pronunciation was not measured.", "suggestions": ["Record and separately review stress, rhythm, and intonation."]}
				  },
				  "repeatedMistakes": [
				    {"type": "grammar", "original": "he go", "correction": "he goes", "explanation": "Use third-person singular verb agreement."}
				  ],
				  "betterAnswerExample": "A more natural and extended version of the spoken answer.",
				  "actionPlan": ["Practice extending each answer with a reason.", "Review one repeated grammar pattern before recording again."]
				}
				""";
		return new SpeakingFeedbackProviderResponse(
				"mock",
				"mock-ielts-speaking-v1",
				content,
				estimateTokens(prompt.combinedPrompt()),
				estimateTokens(content),
				BigDecimal.ZERO);
	}

	private int estimateTokens(String value) {
		return Math.max(1, value.length() / 4);
	}
}
