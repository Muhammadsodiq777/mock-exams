package com.mockexams.backend.mapper;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.mockexams.backend.dto.AiUsageRemainingRDO;
import com.mockexams.backend.dto.ProgressExtensionHookRDO;
import com.mockexams.backend.dto.ProgressRecommendationRDO;
import com.mockexams.backend.dto.ProgressSummaryRDO;
import com.mockexams.backend.dto.RecentAttemptRDO;
import com.mockexams.backend.dto.SkillScorePointRDO;
import com.mockexams.backend.dto.StudentProgressDashboardRDO;
import com.mockexams.backend.dto.WeakQuestionTypeRDO;
import com.mockexams.backend.entity.CatalogTest;
import com.mockexams.backend.entity.TestAttempt;
import com.mockexams.backend.entity.TestResult;

@Component
public class StudentProgressMapper {

	public StudentProgressDashboardRDO toDashboard(
			String state,
			ProgressSummaryRDO summary,
			Map<String, List<SkillScorePointRDO>> skillHistories,
			List<WeakQuestionTypeRDO> weakAreas,
			List<RecentAttemptRDO> recentAttempts,
			List<ProgressRecommendationRDO> recommendations,
			AiUsageRemainingRDO aiUsage,
			List<ProgressExtensionHookRDO> extensionHooks
	) {
		return new StudentProgressDashboardRDO(
				state,
				summary,
				skillHistories,
				weakAreas,
				recentAttempts,
				recommendations,
				aiUsage,
				extensionHooks);
	}

	public SkillScorePointRDO toScorePoint(TestResult result) {
		TestAttempt attempt = result.getAttempt();
		CatalogTest test = attempt.getTest();
		return new SkillScorePointRDO(
				result.getId(),
				attempt.getId(),
				test.getId(),
				test.getTitle(),
				attempt.getStatus(),
				result.getCreatedAt(),
				result.getRawScore(),
				result.getMaxScore(),
				result.getEstimatedBand());
	}

	public RecentAttemptRDO toRecentAttempt(TestAttempt attempt, TestResult result) {
		CatalogTest test = attempt.getTest();
		return new RecentAttemptRDO(
				attempt.getId(),
				result == null ? null : result.getId(),
				test.getId(),
				test.getTitle(),
				test.getType(),
				attempt.getStatus(),
				attempt.getStartedAt(),
				attempt.getSubmittedAt(),
				attempt.getTimeSpentSeconds(),
				result == null ? null : result.getRawScore(),
				result == null ? null : result.getMaxScore(),
				result == null ? null : result.getEstimatedBand());
	}

	public ProgressRecommendationRDO toRecommendation(CatalogTest test, String reason) {
		return new ProgressRecommendationRDO(
				test.getTeacherProfile() == null ? "official_test" : "teacher_test",
				test.getId(),
				test.getTitle(),
				test.getType(),
				reason,
				true,
				true,
				test.getPublishedAt());
	}

	public ProgressSummaryRDO toSummary(
			BigDecimal overallBandEstimate,
			Map<String, BigDecimal> skillBands,
			List<String> missingSkills,
			long completedAttemptCount,
			OffsetDateTime latestResultAt
	) {
		return new ProgressSummaryRDO(
				overallBandEstimate,
				skillBands,
				missingSkills,
				completedAttemptCount,
				latestResultAt);
	}
}
