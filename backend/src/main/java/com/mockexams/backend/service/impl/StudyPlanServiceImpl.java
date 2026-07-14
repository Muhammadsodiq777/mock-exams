package com.mockexams.backend.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mockexams.backend.dto.StudyPlanCDO;
import com.mockexams.backend.dto.StudyPlanRDO;
import com.mockexams.backend.dto.StudyPlanTaskRDO;
import com.mockexams.backend.entity.AiFailureCodes;
import com.mockexams.backend.entity.AiFeedback;
import com.mockexams.backend.entity.AiFeedbackStatuses;
import com.mockexams.backend.entity.AiFeedbackTypes;
import com.mockexams.backend.entity.CatalogTest;
import com.mockexams.backend.entity.MarketplaceListing;
import com.mockexams.backend.entity.StudyPlan;
import com.mockexams.backend.entity.StudyPlanTask;
import com.mockexams.backend.entity.TestResult;
import com.mockexams.backend.entity.TestTypes;
import com.mockexams.backend.error.BusinessException;
import com.mockexams.backend.mapper.StudyPlanMapper;
import com.mockexams.backend.repository.AiFeedbackRepository;
import com.mockexams.backend.repository.MarketplaceListingRepository;
import com.mockexams.backend.repository.StudyPlanRepository;
import com.mockexams.backend.repository.StudyPlanTaskRepository;
import com.mockexams.backend.repository.TestCatalogRepository;
import com.mockexams.backend.repository.TestResultRepository;
import com.mockexams.backend.service.AiUsageAccounting;
import com.mockexams.backend.service.AiUsageRequest;
import com.mockexams.backend.service.AiUsageReservation;
import com.mockexams.backend.service.AiUsageService;
import com.mockexams.backend.service.StudyPlanService;
import com.mockexams.backend.service.studyplan.StudyPlanParser;
import com.mockexams.backend.service.studyplan.StudyPlanPayload;
import com.mockexams.backend.service.studyplan.StudyPlanPrompt;
import com.mockexams.backend.service.studyplan.StudyPlanProvider;
import com.mockexams.backend.service.studyplan.StudyPlanProviderException;
import com.mockexams.backend.service.studyplan.StudyPlanProviderResponse;
import com.mockexams.backend.service.studyplan.StudyPlanResourcePayload;
import com.mockexams.backend.service.studyplan.StudyPlanTaskPayload;
import com.mockexams.backend.service.studyplan.StudyPlanValidationException;
import com.mockexams.backend.users.entity.User;

@Service
class StudyPlanServiceImpl implements StudyPlanService {

	private static final int DEFAULT_AVAILABLE_MINUTES_PER_DAY = 60;
	private static final List<String> PLAN_SKILLS = List.of(
			TestTypes.LISTENING,
			TestTypes.READING,
			"writing",
			"speaking");
	private static final List<String> OBJECTIVE_SKILLS = List.of(TestTypes.LISTENING, TestTypes.READING);
	private static final String PROMPT_VERSION = "study-plan-v1";

	private final StudyPlanRepository studyPlanRepository;
	private final StudyPlanTaskRepository taskRepository;
	private final TestResultRepository testResultRepository;
	private final TestCatalogRepository testCatalogRepository;
	private final MarketplaceListingRepository listingRepository;
	private final AiFeedbackRepository aiFeedbackRepository;
	private final AiUsageService aiUsageService;
	private final StudyPlanProvider provider;
	private final StudyPlanParser parser;
	private final StudyPlanMapper mapper;
	private final Clock clock;

	StudyPlanServiceImpl(
			StudyPlanRepository studyPlanRepository,
			StudyPlanTaskRepository taskRepository,
			TestResultRepository testResultRepository,
			TestCatalogRepository testCatalogRepository,
			MarketplaceListingRepository listingRepository,
			AiFeedbackRepository aiFeedbackRepository,
			AiUsageService aiUsageService,
			StudyPlanProvider provider,
			StudyPlanParser parser,
			StudyPlanMapper mapper,
			Clock clock
	) {
		this.studyPlanRepository = studyPlanRepository;
		this.taskRepository = taskRepository;
		this.testResultRepository = testResultRepository;
		this.testCatalogRepository = testCatalogRepository;
		this.listingRepository = listingRepository;
		this.aiFeedbackRepository = aiFeedbackRepository;
		this.aiUsageService = aiUsageService;
		this.provider = provider;
		this.parser = parser;
		this.mapper = mapper;
		this.clock = clock;
	}

	@Override
	@Transactional(noRollbackFor = BusinessException.class)
	public StudyPlanRDO create(User student, StudyPlanCDO command) {
		StudyPlanRequest request = validate(command);
		LocalDate today = today();
		if (!studyPlanRepository.findActive(student.getId(), today).isEmpty()) {
			throw new BusinessException("STUDY_PLAN_ACTIVE_CONFLICT",
					"Student already has an active study plan.", HttpStatus.CONFLICT);
		}
		StudyPlanInput input = input(student, request);
		GeneratedPlan generated = generate(student, input, request.startsAt(), request.endsAt());
		StudyPlan plan = studyPlanRepository.save(StudyPlan.create(
				student,
				request.targetBand(),
				input.currentBand(),
				request.durationDays(),
				input.weakestSkills(),
				generated.payload().summary(),
				request.startsAt(),
				generated.reservation().feedbackId()));
		saveTasks(plan, generated.tasks());
		aiUsageService.recordSuccess(generated.reservation(), accounting(generated.response()));
		return toRDO(plan);
	}

	@Override
	@Transactional(readOnly = true)
	public StudyPlanRDO get(User student, Long planId) {
		return toRDO(requirePlan(student, planId));
	}

	@Override
	@Transactional(readOnly = true)
	public List<StudyPlanRDO> activePlans(User student) {
		return studyPlanRepository.findActive(student.getId(), today()).stream()
				.map(this::toRDO)
				.toList();
	}

	@Override
	@Transactional(readOnly = true)
	public List<StudyPlanRDO> history(User student) {
		return studyPlanRepository.findHistory(student.getId(), today()).stream()
				.map(this::toRDO)
				.toList();
	}

	@Override
	@Transactional
	public StudyPlanTaskRDO completeTask(User student, Long planId, Long taskId) {
		requirePlan(student, planId);
		StudyPlanTask task = requireTask(planId, taskId);
		task.markCompleted(OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC));
		return mapper.toTaskRDO(task);
	}

	@Override
	@Transactional
	public StudyPlanTaskRDO uncompleteTask(User student, Long planId, Long taskId) {
		requirePlan(student, planId);
		StudyPlanTask task = requireTask(planId, taskId);
		task.markIncomplete();
		return mapper.toTaskRDO(task);
	}

	@Override
	@Transactional(noRollbackFor = BusinessException.class)
	public StudyPlanRDO regenerate(User student, Long planId) {
		StudyPlan plan = studyPlanRepository.findForUpdateByIdAndStudentUserId(planId, student.getId())
				.orElseThrow(this::planNotFound);
		LocalDate fromDate = today().isAfter(plan.getStartsAt()) ? today() : plan.getStartsAt();
		StudyPlanInput input = input(student, new StudyPlanRequest(
				plan.getTargetBand(),
				plan.getDurationDays(),
				DEFAULT_AVAILABLE_MINUTES_PER_DAY,
				plan.getStartsAt(),
				plan.getEndsAt()));
		GeneratedPlan generated = generate(student, input, fromDate, plan.getEndsAt());
		taskRepository.deleteIncompleteFromDate(plan.getId(), fromDate);
		plan.refreshGeneratedContent(
				input.currentBand(),
				input.weakestSkills(),
				generated.payload().summary(),
				generated.reservation().feedbackId());
		saveTasks(plan, generated.tasks());
		aiUsageService.recordSuccess(generated.reservation(), accounting(generated.response()));
		return toRDO(plan);
	}

	private GeneratedPlan generate(User student, StudyPlanInput input, LocalDate fromDate, LocalDate throughDate) {
		StudyPlanPrompt prompt = prompt(input, fromDate, throughDate);
		AiUsageReservation reservation = aiUsageService.guardAndReserve(student.getId(), AiUsageRequest.studyPlan());
		AiFeedback feedback = aiFeedbackRepository.findForUpdateById(reservation.feedbackId())
				.orElseThrow(() -> new BusinessException("AI_FEEDBACK_NOT_FOUND", "AI feedback was not found.",
						HttpStatus.NOT_FOUND));
		feedback.applyPromptAudit(prompt.promptVersion(), prompt.audit());
		try {
			StudyPlanProviderResponse response = provider.generate(prompt);
			StudyPlanPayload payload;
			try {
				payload = parser.parse(response.content());
				List<ResolvedTask> tasks = validateAndResolveTasks(student, payload.tasks(), fromDate, throughDate);
				return new GeneratedPlan(reservation, response, payload, tasks);
			}
			catch (StudyPlanValidationException ex) {
				feedback.applyValidationErrors(ex.getValidationErrors());
				aiUsageService.recordValidationFailure(reservation, accounting(response),
						"AI study plan JSON did not match the study plan schema.");
				throw new BusinessException("AI_STUDY_PLAN_VALIDATION_FAILED",
						"AI study plan response could not be validated.", HttpStatus.SERVICE_UNAVAILABLE);
			}
			catch (RecommendationResolutionException ex) {
				feedback.applyValidationErrors(List.of(ex.getMessage()));
				aiUsageService.recordValidationFailure(reservation, accounting(response),
						"AI study plan referenced inaccessible recommended content.");
				throw new BusinessException("INACCESSIBLE_RECOMMENDED_CONTENT",
						ex.getMessage(), HttpStatus.CONFLICT);
			}
		}
		catch (StudyPlanProviderException ex) {
			aiUsageService.recordProviderFailure(reservation, AiFailureCodes.PROVIDER_FAILURE,
					"AI study plan provider failed.");
			throw new BusinessException("SERVICE_UNAVAILABLE", "AI study plan provider is temporarily unavailable.",
					HttpStatus.SERVICE_UNAVAILABLE);
		}
	}

	private List<ResolvedTask> validateAndResolveTasks(
			User student,
			List<StudyPlanTaskPayload> tasks,
			LocalDate fromDate,
			LocalDate throughDate
	) {
		List<ResolvedTask> resolved = new ArrayList<>();
		for (StudyPlanTaskPayload task : tasks) {
			if (task.date() == null || task.date().isBefore(fromDate) || task.date().isAfter(throughDate)) {
				throw new StudyPlanValidationException(List.of("Task dates must stay within the requested plan window."));
			}
			resolved.add(resolveTask(student, task));
		}
		return resolved.stream()
				.sorted(Comparator.comparing((ResolvedTask task) -> task.payload().date()))
				.toList();
	}

	private ResolvedTask resolveTask(User student, StudyPlanTaskPayload task) {
		StudyPlanResourcePayload resource = task.recommendedResource();
		if ("test".equals(resource.type())) {
			CatalogTest test = testCatalogRepository.findVisibleFreeById(resource.id())
					.orElseThrow(() -> new RecommendationResolutionException(
							"Recommended test is not active, free, and visible."));
			return new ResolvedTask(task, "test", nonBlank(resource.title(), test.getTitle()), test.getId(), null);
		}
		if ("listing".equals(resource.type())) {
			MarketplaceListing listing = listingRepository
					.findAccessibleRecommendation(resource.id(), student.getId())
					.orElseThrow(() -> new RecommendationResolutionException(
							"Recommended listing is inactive or not accessible to this student."));
			return new ResolvedTask(task, "listing", nonBlank(resource.title(), listing.getTitle()), null, listing.getId());
		}
		return new ResolvedTask(task, resource.type(), resource.title(), null, null);
	}

	private void saveTasks(StudyPlan plan, List<ResolvedTask> tasks) {
		Map<LocalDate, Integer> nextOrderByDate = new LinkedHashMap<>();
		for (ResolvedTask task : tasks) {
			int nextOrder = nextOrderByDate.computeIfAbsent(
					task.payload().date(),
					date -> taskRepository.maxOrderForDate(plan.getId(), date)) + 1;
			nextOrderByDate.put(task.payload().date(), nextOrder);
			taskRepository.save(StudyPlanTask.create(
					plan,
					task.payload().date(),
					task.payload().skill(),
					task.payload().title(),
					task.payload().description(),
					task.payload().estimatedMinutes(),
					task.resourceType(),
					task.resourceTitle(),
					task.recommendedTestId(),
					task.recommendedListingId(),
					task.payload().rationale(),
					nextOrder));
		}
	}

	private StudyPlanInput input(User student, StudyPlanRequest request) {
		List<TestResult> objectiveResults = objectiveResults(student.getId());
		Map<String, BigDecimal> latestBands = latestBands(student.getId(), objectiveResults);
		List<String> missingSkills = PLAN_SKILLS.stream()
				.filter(skill -> latestBands.get(skill) == null)
				.toList();
		List<String> weakestSkills = weakestSkills(latestBands, objectiveResults, missingSkills);
		return new StudyPlanInput(
				request,
				currentBand(latestBands),
				latestBands,
				missingSkills,
				weakestSkills,
				evidence(objectiveResults),
				accessibleContent());
	}

	private List<TestResult> objectiveResults(Long studentId) {
		return testResultRepository.findRecentObjectiveResults(
				studentId,
				OBJECTIVE_SKILLS,
				OffsetDateTime.now(clock).minusDays(180),
				PageRequest.of(0, 20));
	}

	private Map<String, BigDecimal> latestBands(Long studentId, List<TestResult> objectiveResults) {
		Map<String, BigDecimal> bands = new LinkedHashMap<>();
		PLAN_SKILLS.forEach(skill -> bands.put(skill, null));
		for (TestResult result : objectiveResults) {
			String skill = result.getAttempt().getTest().getType();
			if (bands.get(skill) == null && result.getEstimatedBand() != null) {
				bands.put(skill, result.getEstimatedBand());
			}
		}
		bands.put("writing", latestAiBand(studentId, AiFeedbackTypes.WRITING));
		bands.put("speaking", latestAiBand(studentId, AiFeedbackTypes.SPEAKING));
		return bands;
	}

	private BigDecimal latestAiBand(Long studentId, String feedbackType) {
		return aiFeedbackRepository
				.findTop5ByStudentUserIdAndFeedbackTypeAndStatusAndOverallBandIsNotNullOrderByCreatedAtDescIdDesc(
						studentId,
						feedbackType,
						AiFeedbackStatuses.COMPLETED)
				.stream()
				.map(AiFeedback::getOverallBand)
				.filter(Objects::nonNull)
				.findFirst()
				.orElse(null);
	}

	private BigDecimal currentBand(Map<String, BigDecimal> latestBands) {
		List<BigDecimal> available = latestBands.values().stream()
				.filter(Objects::nonNull)
				.toList();
		if (available.isEmpty()) {
			return null;
		}
		BigDecimal total = available.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
		return total.divide(BigDecimal.valueOf(available.size()), 1, RoundingMode.HALF_UP);
	}

	private List<String> weakestSkills(
			Map<String, BigDecimal> latestBands,
			List<TestResult> objectiveResults,
			List<String> missingSkills
	) {
		LinkedHashSet<String> weakest = new LinkedHashSet<>();
		latestBands.entrySet().stream()
				.filter(entry -> entry.getValue() != null)
				.sorted(Map.Entry.comparingByValue())
				.limit(2)
				.map(Map.Entry::getKey)
				.forEach(weakest::add);
		for (TestResult result : objectiveResults) {
			if (!result.getWeakQuestionTypes().isEmpty()) {
				weakest.add(result.getAttempt().getTest().getType());
			}
		}
		weakest.addAll(missingSkills);
		return List.copyOf(weakest);
	}

	private List<Map<String, Object>> evidence(List<TestResult> objectiveResults) {
		return objectiveResults.stream()
				.limit(5)
				.map(result -> {
					Map<String, Object> row = new LinkedHashMap<>();
					row.put("skill", result.getAttempt().getTest().getType());
					row.put("estimatedBand", result.getEstimatedBand());
					row.put("weakQuestionTypes", result.getWeakQuestionTypes());
					row.put("createdAt", result.getCreatedAt());
					return row;
				})
				.toList();
	}

	private List<Map<String, Object>> accessibleContent() {
		List<Map<String, Object>> content = new ArrayList<>();
		for (String skill : OBJECTIVE_SKILLS) {
			testCatalogRepository.findRecommendedFreeVisibleByType(skill, PageRequest.of(0, 2))
					.forEach(test -> {
						Map<String, Object> row = new LinkedHashMap<>();
						row.put("type", "test");
						row.put("id", test.getId());
						row.put("title", test.getTitle());
						row.put("skill", test.getType());
						row.put("free", true);
						content.add(row);
					});
		}
		listingRepository.findActiveFree(PageRequest.of(0, 3))
				.forEach(listing -> {
					Map<String, Object> row = new LinkedHashMap<>();
					row.put("type", "listing");
					row.put("id", listing.getId());
					row.put("title", listing.getTitle());
					row.put("free", true);
					content.add(row);
				});
		return content;
	}

	private StudyPlanPrompt prompt(StudyPlanInput input, LocalDate fromDate, LocalDate throughDate) {
		String systemPrompt = """
				Generate an IELTS study plan as strict JSON with summary and tasks.
				Each task must include date, skill, title, description, estimatedMinutes, recommendedResource, and rationale.
				Use only listed accessible content by id. If evidence is sparse, label uncertainty and recommend diagnostics.
				""";
		String userPrompt = """
				Target band: %s
				Current band estimate: %s
				Duration days: %d
				Available minutes per day: %d
				Plan window: %s to %s
				Missing skills: %s
				Weakest skills: %s
				Latest skill bands: %s
				Recent evidence: %s
				Accessible content: %s
				""".formatted(
				input.request().targetBand(),
				input.currentBand(),
				input.request().durationDays(),
				input.request().availableMinutesPerDay(),
				fromDate,
				throughDate,
				input.missingSkills(),
				input.weakestSkills(),
				input.latestBands(),
				input.evidence(),
				input.accessibleContent());
		Map<String, Object> audit = new LinkedHashMap<>();
		audit.put("targetBand", input.request().targetBand());
		audit.put("durationDays", input.request().durationDays());
		audit.put("availableMinutesPerDay", input.request().availableMinutesPerDay());
		audit.put("fromDate", fromDate);
		audit.put("throughDate", throughDate);
		audit.put("missingSkills", input.missingSkills());
		audit.put("weakestSkills", input.weakestSkills());
		audit.put("accessibleContentCount", input.accessibleContent().size());
		return new StudyPlanPrompt(systemPrompt, userPrompt, PROMPT_VERSION, audit, fromDate,
				input.request().durationDays(), input.missingSkills());
	}

	private StudyPlanRequest validate(StudyPlanCDO command) {
		if (command == null) {
			throw validationFailed("Study plan request is required.");
		}
		if (command.targetBand() == null) {
			throw validationFailed("Target band is required.");
		}
		if (command.targetBand().compareTo(BigDecimal.ONE) < 0
				|| command.targetBand().compareTo(new BigDecimal("9.0")) > 0) {
			throw validationFailed("Target band must be between 1.0 and 9.0.");
		}
		if (command.durationDays() == null) {
			throw validationFailed("Duration days is required.");
		}
		if (command.durationDays() < 1 || command.durationDays() > 90) {
			throw validationFailed("Duration days must be between 1 and 90.");
		}
		int minutes = command.availableMinutesPerDay() == null
				? DEFAULT_AVAILABLE_MINUTES_PER_DAY
				: command.availableMinutesPerDay();
		if (minutes < 15 || minutes > 240) {
			throw validationFailed("Available minutes per day must be between 15 and 240.");
		}
		LocalDate startsAt = command.startsAt() == null ? today() : command.startsAt();
		return new StudyPlanRequest(
				command.targetBand(),
				command.durationDays(),
				minutes,
				startsAt,
				startsAt.plusDays(command.durationDays() - 1L));
	}

	private StudyPlan requirePlan(User student, Long planId) {
		return studyPlanRepository.findByIdAndStudentUserId(planId, student.getId())
				.orElseThrow(this::planNotFound);
	}

	private StudyPlanTask requireTask(Long planId, Long taskId) {
		return taskRepository.findForUpdateByIdAndStudyPlanId(taskId, planId)
				.orElseThrow(() -> new BusinessException("STUDY_PLAN_TASK_NOT_FOUND",
						"Study plan task was not found.", HttpStatus.NOT_FOUND));
	}

	private StudyPlanRDO toRDO(StudyPlan plan) {
		return mapper.toRDO(plan, taskRepository.findByStudyPlanIdOrderByTaskDateAscTaskOrderAscIdAsc(plan.getId()));
	}

	private AiUsageAccounting accounting(StudyPlanProviderResponse response) {
		return new AiUsageAccounting(
				response.provider(),
				response.model(),
				response.promptTokens(),
				response.completionTokens(),
				response.estimatedCost());
	}

	private LocalDate today() {
		return LocalDate.now(clock);
	}

	private String nonBlank(String first, String fallback) {
		return first == null || first.isBlank() ? fallback : first;
	}

	private BusinessException validationFailed(String message) {
		return new BusinessException("VALIDATION_FAILED", message, HttpStatus.BAD_REQUEST);
	}

	private BusinessException planNotFound() {
		return new BusinessException("STUDY_PLAN_NOT_FOUND", "Study plan was not found.", HttpStatus.NOT_FOUND);
	}

	private record StudyPlanRequest(
			BigDecimal targetBand,
			int durationDays,
			int availableMinutesPerDay,
			LocalDate startsAt,
			LocalDate endsAt
	) {
	}

	private record StudyPlanInput(
			StudyPlanRequest request,
			BigDecimal currentBand,
			Map<String, BigDecimal> latestBands,
			List<String> missingSkills,
			List<String> weakestSkills,
			List<Map<String, Object>> evidence,
			List<Map<String, Object>> accessibleContent
	) {
	}

	private record GeneratedPlan(
			AiUsageReservation reservation,
			StudyPlanProviderResponse response,
			StudyPlanPayload payload,
			List<ResolvedTask> tasks
	) {
	}

	private record ResolvedTask(
			StudyPlanTaskPayload payload,
			String resourceType,
			String resourceTitle,
			Long recommendedTestId,
			Long recommendedListingId
	) {
	}

	private static class RecommendationResolutionException extends RuntimeException {

		RecommendationResolutionException(String message) {
			super(message);
		}
	}
}
