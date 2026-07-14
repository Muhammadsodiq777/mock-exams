package com.mockexams.backend.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mockexams.backend.config.BackendEnvironmentProperties;
import com.mockexams.backend.dto.AdminAiUsageReportRDO;
import com.mockexams.backend.dto.AdminAiUsageReportTotalsRDO;
import com.mockexams.backend.dto.AiUsageRemainingRDO;
import com.mockexams.backend.entity.AiFailureCodes;
import com.mockexams.backend.entity.AiFeedback;
import com.mockexams.backend.entity.AiFeedbackStatuses;
import com.mockexams.backend.entity.AiFeedbackTypes;
import com.mockexams.backend.entity.AiUsageLimit;
import com.mockexams.backend.error.BusinessException;
import com.mockexams.backend.mapper.AiUsageMapper;
import com.mockexams.backend.repository.AiFeedbackRepository;
import com.mockexams.backend.repository.AiUsageLimitRepository;
import com.mockexams.backend.service.AiUsageAccounting;
import com.mockexams.backend.service.AiUsageReportCriteria;
import com.mockexams.backend.service.AiUsageRequest;
import com.mockexams.backend.service.AiUsageReservation;
import com.mockexams.backend.service.AiUsageService;
import com.mockexams.backend.service.PlatformSettingService;
import com.mockexams.backend.users.entity.User;
import com.mockexams.backend.users.repository.UserRepository;

@Service
class AiUsageServiceImpl implements AiUsageService {

	private static final BigDecimal TOKENS_PER_THOUSAND = new BigDecimal("1000");

	private final UserRepository userRepository;
	private final AiUsageLimitRepository usageLimitRepository;
	private final AiFeedbackRepository feedbackRepository;
	private final AiUsageMapper mapper;
	private final BackendEnvironmentProperties properties;
	private final PlatformSettingService platformSettingService;
	private final Clock clock;

	AiUsageServiceImpl(
			UserRepository userRepository,
			AiUsageLimitRepository usageLimitRepository,
			AiFeedbackRepository feedbackRepository,
			AiUsageMapper mapper,
			BackendEnvironmentProperties properties,
			PlatformSettingService platformSettingService,
			Clock clock
	) {
		this.userRepository = userRepository;
		this.usageLimitRepository = usageLimitRepository;
		this.feedbackRepository = feedbackRepository;
		this.mapper = mapper;
		this.properties = properties;
		this.platformSettingService = platformSettingService;
		this.clock = clock;
	}

	@Override
	@Transactional(noRollbackFor = BusinessException.class)
	public AiUsageReservation guardAndReserve(Long studentUserId, AiUsageRequest request) {
		validateRequestShape(request);
		User user = lockUser(studentUserId);
		if (!providerAvailableFor(request)) {
			feedbackRepository.save(AiFeedback.failed(
					user,
					request.feedbackType(),
					request.writingSubmissionId(),
					request.speakingSubmissionId(),
					AiFailureCodes.PROVIDER_DISABLED,
					"AI provider is disabled."));
			throw new BusinessException("SERVICE_UNAVAILABLE", "AI provider is temporarily unavailable.",
					HttpStatus.SERVICE_UNAVAILABLE);
		}

		AiUsageLimit limit = activeLimitForUpdate(user);
		if (limit.isHardLimitReached()) {
			feedbackRepository.save(AiFeedback.failed(
					user,
					request.feedbackType(),
					request.writingSubmissionId(),
					request.speakingSubmissionId(),
					AiFailureCodes.USAGE_LIMIT_EXCEEDED,
					"AI usage limit has been reached."));
			throw new BusinessException(AiFailureCodes.USAGE_LIMIT_EXCEEDED, "AI usage limit has been reached.",
					HttpStatus.TOO_MANY_REQUESTS);
		}

		limit.reserveRequest();
		AiFeedback feedback = feedbackRepository.save(AiFeedback.processing(
				user,
				request.feedbackType(),
				request.writingSubmissionId(),
				request.speakingSubmissionId()));
		return new AiUsageReservation(feedback.getId(), limit.getId(), user.getId(), request.feedbackType());
	}

	@Override
	@Transactional
	public void recordSuccess(AiUsageReservation reservation, AiUsageAccounting accounting) {
		AiFeedback feedback = feedbackRepository.findForUpdateById(reservation.feedbackId())
				.orElseThrow(() -> notFound("AI feedback record was not found."));
		requireProcessing(feedback);
		BigDecimal estimatedCost = accounting.estimatedCost() == null
				? estimateCost(accounting.promptTokens(), accounting.completionTokens())
				: accounting.estimatedCost();
		feedback.markCompleted(
				accounting.provider(),
				accounting.model(),
				accounting.promptTokens(),
				accounting.completionTokens(),
				estimatedCost);
	}

	@Override
	@Transactional
	public void recordProviderFailure(AiUsageReservation reservation, String failureCode, String errorMessage) {
		AiFeedback feedback = feedbackRepository.findForUpdateById(reservation.feedbackId())
				.orElseThrow(() -> notFound("AI feedback record was not found."));
		requireProcessing(feedback);
		feedback.markFailed(
				failureCode == null || failureCode.isBlank() ? AiFailureCodes.PROVIDER_FAILURE : failureCode,
				errorMessage,
				properties.openai().enabled() ? "openai" : null,
				properties.openai().enabled() ? properties.openai().model() : null,
				0,
				0,
				BigDecimal.ZERO);
		if (!properties.ai().usage().countProviderFailures()) {
			AiUsageLimit limit = usageLimitRepository.findForUpdateById(reservation.usageLimitId())
					.orElseThrow(() -> notFound("AI usage limit record was not found."));
			limit.releaseRequest();
		}
	}

	@Override
	@Transactional
	public void recordValidationFailure(AiUsageReservation reservation, AiUsageAccounting accounting,
			String errorMessage) {
		AiFeedback feedback = feedbackRepository.findForUpdateById(reservation.feedbackId())
				.orElseThrow(() -> notFound("AI feedback record was not found."));
		requireProcessing(feedback);
		BigDecimal estimatedCost = accounting.estimatedCost() == null
				? estimateCost(accounting.promptTokens(), accounting.completionTokens())
				: accounting.estimatedCost();
		feedback.markFailed(
				AiFailureCodes.VALIDATION_FAILED,
				errorMessage,
				accounting.provider(),
				accounting.model(),
				accounting.promptTokens(),
				accounting.completionTokens(),
				estimatedCost);
	}

	@Override
	@Transactional
	public void recordValidationFailure(Long studentUserId, AiUsageRequest request, String errorMessage) {
		validateRequestShape(request);
		User user = userRepository.findById(studentUserId)
				.orElseThrow(() -> notFound("User was not found."));
		feedbackRepository.save(AiFeedback.failed(
				user,
				request.feedbackType(),
				request.writingSubmissionId(),
				request.speakingSubmissionId(),
				AiFailureCodes.VALIDATION_FAILED,
				errorMessage));
	}

	@Override
	public BigDecimal estimateCost(int promptTokens, int completionTokens) {
		if (promptTokens < 0 || completionTokens < 0) {
			throw new BusinessException("VALIDATION_FAILED", "AI token counts cannot be negative.", HttpStatus.BAD_REQUEST);
		}
		BackendEnvironmentProperties.Usage usage = properties.ai().usage();
		BigDecimal promptCost = usage.promptTokenCostPer1k()
				.multiply(BigDecimal.valueOf(promptTokens))
				.divide(TOKENS_PER_THOUSAND, 6, RoundingMode.HALF_UP);
		BigDecimal completionCost = usage.completionTokenCostPer1k()
				.multiply(BigDecimal.valueOf(completionTokens))
				.divide(TOKENS_PER_THOUSAND, 6, RoundingMode.HALF_UP);
		return promptCost.add(completionCost).setScale(6, RoundingMode.HALF_UP);
	}

	@Override
	@Transactional
	public AiUsageRemainingRDO remainingUsage(Long studentUserId) {
		User user = lockUser(studentUserId);
		return mapper.toRemainingUsage(activeLimitForUpdate(user));
	}

	@Override
	@Transactional(readOnly = true)
	public Page<AdminAiUsageReportRDO> listReport(AiUsageReportCriteria criteria, Pageable pageable) {
		validateStatusFilter(criteria.status());
		return feedbackRepository.searchReport(
				criteria.studentUserId(),
				criteria.feedbackType(),
				criteria.status(),
				criteria.provider(),
				criteria.from(),
				criteria.to(),
				pageable).map(mapper::toAdminReport);
	}

	@Override
	@Transactional(readOnly = true)
	public AdminAiUsageReportTotalsRDO summarizeReport(AiUsageReportCriteria criteria) {
		validateStatusFilter(criteria.status());
		return feedbackRepository.summarizeReport(
				criteria.studentUserId(),
				criteria.feedbackType(),
				criteria.status(),
				criteria.provider(),
				criteria.from(),
				criteria.to());
	}

	@Override
	public List<String> supportedFeedbackTypes() {
		return List.of(AiFeedbackTypes.WRITING, AiFeedbackTypes.SPEAKING, AiFeedbackTypes.STUDY_PLAN);
	}

	private User lockUser(Long studentUserId) {
		if (studentUserId == null) {
			throw new BusinessException("VALIDATION_FAILED", "Student user id is required.", HttpStatus.BAD_REQUEST);
		}
		return userRepository.findForUpdateById(studentUserId)
				.orElseThrow(() -> notFound("User was not found."));
	}

	private AiUsageLimit activeLimitForUpdate(User user) {
		Period period = currentPeriod();
		return usageLimitRepository.findActiveForUpdate(user.getId(), period.start(), period.end())
				.orElseGet(() -> usageLimitRepository.save(new AiUsageLimit(
						user,
						period.start(),
						period.end(),
						platformSettingService.aiFreeMonthlyRequestsLimit(),
						platformSettingService.aiHardMonthlyRequestsLimit())));
	}

	private Period currentPeriod() {
		LocalDate today = LocalDate.now(clock);
		YearMonth month = YearMonth.from(today);
		return new Period(month.atDay(1), month.atEndOfMonth());
	}

	private void validateRequestShape(AiUsageRequest request) {
		if (request == null || !AiFeedbackTypes.isSupported(request.feedbackType())) {
			throw new BusinessException("VALIDATION_FAILED", "Unsupported AI feedback type.", HttpStatus.BAD_REQUEST);
		}
		boolean valid = switch (request.feedbackType()) {
			case AiFeedbackTypes.WRITING -> request.writingSubmissionId() != null && request.speakingSubmissionId() == null;
			case AiFeedbackTypes.SPEAKING -> request.speakingSubmissionId() != null && request.writingSubmissionId() == null;
			case AiFeedbackTypes.STUDY_PLAN -> request.writingSubmissionId() == null && request.speakingSubmissionId() == null;
			default -> false;
		};
		if (!valid) {
			throw new BusinessException("VALIDATION_FAILED", "AI feedback request shape is invalid.",
					HttpStatus.BAD_REQUEST);
		}
	}

	private boolean providerAvailableFor(AiUsageRequest request) {
		if (AiFeedbackTypes.WRITING.equals(request.feedbackType())
				&& "mock".equalsIgnoreCase(properties.ai().writing().provider())) {
			return true;
		}
		if (AiFeedbackTypes.SPEAKING.equals(request.feedbackType())
				&& "mock".equalsIgnoreCase(properties.ai().speaking().provider())) {
			return true;
		}
		if (AiFeedbackTypes.STUDY_PLAN.equals(request.feedbackType())
				&& "mock".equalsIgnoreCase(properties.ai().studyPlan().provider())) {
			return true;
		}
		return properties.openai().enabled();
	}

	private void requireProcessing(AiFeedback feedback) {
		if (!feedback.isProcessing()) {
			throw new BusinessException("AI_FEEDBACK_STATUS_CONFLICT", "AI feedback has already been accounted.",
					HttpStatus.CONFLICT);
		}
	}

	private void validateStatusFilter(String status) {
		if (status != null && !AiFeedbackStatuses.isSupported(status)) {
			throw new BusinessException("VALIDATION_FAILED", "Unsupported AI feedback status.", HttpStatus.BAD_REQUEST);
		}
	}

	private BusinessException notFound(String message) {
		return new BusinessException("NOT_FOUND", message, HttpStatus.NOT_FOUND);
	}

	private record Period(LocalDate start, LocalDate end) {
	}
}
