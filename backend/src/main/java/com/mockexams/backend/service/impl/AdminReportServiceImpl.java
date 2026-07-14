package com.mockexams.backend.service.impl;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mockexams.backend.dto.AdminAiUsageAggregateReportRDO;
import com.mockexams.backend.dto.AdminAiUsageAggregateRowRDO;
import com.mockexams.backend.dto.AdminAiUsageReportTotalsRDO;
import com.mockexams.backend.dto.AdminProviderFailureReportRDO;
import com.mockexams.backend.dto.AdminProviderFailureReportRowRDO;
import com.mockexams.backend.dto.AdminProviderFailureReportTotalsRDO;
import com.mockexams.backend.dto.AdminReportFiltersRDO;
import com.mockexams.backend.dto.AdminRevenueReportRDO;
import com.mockexams.backend.dto.AdminRevenueReportRowRDO;
import com.mockexams.backend.dto.AdminRevenueReportTotalsRDO;
import com.mockexams.backend.dto.AdminSalesReportDisputeRowRDO;
import com.mockexams.backend.dto.AdminSalesReportRDO;
import com.mockexams.backend.dto.AdminSalesReportRowRDO;
import com.mockexams.backend.dto.AdminSalesReportTotalsRDO;
import com.mockexams.backend.dto.AdminTestActivityReportRDO;
import com.mockexams.backend.dto.AdminTestActivityReportRowRDO;
import com.mockexams.backend.dto.AdminTestActivityReportTotalsRDO;
import com.mockexams.backend.entity.AiFailureCodes;
import com.mockexams.backend.entity.AiFeedbackStatuses;
import com.mockexams.backend.entity.AiFeedbackTypes;
import com.mockexams.backend.entity.Payment;
import com.mockexams.backend.entity.PaymentStatuses;
import com.mockexams.backend.entity.PaymentTargetTypes;
import com.mockexams.backend.entity.TestTypes;
import com.mockexams.backend.entity.TeacherTransactionTypes;
import com.mockexams.backend.error.BusinessException;
import com.mockexams.backend.repository.AiFeedbackRepository;
import com.mockexams.backend.repository.PaymentDisputeRepository;
import com.mockexams.backend.repository.PaymentRepository;
import com.mockexams.backend.repository.TeacherTransactionRepository;
import com.mockexams.backend.repository.TestAttemptRepository;
import com.mockexams.backend.service.AdminReportCriteria;
import com.mockexams.backend.service.AdminReportService;

@Service
class AdminReportServiceImpl implements AdminReportService {

	private static final String LIVE_SPEAKING_RESERVED =
			"Live-speaking booking reports are reserved until booking persistence from plan 0067 exists.";

	private final AiFeedbackRepository aiFeedbackRepository;
	private final PaymentRepository paymentRepository;
	private final PaymentDisputeRepository paymentDisputeRepository;
	private final TeacherTransactionRepository teacherTransactionRepository;
	private final TestAttemptRepository testAttemptRepository;
	private final Clock clock;

	AdminReportServiceImpl(
			AiFeedbackRepository aiFeedbackRepository,
			PaymentRepository paymentRepository,
			PaymentDisputeRepository paymentDisputeRepository,
			TeacherTransactionRepository teacherTransactionRepository,
			TestAttemptRepository testAttemptRepository,
			Clock clock
	) {
		this.aiFeedbackRepository = aiFeedbackRepository;
		this.paymentRepository = paymentRepository;
		this.paymentDisputeRepository = paymentDisputeRepository;
		this.teacherTransactionRepository = teacherTransactionRepository;
		this.testAttemptRepository = testAttemptRepository;
		this.clock = clock;
	}

	@Override
	@Transactional(readOnly = true)
	public AdminAiUsageAggregateReportRDO aiUsage(AdminReportCriteria criteria) {
		AdminReportCriteria normalized = normalize(criteria);
		validateRange(normalized);
		validateAiFilters(normalized.feedbackType(), normalized.status());
		List<AdminAiUsageAggregateRowRDO> rows = aiFeedbackRepository.aggregateReport(
				normalized.studentUserId(),
				normalized.feedbackType(),
				normalized.status(),
				normalized.provider(),
				normalized.model(),
				normalized.from(),
				normalized.to());
		return new AdminAiUsageAggregateReportRDO(
				generatedAt(),
				filters(normalized),
				aiTotals(rows),
				rows);
	}

	@Override
	@Transactional(readOnly = true)
	public AdminSalesReportRDO sales(AdminReportCriteria criteria) {
		AdminReportCriteria normalized = normalize(criteria);
		validateRange(normalized);
		validatePaymentStatus(normalized.paymentStatus());
		List<AdminSalesReportRowRDO> rows = paymentRepository.salesReportRows(
				normalized.teacherProfileId(),
				normalized.listingId(),
				normalized.provider(),
				normalized.paymentStatus(),
				normalized.from(),
				normalized.to());
		rows = withSalesDisputeCounts(rows, salesDisputeRows(normalized));
		return new AdminSalesReportRDO(
				generatedAt(),
				filters(normalized),
				salesTotals(rows, salesDisputeCount(normalized)),
				rows,
				LIVE_SPEAKING_RESERVED);
	}

	@Override
	@Transactional(readOnly = true)
	public AdminRevenueReportRDO revenue(AdminReportCriteria criteria) {
		AdminReportCriteria normalized = normalize(criteria);
		validateRange(normalized);
		List<AdminRevenueReportRowRDO> rows = teacherTransactionRepository.revenueReportRows(
				normalized.teacherProfileId(),
				normalized.listingId(),
				normalized.from(),
				normalized.to());
		return new AdminRevenueReportRDO(
				generatedAt(),
				filters(normalized),
				revenueTotals(rows),
				rows,
				LIVE_SPEAKING_RESERVED);
	}

	@Override
	@Transactional(readOnly = true)
	public AdminTestActivityReportRDO testActivity(AdminReportCriteria criteria) {
		AdminReportCriteria normalized = normalize(criteria);
		validateRange(normalized);
		if (normalized.testType() != null && !TestTypes.isSupported(normalized.testType())) {
			throw validation("Unsupported test type.");
		}
		List<AdminTestActivityReportRowRDO> rows = testAttemptRepository.activityReportRows(
				normalized.teacherProfileId(),
				normalized.testType(),
				normalized.from(),
				normalized.to());
		return new AdminTestActivityReportRDO(
				generatedAt(),
				filters(normalized),
				testActivityTotals(rows),
				rows);
	}

	@Override
	@Transactional(readOnly = true)
	public AdminProviderFailureReportRDO providerFailures(AdminReportCriteria criteria) {
		AdminReportCriteria normalized = normalize(criteria);
		validateRange(normalized);
		validatePaymentStatus(normalized.paymentStatus());
		if (normalized.status() != null && !AiFeedbackStatuses.isSupported(normalized.status())) {
			throw validation("Unsupported AI feedback status.");
		}
		List<AdminProviderFailureReportRowRDO> rows = new ArrayList<>();
		rows.addAll(paymentRepository.paymentFailureReportRows(
				normalized.provider(),
				normalized.paymentStatus(),
				normalized.from(),
				normalized.to()));
		rows.addAll(aiFeedbackRepository.aiFailureReportRows(
				normalized.provider(),
				normalized.model(),
				normalized.status(),
				normalized.from(),
				normalized.to()));
		rows.sort(Comparator
				.comparing(AdminProviderFailureReportRowRDO::activityDate, Comparator.nullsLast(Comparator.naturalOrder()))
				.thenComparing(AdminProviderFailureReportRowRDO::sourceType, Comparator.nullsLast(String::compareTo))
				.thenComparing(AdminProviderFailureReportRowRDO::provider, Comparator.nullsLast(String::compareTo))
				.thenComparing(AdminProviderFailureReportRowRDO::failureCode, Comparator.nullsLast(String::compareTo)));
		return new AdminProviderFailureReportRDO(
				generatedAt(),
				filters(normalized),
				providerFailureTotals(rows),
				rows,
				LIVE_SPEAKING_RESERVED);
	}

	private AdminReportCriteria normalize(AdminReportCriteria criteria) {
		AdminReportCriteria source = criteria == null
				? new AdminReportCriteria(null, null, null, null, null, null, null, null, null, null, null)
				: criteria;
		return new AdminReportCriteria(
				utc(source.from()),
				utc(source.to()),
				source.studentUserId(),
				source.teacherProfileId(),
				source.listingId(),
				lower(source.testType()),
				lower(source.feedbackType()),
				lower(source.status()),
				blankToNull(source.model()),
				lower(source.provider()),
				lower(source.paymentStatus()));
	}

	private OffsetDateTime utc(OffsetDateTime value) {
		return value == null ? null : value.withOffsetSameInstant(ZoneOffset.UTC);
	}

	private String lower(String value) {
		String trimmed = blankToNull(value);
		return trimmed == null ? null : trimmed.toLowerCase(Locale.ROOT);
	}

	private String blankToNull(String value) {
		if (value == null || value.isBlank()) {
			return null;
		}
		return value.trim();
	}

	private void validateRange(AdminReportCriteria criteria) {
		if (criteria.from() != null && criteria.to() != null && criteria.to().isBefore(criteria.from())) {
			throw validation("The to filter must not be before from.");
		}
	}

	private void validateAiFilters(String feedbackType, String status) {
		if (feedbackType != null && !AiFeedbackTypes.isSupported(feedbackType)) {
			throw validation("Unsupported AI feedback type.");
		}
		if (status != null && !AiFeedbackStatuses.isSupported(status)) {
			throw validation("Unsupported AI feedback status.");
		}
	}

	private void validatePaymentStatus(String paymentStatus) {
		if (paymentStatus != null && !PaymentStatuses.isSupported(paymentStatus)) {
			throw validation("Unsupported payment status.");
		}
	}

	private AdminReportFiltersRDO filters(AdminReportCriteria criteria) {
		return new AdminReportFiltersRDO(
				criteria.from(),
				criteria.to(),
				criteria.studentUserId(),
				criteria.teacherProfileId(),
				criteria.listingId(),
				criteria.testType(),
				criteria.feedbackType(),
				criteria.status(),
				criteria.model(),
				criteria.provider(),
				criteria.paymentStatus());
	}

	private OffsetDateTime generatedAt() {
		return OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC);
	}

	private AdminAiUsageReportTotalsRDO aiTotals(List<AdminAiUsageAggregateRowRDO> rows) {
		long total = 0;
		long completed = 0;
		long failed = 0;
		long validationFailed = 0;
		long blocked = 0;
		long promptTokens = 0;
		long completionTokens = 0;
		BigDecimal estimatedCost = BigDecimal.ZERO;
		for (AdminAiUsageAggregateRowRDO row : rows) {
			total += row.requestCount();
			if (AiFeedbackStatuses.COMPLETED.equals(row.status())) {
				completed += row.requestCount();
			}
			if (AiFeedbackStatuses.FAILED.equals(row.status())
					&& !AiFailureCodes.VALIDATION_FAILED.equals(row.failureCode())
					&& !AiFailureCodes.USAGE_LIMIT_EXCEEDED.equals(row.failureCode())) {
				failed += row.requestCount();
			}
			validationFailed += row.validationFailedRequests();
			blocked += row.blockedRequests();
			promptTokens += row.promptTokens();
			completionTokens += row.completionTokens();
			estimatedCost = estimatedCost.add(row.estimatedCost());
		}
		return new AdminAiUsageReportTotalsRDO(
				total,
				completed,
				failed,
				validationFailed,
				blocked,
				promptTokens,
				completionTokens,
				estimatedCost);
	}

	private long salesDisputeCount(AdminReportCriteria criteria) {
		return paymentDisputeRepository.countForSalesReport(
				criteria.teacherProfileId(),
				criteria.listingId(),
				criteria.provider(),
				criteria.paymentStatus(),
				criteria.from(),
				criteria.to());
	}

	private List<AdminSalesReportDisputeRowRDO> salesDisputeRows(AdminReportCriteria criteria) {
		return paymentDisputeRepository.salesReportDisputeRows(
				criteria.teacherProfileId(),
				criteria.listingId(),
				criteria.provider(),
				criteria.paymentStatus(),
				criteria.from(),
				criteria.to());
	}

	private List<AdminSalesReportRowRDO> withSalesDisputeCounts(
			List<AdminSalesReportRowRDO> rows,
			List<AdminSalesReportDisputeRowRDO> disputeRows
	) {
		Map<SalesReportGroupKey, Long> disputeCounts = new HashMap<>();
		for (AdminSalesReportDisputeRowRDO disputeRow : disputeRows) {
			disputeCounts.put(
					SalesReportGroupKey.from(disputeRow),
					disputeRow.disputeCount());
		}
		List<AdminSalesReportRowRDO> updatedRows = new ArrayList<>(rows.size());
		for (AdminSalesReportRowRDO row : rows) {
			updatedRows.add(new AdminSalesReportRowRDO(
					row.activityDate(),
					row.teacherProfileId(),
					row.listingId(),
					row.targetType(),
					row.provider(),
					row.status(),
					row.currency(),
					row.totalPayments(),
					row.grossAmount(),
					row.paidGrossAmount(),
					row.failedGrossAmount(),
					row.refundedGrossAmount(),
					disputeCounts.getOrDefault(SalesReportGroupKey.from(row), 0L)));
		}
		return updatedRows;
	}

	private AdminSalesReportTotalsRDO salesTotals(List<AdminSalesReportRowRDO> rows, long disputeCount) {
		long totalPayments = 0;
		long paidPayments = 0;
		long failedPayments = 0;
		long refundedPayments = 0;
		BigDecimal grossAmount = BigDecimal.ZERO;
		BigDecimal paidGrossAmount = BigDecimal.ZERO;
		BigDecimal failedGrossAmount = BigDecimal.ZERO;
		BigDecimal refundedGrossAmount = BigDecimal.ZERO;
		BigDecimal marketplaceGrossAmount = BigDecimal.ZERO;
		BigDecimal liveSpeakingGrossAmount = BigDecimal.ZERO;
		for (AdminSalesReportRowRDO row : rows) {
			totalPayments += row.totalPayments();
			if (PaymentStatuses.PAID.equals(row.status())) {
				paidPayments += row.totalPayments();
			}
			if (PaymentStatuses.FAILED.equals(row.status())) {
				failedPayments += row.totalPayments();
			}
			if (PaymentStatuses.REFUNDED.equals(row.status())) {
				refundedPayments += row.totalPayments();
			}
			grossAmount = grossAmount.add(row.grossAmount());
			paidGrossAmount = paidGrossAmount.add(row.paidGrossAmount());
			failedGrossAmount = failedGrossAmount.add(row.failedGrossAmount());
			refundedGrossAmount = refundedGrossAmount.add(row.refundedGrossAmount());
			if (PaymentTargetTypes.LIVE_SPEAKING_BOOKING.equals(row.targetType())) {
				liveSpeakingGrossAmount = liveSpeakingGrossAmount.add(row.grossAmount());
			}
			else if (PaymentTargetTypes.MARKETPLACE_LISTING.equals(row.targetType())) {
				marketplaceGrossAmount = marketplaceGrossAmount.add(row.grossAmount());
			}
		}
		return new AdminSalesReportTotalsRDO(
				totalPayments,
				paidPayments,
				failedPayments,
				refundedPayments,
				grossAmount,
				paidGrossAmount,
				failedGrossAmount,
				refundedGrossAmount,
				marketplaceGrossAmount,
				liveSpeakingGrossAmount,
				disputeCount);
	}

	private AdminRevenueReportTotalsRDO revenueTotals(List<AdminRevenueReportRowRDO> rows) {
		long transactionCount = 0;
		long refundCount = 0;
		BigDecimal grossTotal = BigDecimal.ZERO;
		BigDecimal platformCommissionTotal = BigDecimal.ZERO;
		BigDecimal netTotal = BigDecimal.ZERO;
		for (AdminRevenueReportRowRDO row : rows) {
			transactionCount += row.transactionCount();
			if (TeacherTransactionTypes.REFUND.equals(row.transactionType())) {
				refundCount += row.transactionCount();
			}
			else {
				refundCount += row.refundCount();
			}
			grossTotal = grossTotal.add(row.grossTotal());
			platformCommissionTotal = platformCommissionTotal.add(row.platformCommissionTotal());
			netTotal = netTotal.add(row.netTotal());
		}
		return new AdminRevenueReportTotalsRDO(transactionCount, grossTotal, platformCommissionTotal, netTotal, refundCount);
	}

	private AdminTestActivityReportTotalsRDO testActivityTotals(List<AdminTestActivityReportRowRDO> rows) {
		long totalAttempts = 0;
		long submittedAttempts = 0;
		long expiredAttempts = 0;
		long inProgressAttempts = 0;
		long totalTimeSpentSeconds = 0;
		for (AdminTestActivityReportRowRDO row : rows) {
			totalAttempts += row.attemptCount();
			submittedAttempts += row.submittedCount();
			expiredAttempts += row.expiredCount();
			inProgressAttempts += row.inProgressCount();
			totalTimeSpentSeconds += row.totalTimeSpentSeconds();
		}
		long average = totalAttempts == 0 ? 0 : Math.round((double) totalTimeSpentSeconds / totalAttempts);
		return new AdminTestActivityReportTotalsRDO(
				totalAttempts,
				submittedAttempts,
				expiredAttempts,
				inProgressAttempts,
				totalTimeSpentSeconds,
				average);
	}

	private AdminProviderFailureReportTotalsRDO providerFailureTotals(List<AdminProviderFailureReportRowRDO> rows) {
		long failureCount = 0;
		long paymentFailures = 0;
		long aiFailures = 0;
		long amountMismatchPayments = 0;
		long validationFailures = 0;
		long providerFailures = 0;
		BigDecimal paymentAmountTotal = BigDecimal.ZERO;
		BigDecimal aiEstimatedCostTotal = BigDecimal.ZERO;
		for (AdminProviderFailureReportRowRDO row : rows) {
			failureCount += row.failureCount();
			if ("payment".equals(row.sourceType())) {
				paymentFailures += row.failureCount();
				paymentAmountTotal = paymentAmountTotal.add(row.paymentAmountTotal());
				if (Payment.FAILURE_CODE_AMOUNT_MISMATCH.equals(row.failureCode())) {
					amountMismatchPayments += row.failureCount();
				}
			}
			if ("ai".equals(row.sourceType())) {
				aiFailures += row.failureCount();
				aiEstimatedCostTotal = aiEstimatedCostTotal.add(row.aiEstimatedCostTotal());
				if (AiFailureCodes.VALIDATION_FAILED.equals(row.failureCode())) {
					validationFailures += row.failureCount();
				}
				else if (!AiFailureCodes.USAGE_LIMIT_EXCEEDED.equals(row.failureCode())) {
					providerFailures += row.failureCount();
				}
			}
		}
		return new AdminProviderFailureReportTotalsRDO(
				failureCount,
				paymentFailures,
				aiFailures,
				amountMismatchPayments,
				validationFailures,
				providerFailures,
				paymentAmountTotal,
				aiEstimatedCostTotal);
	}

	private BusinessException validation(String message) {
		return new BusinessException("VALIDATION_FAILED", message, HttpStatus.BAD_REQUEST);
	}

	private record SalesReportGroupKey(
			LocalDate activityDate,
			Long teacherProfileId,
			Long listingId,
			String targetType,
			String provider,
			String status,
			String currency
	) {

		private static SalesReportGroupKey from(AdminSalesReportRowRDO row) {
			return new SalesReportGroupKey(
					row.activityDate(),
					row.teacherProfileId(),
					row.listingId(),
					row.targetType(),
					row.provider(),
					row.status(),
					row.currency());
		}

		private static SalesReportGroupKey from(AdminSalesReportDisputeRowRDO row) {
			return new SalesReportGroupKey(
					row.activityDate(),
					row.teacherProfileId(),
					row.listingId(),
					row.targetType(),
					row.provider(),
					row.status(),
					row.currency());
		}
	}
}
