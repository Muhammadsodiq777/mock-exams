package com.mockexams.backend.service.impl;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Locale;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mockexams.backend.dto.AdminPaymentDisputeRDO;
import com.mockexams.backend.dto.PaymentDisputeCDO;
import com.mockexams.backend.dto.PaymentDisputeDecisionCDO;
import com.mockexams.backend.dto.PaymentDisputeRDO;
import com.mockexams.backend.entity.Payment;
import com.mockexams.backend.entity.PaymentDispute;
import com.mockexams.backend.entity.PaymentDisputeStatuses;
import com.mockexams.backend.entity.PaymentStatuses;
import com.mockexams.backend.error.BusinessException;
import com.mockexams.backend.mapper.PaymentDisputeMapper;
import com.mockexams.backend.repository.PaymentDisputeRepository;
import com.mockexams.backend.repository.PaymentRepository;
import com.mockexams.backend.service.AdminPaymentDisputeSearchCriteria;
import com.mockexams.backend.service.NotificationService;
import com.mockexams.backend.service.PaymentDisputeService;
import com.mockexams.backend.users.entity.User;

@Service
class PaymentDisputeServiceImpl implements PaymentDisputeService {

	/**
	 * Only payments that reached a terminal provider outcome are disputable: a completed charge
	 * (paid) the student wants investigated, or a charge attempt the student believes failed
	 * incorrectly (failed). Pending payments have no settled outcome yet, and cancelled/already
	 * refunded payments have no student-facing state left to dispute.
	 */
	private static final Set<String> DISPUTABLE_PAYMENT_STATUSES = Set.of(PaymentStatuses.PAID, PaymentStatuses.FAILED);

	/**
	 * A payment may only have one dispute open for investigation at a time. Once a prior dispute
	 * reaches a terminal outcome, a new one may be opened if the student still has a problem.
	 */
	private static final Set<String> NON_TERMINAL_DISPUTE_STATUSES = Set.of(
			PaymentDisputeStatuses.OPEN,
			PaymentDisputeStatuses.UNDER_REVIEW);

	private final PaymentDisputeRepository paymentDisputeRepository;
	private final PaymentRepository paymentRepository;
	private final NotificationService notificationService;
	private final PaymentDisputeMapper mapper;

	PaymentDisputeServiceImpl(
			PaymentDisputeRepository paymentDisputeRepository,
			PaymentRepository paymentRepository,
			NotificationService notificationService,
			PaymentDisputeMapper mapper
	) {
		this.paymentDisputeRepository = paymentDisputeRepository;
		this.paymentRepository = paymentRepository;
		this.notificationService = notificationService;
		this.mapper = mapper;
	}

	@Override
	@Transactional
	public PaymentDisputeRDO create(User student, PaymentDisputeCDO command) {
		// Serialize duplicate checks per payment. A plain unique index would reject valid terminal
		// dispute history, and a partial unique index is not portable across this repo's H2 test
		// database and PostgreSQL runtime.
		Payment payment = paymentRepository.findByIdAndStudentIdForUpdate(command.paymentId(), student.getId())
				.orElseThrow(this::paymentNotFound);
		if (!DISPUTABLE_PAYMENT_STATUSES.contains(payment.getStatus())) {
			throw new BusinessException(
					"PAYMENT_DISPUTE_STATUS_NOT_ALLOWED",
					"Only paid or failed payments can be disputed.",
					HttpStatus.CONFLICT);
		}
		if (paymentDisputeRepository.existsByPayment_IdAndStatusIn(payment.getId(), NON_TERMINAL_DISPUTE_STATUSES)) {
			throw new BusinessException(
					"PAYMENT_DISPUTE_ALREADY_OPEN",
					"This payment already has an open dispute awaiting resolution.",
					HttpStatus.CONFLICT);
		}
		PaymentDispute dispute = PaymentDispute.open(
				payment,
				student,
				command.reason().trim(),
				trimToNull(command.description()));
		dispute = paymentDisputeRepository.saveAndFlush(dispute);
		notify(dispute);
		return mapper.toRDO(dispute);
	}

	@Override
	@Transactional(readOnly = true)
	public Page<PaymentDisputeRDO> myDisputes(User student, String status, Pageable pageable) {
		String normalizedStatus = normalizedStatusFilter(status);
		Page<PaymentDispute> disputes = normalizedStatus == null
				? paymentDisputeRepository.findByOpenedByUser_Id(student.getId(), pageable)
				: paymentDisputeRepository.findByOpenedByUser_IdAndStatus(student.getId(), normalizedStatus, pageable);
		return disputes.map(mapper::toRDO);
	}

	@Override
	@Transactional(readOnly = true)
	public PaymentDisputeRDO myDispute(User student, Long disputeId) {
		return mapper.toRDO(paymentDisputeRepository.findByIdAndOpenedByUser_Id(disputeId, student.getId())
				.orElseThrow(this::disputeNotFound));
	}

	@Override
	@Transactional(readOnly = true)
	public Page<AdminPaymentDisputeRDO> adminQueue(AdminPaymentDisputeSearchCriteria criteria, Pageable pageable) {
		String status = normalizedStatusFilter(criteria.status());
		String paymentStatus = normalizedPaymentStatusFilter(criteria.paymentStatus());
		String reason = trimToNull(criteria.reason());
		return paymentDisputeRepository.adminQueue(
				status,
				paymentStatus,
				criteria.from(),
				criteria.to(),
				criteria.teacherProfileId(),
				criteria.studentUserId(),
				reason,
				pageable).map(mapper::toAdminRDO);
	}

	@Override
	@Transactional(readOnly = true)
	public AdminPaymentDisputeRDO adminDispute(Long disputeId) {
		return mapper.toAdminRDO(requireDispute(disputeId));
	}

	@Override
	@Transactional
	public AdminPaymentDisputeRDO decide(User admin, Long disputeId, PaymentDisputeDecisionCDO command) {
		String targetStatus = normalizedDecisionStatus(command.status());
		PaymentDispute dispute = requireDispute(disputeId);
		if (dispute.isTerminal()) {
			throw invalidTransition();
		}
		String resolutionNote = trimToNull(command.resolutionNote());
		if (PaymentDisputeStatuses.isTerminal(targetStatus) && resolutionNote == null) {
			throw validationFailed("Resolution note is required to resolve, reject, or mark a dispute as refunded.");
		}
		boolean statusChanged = !targetStatus.equals(dispute.getStatus());
		dispute.applyDecision(targetStatus, admin.getId(), now(), resolutionNote);
		if (statusChanged) {
			notify(dispute);
		}
		return mapper.toAdminRDO(dispute);
	}

	private void notify(PaymentDispute dispute) {
		notificationService.notifyPaymentDisputeStatus(
				dispute.getOpenedByUser().getId(),
				dispute.getId(),
				dispute.getPayment().getId(),
				dispute.getStatus(),
				dispute.getResolutionNote());
	}

	private PaymentDispute requireDispute(Long disputeId) {
		return paymentDisputeRepository.findById(disputeId).orElseThrow(this::disputeNotFound);
	}

	private String normalizedStatusFilter(String status) {
		String normalized = trimToNull(status);
		if (normalized == null) {
			return null;
		}
		normalized = normalized.toLowerCase(Locale.ROOT);
		if (!PaymentDisputeStatuses.isSupported(normalized)) {
			throw validationFailed("Unsupported payment dispute status filter.");
		}
		return normalized;
	}

	private String normalizedPaymentStatusFilter(String paymentStatus) {
		String normalized = trimToNull(paymentStatus);
		if (normalized == null) {
			return null;
		}
		normalized = normalized.toLowerCase(Locale.ROOT);
		if (!PaymentStatuses.isSupported(normalized)) {
			throw validationFailed("Unsupported payment status filter.");
		}
		return normalized;
	}

	private String normalizedDecisionStatus(String status) {
		String normalized = trimToNull(status);
		if (normalized == null) {
			throw validationFailed("Dispute decision status is required.");
		}
		normalized = normalized.toLowerCase(Locale.ROOT);
		if (!PaymentDisputeStatuses.isValidDecisionTarget(normalized)) {
			throw validationFailed("Unsupported payment dispute decision status.");
		}
		return normalized;
	}

	private String trimToNull(String value) {
		if (value == null) {
			return null;
		}
		String trimmed = value.trim();
		return trimmed.isBlank() ? null : trimmed;
	}

	private OffsetDateTime now() {
		return OffsetDateTime.now(ZoneOffset.UTC);
	}

	private BusinessException validationFailed(String message) {
		return new BusinessException("VALIDATION_FAILED", message, HttpStatus.BAD_REQUEST);
	}

	private BusinessException paymentNotFound() {
		return new BusinessException("PAYMENT_NOT_FOUND", "Payment was not found.", HttpStatus.NOT_FOUND);
	}

	private BusinessException disputeNotFound() {
		return new BusinessException(
				"PAYMENT_DISPUTE_NOT_FOUND", "Payment dispute was not found.", HttpStatus.NOT_FOUND);
	}

	private BusinessException invalidTransition() {
		return new BusinessException(
				"PAYMENT_DISPUTE_INVALID_STATUS_TRANSITION",
				"Payment dispute is already in a terminal status and cannot be decided again.",
				HttpStatus.CONFLICT);
	}
}
