package com.mockexams.backend.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mockexams.backend.dto.ProgressExtensionHookRDO;
import com.mockexams.backend.dto.ProgressRecommendationRDO;
import com.mockexams.backend.dto.ProgressSummaryRDO;
import com.mockexams.backend.dto.RecentAttemptRDO;
import com.mockexams.backend.dto.SkillScorePointRDO;
import com.mockexams.backend.dto.StudentProgressDashboardRDO;
import com.mockexams.backend.dto.WeakQuestionTypeRDO;
import com.mockexams.backend.entity.CatalogTest;
import com.mockexams.backend.entity.TestAttempt;
import com.mockexams.backend.entity.TestAttemptStatuses;
import com.mockexams.backend.entity.TestResult;
import com.mockexams.backend.entity.TestTypes;
import com.mockexams.backend.mapper.StudentProgressMapper;
import com.mockexams.backend.repository.TestAttemptRepository;
import com.mockexams.backend.repository.TestCatalogRepository;
import com.mockexams.backend.repository.TestResultRepository;
import com.mockexams.backend.service.AiUsageService;
import com.mockexams.backend.service.StudentProgressDashboard;
import com.mockexams.backend.service.StudentProgressService;
import com.mockexams.backend.users.entity.User;

@Service
class StudentProgressServiceImpl implements StudentProgressService {

	private static final List<String> OBJECTIVE_SKILLS = List.of(TestTypes.LISTENING, TestTypes.READING);
	private static final List<String> DASHBOARD_SKILLS = List.of(
			TestTypes.LISTENING,
			TestTypes.READING,
			"writing",
			"speaking");
	private static final int HISTORY_LIMIT = 100;
	private static final int RECENT_ATTEMPT_LIMIT = 10;
	private static final int RECOMMENDATION_LIMIT = 3;

	private final TestResultRepository testResultRepository;
	private final TestAttemptRepository testAttemptRepository;
	private final TestCatalogRepository testCatalogRepository;
	private final AiUsageService aiUsageService;
	private final StudentProgressMapper mapper;
	private final Clock clock;

	StudentProgressServiceImpl(
			TestResultRepository testResultRepository,
			TestAttemptRepository testAttemptRepository,
			TestCatalogRepository testCatalogRepository,
			AiUsageService aiUsageService,
			StudentProgressMapper mapper,
			Clock clock
	) {
		this.testResultRepository = testResultRepository;
		this.testAttemptRepository = testAttemptRepository;
		this.testCatalogRepository = testCatalogRepository;
		this.aiUsageService = aiUsageService;
		this.mapper = mapper;
		this.clock = clock;
	}

	@Override
	@Transactional
	public StudentProgressDashboard dashboard(User student, int days) {
		OffsetDateTime asOf = OffsetDateTime.now(clock);
		OffsetDateTime from = asOf.minusDays(days);
		List<TestResult> results = recentObjectiveResultsBySkill(student.getId(), from);
		Map<String, List<SkillScorePointRDO>> histories = histories(results);
		Map<String, TestResult> latestBySkill = latestBySkill(results);
		ProgressSummaryRDO summary = summary(student.getId(), from, latestBySkill);
		List<WeakQuestionTypeRDO> weakAreas = weakAreas(results);
		List<RecentAttemptRDO> recentAttempts = recentAttempts(student.getId());
		List<ProgressRecommendationRDO> recommendations = recommendations(weakAreas, histories);
		StudentProgressDashboardRDO data = mapper.toDashboard(
				summary.completedAttemptCount() == 0 ? "empty" : "active",
				summary,
				histories,
				weakAreas,
				recentAttempts,
				recommendations,
				aiUsageService.remainingUsage(student.getId()),
				extensionHooks());
		return new StudentProgressDashboard(data, asOf, window(days, from, asOf));
	}

	private Map<String, List<SkillScorePointRDO>> histories(List<TestResult> results) {
		Map<String, List<SkillScorePointRDO>> histories = new LinkedHashMap<>();
		for (String skill : OBJECTIVE_SKILLS) {
			List<SkillScorePointRDO> points = results.stream()
					.filter(result -> Objects.equals(result.getAttempt().getTest().getType(), skill))
					.sorted(Comparator.comparing(TestResult::getCreatedAt).thenComparing(TestResult::getId))
					.map(mapper::toScorePoint)
					.toList();
			histories.put(skill, points);
		}
		return histories;
	}

	private List<TestResult> recentObjectiveResultsBySkill(Long studentId, OffsetDateTime from) {
		List<TestResult> results = new ArrayList<>();
		for (String skill : OBJECTIVE_SKILLS) {
			results.addAll(testResultRepository.findRecentObjectiveResults(
					studentId,
					List.of(skill),
					from,
					PageRequest.of(0, HISTORY_LIMIT)));
		}
		return results;
	}

	private Map<String, TestResult> latestBySkill(List<TestResult> results) {
		Map<String, TestResult> latestBySkill = new LinkedHashMap<>();
		for (TestResult result : results) {
			latestBySkill.merge(
					result.getAttempt().getTest().getType(),
					result,
					(current, candidate) -> isLater(candidate, current) ? candidate : current);
		}
		return latestBySkill;
	}

	private boolean isLater(TestResult candidate, TestResult current) {
		int createdAtComparison = candidate.getCreatedAt().compareTo(current.getCreatedAt());
		if (createdAtComparison != 0) {
			return createdAtComparison > 0;
		}
		return candidate.getId() > current.getId();
	}

	private ProgressSummaryRDO summary(Long studentId, OffsetDateTime from, Map<String, TestResult> latestBySkill) {
		Map<String, BigDecimal> skillBands = new LinkedHashMap<>();
		List<BigDecimal> availableBands = new ArrayList<>();
		for (String skill : DASHBOARD_SKILLS) {
			TestResult result = latestBySkill.get(skill);
			BigDecimal band = result == null ? null : result.getEstimatedBand();
			skillBands.put(skill, band);
			if (band != null) {
				availableBands.add(band);
			}
		}
		List<String> missingSkills = skillBands.entrySet().stream()
				.filter(entry -> entry.getValue() == null)
				.map(Map.Entry::getKey)
				.toList();
		OffsetDateTime latestResultAt = latestBySkill.values().stream()
				.map(TestResult::getCreatedAt)
				.max(Comparator.naturalOrder())
				.orElse(null);
		return mapper.toSummary(
				averageBand(availableBands),
				skillBands,
				missingSkills,
				testResultRepository.countObjectiveResults(studentId, OBJECTIVE_SKILLS, from),
				latestResultAt);
	}

	private BigDecimal averageBand(List<BigDecimal> bands) {
		if (bands.isEmpty()) {
			return null;
		}
		BigDecimal total = bands.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
		return total.divide(BigDecimal.valueOf(bands.size()), 1, RoundingMode.HALF_UP);
	}

	private List<WeakQuestionTypeRDO> weakAreas(List<TestResult> results) {
		Map<WeakAreaKey, WeakAreaAggregate> aggregates = new LinkedHashMap<>();
		for (TestResult result : results) {
			String skill = result.getAttempt().getTest().getType();
			for (Map.Entry<String, Object> entry : result.getWeakQuestionTypes().entrySet()) {
				int count = integerValue(entry.getValue());
				if (count <= 0) {
					continue;
				}
				WeakAreaKey key = new WeakAreaKey(skill, entry.getKey());
				WeakAreaAggregate aggregate = aggregates.computeIfAbsent(key, ignored -> new WeakAreaAggregate());
				aggregate.add(count, result.getCreatedAt());
			}
		}
		return aggregates.entrySet().stream()
				.map(entry -> new WeakQuestionTypeRDO(
						entry.getKey().skill(),
						entry.getKey().questionType(),
						entry.getValue().incorrectCount(),
						entry.getValue().attemptCount(),
						entry.getValue().latestSeenAt()))
				.sorted(Comparator
						.comparing(WeakQuestionTypeRDO::latestSeenAt, Comparator.nullsLast(Comparator.reverseOrder()))
						.thenComparing(WeakQuestionTypeRDO::incorrectCount, Comparator.reverseOrder())
						.thenComparing(WeakQuestionTypeRDO::skill)
						.thenComparing(WeakQuestionTypeRDO::questionType))
				.limit(10)
				.toList();
	}

	private int integerValue(Object value) {
		if (value instanceof Number number) {
			return number.intValue();
		}
		if (value instanceof String stringValue && !stringValue.isBlank()) {
			try {
				return Integer.parseInt(stringValue);
			}
			catch (NumberFormatException ignored) {
				return 0;
			}
		}
		return 0;
	}

	private List<RecentAttemptRDO> recentAttempts(Long studentId) {
		List<TestAttempt> attempts = testAttemptRepository.findRecentCompletedAttempts(
				studentId,
				List.of(TestAttemptStatuses.SUBMITTED, TestAttemptStatuses.EXPIRED),
				PageRequest.of(0, RECENT_ATTEMPT_LIMIT));
		if (attempts.isEmpty()) {
			return List.of();
		}
		Map<Long, TestResult> resultsByAttemptId = testResultRepository.findByAttemptIdIn(
						attempts.stream().map(TestAttempt::getId).toList()).stream()
				.collect(Collectors.toMap(
						result -> result.getAttempt().getId(),
						result -> result,
						(first, second) -> first,
						LinkedHashMap::new));
		return attempts.stream()
				.map(attempt -> mapper.toRecentAttempt(attempt, resultsByAttemptId.get(attempt.getId())))
				.toList();
	}

	private List<ProgressRecommendationRDO> recommendations(
			List<WeakQuestionTypeRDO> weakAreas,
			Map<String, List<SkillScorePointRDO>> histories
	) {
		Set<String> preferredTypes = new LinkedHashSet<>();
		weakAreas.stream().map(WeakQuestionTypeRDO::skill).forEach(preferredTypes::add);
		OBJECTIVE_SKILLS.stream()
				.filter(skill -> histories.getOrDefault(skill, List.of()).isEmpty())
				.forEach(preferredTypes::add);
		OBJECTIVE_SKILLS.forEach(preferredTypes::add);

		Map<Long, ProgressRecommendationRDO> recommendations = new LinkedHashMap<>();
		for (String testType : preferredTypes) {
			List<CatalogTest> tests = testCatalogRepository.findRecommendedFreeVisibleByType(
					testType,
					PageRequest.of(0, RECOMMENDATION_LIMIT));
			for (CatalogTest test : tests) {
				recommendations.putIfAbsent(test.getId(), mapper.toRecommendation(test, reason(testType, weakAreas)));
				if (recommendations.size() >= RECOMMENDATION_LIMIT) {
					return List.copyOf(recommendations.values());
				}
			}
		}
		return List.copyOf(recommendations.values());
	}

	private String reason(String testType, List<WeakQuestionTypeRDO> weakAreas) {
		return weakAreas.stream()
				.filter(area -> Objects.equals(area.skill(), testType))
				.findFirst()
				.map(area -> "Practice " + testType + " " + area.questionType())
				.orElse("Start with " + testType + " practice");
	}

	private List<ProgressExtensionHookRDO> extensionHooks() {
		return List.of(
				new ProgressExtensionHookRDO("writing", "ai_feedback", "pending"),
				new ProgressExtensionHookRDO("speaking", "ai_feedback", "pending"),
				new ProgressExtensionHookRDO("study_plan", "ai_recommendations", "pending"));
	}

	private Map<String, Object> window(int days, OffsetDateTime from, OffsetDateTime to) {
		Map<String, Object> window = new LinkedHashMap<>();
		window.put("days", days);
		window.put("from", from);
		window.put("to", to);
		window.put("historyLimit", HISTORY_LIMIT);
		window.put("recentAttemptLimit", RECENT_ATTEMPT_LIMIT);
		return window;
	}

	private record WeakAreaKey(String skill, String questionType) {
	}

	private static final class WeakAreaAggregate {

		private int incorrectCount;
		private int attemptCount;
		private OffsetDateTime latestSeenAt;

		void add(int count, OffsetDateTime seenAt) {
			incorrectCount += count;
			attemptCount++;
			if (latestSeenAt == null || (seenAt != null && seenAt.isAfter(latestSeenAt))) {
				latestSeenAt = seenAt;
			}
		}

		int incorrectCount() {
			return incorrectCount;
		}

		int attemptCount() {
			return attemptCount;
		}

		OffsetDateTime latestSeenAt() {
			return latestSeenAt;
		}
	}
}
