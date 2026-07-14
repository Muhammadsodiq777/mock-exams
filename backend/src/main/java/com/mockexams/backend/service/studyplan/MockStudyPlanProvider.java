package com.mockexams.backend.service.studyplan;

import java.math.BigDecimal;

import com.mockexams.backend.entity.TestTypes;

public class MockStudyPlanProvider implements StudyPlanProvider {

	@Override
	public StudyPlanProviderResponse generate(StudyPlanPrompt prompt) {
		String firstSkill = prompt.missingSkills().isEmpty() ? TestTypes.READING : prompt.missingSkills().getFirst();
		String content = """
				{
				  "summary": {
				    "overview": "Mock study plan generated from available progress evidence.",
				    "uncertaintyNote": "%s",
				    "missingSkills": %s,
				    "evidence": ["Local mock provider used for development and tests."]
				  },
				  "tasks": [
				    {
				      "date": "%s",
				      "skill": "%s",
				      "title": "Diagnostic practice",
				      "description": "Complete a focused diagnostic task and review mistakes.",
				      "estimatedMinutes": 45,
				      "recommendedResource": {"type": "diagnostic", "title": "Diagnostic practice"},
				      "rationale": "Use diagnostics to ground the next plan revision in real evidence."
				    }
				  ]
				}
				""".formatted(
				prompt.missingSkills().isEmpty()
						? "Evidence is sufficient for an initial plan."
						: "Some skills are missing recent evidence; complete diagnostics before trusting score-specific advice.",
				missingSkillsJson(prompt),
				prompt.startsAt(),
				firstSkill);
		return new StudyPlanProviderResponse("mock", "mock-study-plan", content, 0, 0, BigDecimal.ZERO);
	}

	private String missingSkillsJson(StudyPlanPrompt prompt) {
		return prompt.missingSkills().stream()
				.map(skill -> "\"" + skill + "\"")
				.toList()
				.toString();
	}
}
