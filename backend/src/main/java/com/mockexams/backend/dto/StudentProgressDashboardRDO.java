package com.mockexams.backend.dto;

import java.util.List;
import java.util.Map;

public record StudentProgressDashboardRDO(
		String state,
		ProgressSummaryRDO summary,
		Map<String, List<SkillScorePointRDO>> skillHistories,
		List<WeakQuestionTypeRDO> weakAreas,
		List<RecentAttemptRDO> recentAttempts,
		List<ProgressRecommendationRDO> recommendations,
		AiUsageRemainingRDO aiUsage,
		List<ProgressExtensionHookRDO> extensionHooks
) {
}
