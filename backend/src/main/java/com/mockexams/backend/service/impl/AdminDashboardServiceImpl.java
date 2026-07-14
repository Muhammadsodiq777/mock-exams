package com.mockexams.backend.service.impl;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mockexams.backend.dto.AdminAiUsageReportTotalsRDO;
import com.mockexams.backend.dto.AdminDashboardActivityRDO;
import com.mockexams.backend.dto.AdminDashboardPaymentsRDO;
import com.mockexams.backend.dto.AdminDashboardQueuesRDO;
import com.mockexams.backend.dto.AdminDashboardRDO;
import com.mockexams.backend.dto.AdminDashboardStatusCountRDO;
import com.mockexams.backend.dto.AdminDashboardTeachersRDO;
import com.mockexams.backend.dto.AdminDashboardTestsRDO;
import com.mockexams.backend.dto.AdminDashboardUsersRDO;
import com.mockexams.backend.dto.AdminDashboardWithdrawalsRDO;
import com.mockexams.backend.entity.Payment;
import com.mockexams.backend.entity.PaymentStatuses;
import com.mockexams.backend.entity.TestVisibilities;
import com.mockexams.backend.entity.WithdrawalRequestStatuses;
import com.mockexams.backend.error.BusinessException;
import com.mockexams.backend.mapper.AdminDashboardMapper;
import com.mockexams.backend.repository.PaymentRepository;
import com.mockexams.backend.repository.TestCatalogRepository;
import com.mockexams.backend.repository.WithdrawalRequestRepository;
import com.mockexams.backend.service.AdminDashboard;
import com.mockexams.backend.service.AdminDashboardService;
import com.mockexams.backend.service.AiUsageReportCriteria;
import com.mockexams.backend.service.AiUsageService;
import com.mockexams.backend.teachers.TeacherProfileStatuses;
import com.mockexams.backend.teachers.repository.TeacherProfileRepository;
import com.mockexams.backend.users.UserStatuses;
import com.mockexams.backend.users.repository.UserRepository;

@Service
class AdminDashboardServiceImpl implements AdminDashboardService {

	private static final int RECENT_ACTIVITY_LIMIT = 10;

	private final UserRepository userRepository;
	private final TeacherProfileRepository teacherProfileRepository;
	private final TestCatalogRepository testCatalogRepository;
	private final PaymentRepository paymentRepository;
	private final WithdrawalRequestRepository withdrawalRequestRepository;
	private final AiUsageService aiUsageService;
	private final AdminDashboardMapper mapper;
	private final Clock clock;

	AdminDashboardServiceImpl(
			UserRepository userRepository,
			TeacherProfileRepository teacherProfileRepository,
			TestCatalogRepository testCatalogRepository,
			PaymentRepository paymentRepository,
			WithdrawalRequestRepository withdrawalRequestRepository,
			AiUsageService aiUsageService,
			AdminDashboardMapper mapper,
			Clock clock
	) {
		this.userRepository = userRepository;
		this.teacherProfileRepository = teacherProfileRepository;
		this.testCatalogRepository = testCatalogRepository;
		this.paymentRepository = paymentRepository;
		this.withdrawalRequestRepository = withdrawalRequestRepository;
		this.aiUsageService = aiUsageService;
		this.mapper = mapper;
		this.clock = clock;
	}

	@Override
	@Transactional(readOnly = true)
	public AdminDashboard dashboard(OffsetDateTime from, OffsetDateTime to) {
		requireValidRange(from, to);
		OffsetDateTime asOf = OffsetDateTime.now(clock);
		AdminDashboardRDO data = new AdminDashboardRDO(
				users(from, to),
				teachers(),
				tests(),
				payments(from, to),
				withdrawals(from, to),
				aiUsage(from, to),
				queues(),
				recentActivity());
		return new AdminDashboard(data, asOf, window(from, to));
	}

	private void requireValidRange(OffsetDateTime from, OffsetDateTime to) {
		if (from != null && to != null && to.isBefore(from)) {
			throw new BusinessException(
					"VALIDATION_FAILED",
					"The to filter must not be before from.",
					HttpStatus.BAD_REQUEST);
		}
	}

	private AdminDashboardUsersRDO users(OffsetDateTime from, OffsetDateTime to) {
		Map<String, Long> counts = countsByKey(userRepository.countGroupedByStatus());
		return new AdminDashboardUsersRDO(
				total(counts),
				counts.getOrDefault(UserStatuses.ACTIVE, 0L),
				counts.getOrDefault(UserStatuses.SUSPENDED, 0L),
				counts.getOrDefault(UserStatuses.BANNED, 0L),
				counts.getOrDefault(UserStatuses.DELETED, 0L),
				userRepository.countRegisteredInWindow(from, to));
	}

	private AdminDashboardTeachersRDO teachers() {
		Map<String, Long> counts = countsByKey(teacherProfileRepository.countGroupedByStatus());
		return new AdminDashboardTeachersRDO(
				total(counts),
				counts.getOrDefault(TeacherProfileStatuses.DRAFT, 0L),
				counts.getOrDefault(TeacherProfileStatuses.PENDING_APPROVAL, 0L),
				counts.getOrDefault(TeacherProfileStatuses.APPROVED, 0L),
				counts.getOrDefault(TeacherProfileStatuses.REJECTED, 0L),
				counts.getOrDefault(TeacherProfileStatuses.SUSPENDED, 0L));
	}

	private AdminDashboardTestsRDO tests() {
		Map<String, Long> counts = countsByKey(testCatalogRepository.countGroupedByVisibility());
		return new AdminDashboardTestsRDO(
				total(counts),
				counts.getOrDefault(TestVisibilities.DRAFT, 0L),
				counts.getOrDefault(TestVisibilities.SUBMITTED, 0L),
				counts.getOrDefault(TestVisibilities.PUBLISHED, 0L),
				counts.getOrDefault(TestVisibilities.REJECTED, 0L),
				counts.getOrDefault(TestVisibilities.ARCHIVED, 0L));
	}

	private AdminDashboardPaymentsRDO payments(OffsetDateTime from, OffsetDateTime to) {
		Map<String, Long> counts = countsByKey(paymentRepository.countGroupedByStatusInWindow(from, to));
		return new AdminDashboardPaymentsRDO(
				total(counts),
				counts.getOrDefault(PaymentStatuses.PENDING, 0L),
				counts.getOrDefault(PaymentStatuses.PAID, 0L),
				counts.getOrDefault(PaymentStatuses.FAILED, 0L),
				counts.getOrDefault(PaymentStatuses.CANCELLED, 0L),
				counts.getOrDefault(PaymentStatuses.REFUNDED, 0L),
				paymentRepository.countByFailureCodeInWindow(Payment.FAILURE_CODE_AMOUNT_MISMATCH, from, to),
				paymentRepository.sumAmountByStatusInWindow(PaymentStatuses.PAID, from, to));
	}

	private AdminDashboardWithdrawalsRDO withdrawals(OffsetDateTime from, OffsetDateTime to) {
		Map<String, Long> counts = countsByKey(withdrawalRequestRepository.countGroupedByStatusInWindow(from, to));
		return new AdminDashboardWithdrawalsRDO(
				total(counts),
				counts.getOrDefault(WithdrawalRequestStatuses.REQUESTED, 0L),
				counts.getOrDefault(WithdrawalRequestStatuses.APPROVED, 0L),
				counts.getOrDefault(WithdrawalRequestStatuses.REJECTED, 0L),
				counts.getOrDefault(WithdrawalRequestStatuses.PAID, 0L),
				counts.getOrDefault(WithdrawalRequestStatuses.CANCELLED, 0L),
				withdrawalRequestRepository.sumRequestedAmountInWindow(from, to));
	}

	private AdminAiUsageReportTotalsRDO aiUsage(OffsetDateTime from, OffsetDateTime to) {
		return aiUsageService.summarizeReport(new AiUsageReportCriteria(null, null, null, null, from, to));
	}

	private AdminDashboardQueuesRDO queues() {
		return new AdminDashboardQueuesRDO(
				teacherProfileRepository.countByStatus(TeacherProfileStatuses.PENDING_APPROVAL),
				testCatalogRepository.countByVisibilityAndDeletedAtIsNull(TestVisibilities.SUBMITTED),
				withdrawalRequestRepository.countByStatus(WithdrawalRequestStatuses.REQUESTED),
				paymentRepository.countByStatus(PaymentStatuses.FAILED),
				paymentRepository.countByFailureCode(Payment.FAILURE_CODE_AMOUNT_MISMATCH));
	}

	private List<AdminDashboardActivityRDO> recentActivity() {
		List<AdminDashboardActivityRDO> activity = new ArrayList<>();
		userRepository.findTop5ByOrderByCreatedAtDescIdDesc().stream()
				.map(mapper::toUserRegisteredActivity)
				.forEach(activity::add);
		testCatalogRepository
				.findTop5ByVisibilityAndDeletedAtIsNullOrderByUpdatedAtDescIdDesc(TestVisibilities.SUBMITTED)
				.stream()
				.map(mapper::toTestSubmittedActivity)
				.forEach(activity::add);
		paymentRepository.findTop5ByStatusOrderByCreatedAtDescIdDesc(PaymentStatuses.PAID).stream()
				.map(mapper::toPaymentPaidActivity)
				.forEach(activity::add);
		withdrawalRequestRepository
				.findTop5ByStatusOrderByCreatedAtDescIdDesc(WithdrawalRequestStatuses.REQUESTED)
				.stream()
				.map(mapper::toWithdrawalRequestedActivity)
				.forEach(activity::add);
		return activity.stream()
				.sorted(Comparator.comparing(
						AdminDashboardActivityRDO::occurredAt,
						Comparator.nullsLast(Comparator.reverseOrder())))
				.limit(RECENT_ACTIVITY_LIMIT)
				.toList();
	}

	private Map<String, Long> countsByKey(List<AdminDashboardStatusCountRDO> counts) {
		Map<String, Long> countsByKey = new LinkedHashMap<>();
		for (AdminDashboardStatusCountRDO count : counts) {
			countsByKey.put(count.status(), count.count());
		}
		return countsByKey;
	}

	private long total(Map<String, Long> counts) {
		return counts.values().stream().mapToLong(Long::longValue).sum();
	}

	private Map<String, Object> window(OffsetDateTime from, OffsetDateTime to) {
		Map<String, Object> window = new LinkedHashMap<>();
		if (from != null) {
			window.put("from", from);
		}
		if (to != null) {
			window.put("to", to);
		}
		return window;
	}
}
