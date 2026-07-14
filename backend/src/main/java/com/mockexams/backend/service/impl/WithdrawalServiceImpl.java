package com.mockexams.backend.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mockexams.backend.dto.AdminWithdrawalRequestRDO;
import com.mockexams.backend.dto.WithdrawalRequestCDO;
import com.mockexams.backend.dto.WithdrawalRequestRDO;
import com.mockexams.backend.entity.WithdrawalRequest;
import com.mockexams.backend.entity.WithdrawalRequestStatuses;
import com.mockexams.backend.error.BusinessException;
import com.mockexams.backend.mapper.WithdrawalRequestMapper;
import com.mockexams.backend.repository.WithdrawalRequestRepository;
import com.mockexams.backend.service.NotificationService;
import com.mockexams.backend.service.PlatformSettingService;
import com.mockexams.backend.service.TeacherWalletService;
import com.mockexams.backend.service.WithdrawalService;
import com.mockexams.backend.teachers.entity.TeacherProfile;
import com.mockexams.backend.teachers.repository.TeacherProfileRepository;
import com.mockexams.backend.users.entity.User;

@Service
class WithdrawalServiceImpl implements WithdrawalService {

	private static final String PAYOUT_METHOD_CARD = "card";
	private static final String PAYOUT_METHOD_BANK_TRANSFER = "bank_transfer";
	private static final Set<String> CARD_PAYOUT_KEYS = Set.of("method", "cardNumber", "cardHolder");
	private static final Set<String> BANK_PAYOUT_KEYS = Set.of("method", "bankName", "accountNumber", "accountHolder");
	private static final Pattern CARD_NUMBER_PATTERN = Pattern.compile("\\d{12,19}");
	private static final Pattern ACCOUNT_NUMBER_PATTERN = Pattern.compile("[A-Za-z0-9]{6,34}");
	private static final int MAX_PAYOUT_TEXT_LENGTH = 160;

	private final WithdrawalRequestRepository withdrawalRequestRepository;
	private final TeacherProfileRepository teacherProfileRepository;
	private final PlatformSettingService platformSettingService;
	private final TeacherWalletService teacherWalletService;
	private final NotificationService notificationService;
	private final WithdrawalRequestMapper mapper;

	WithdrawalServiceImpl(
			WithdrawalRequestRepository withdrawalRequestRepository,
			TeacherProfileRepository teacherProfileRepository,
			PlatformSettingService platformSettingService,
			TeacherWalletService teacherWalletService,
			NotificationService notificationService,
			WithdrawalRequestMapper mapper
	) {
		this.withdrawalRequestRepository = withdrawalRequestRepository;
		this.teacherProfileRepository = teacherProfileRepository;
		this.platformSettingService = platformSettingService;
		this.teacherWalletService = teacherWalletService;
		this.notificationService = notificationService;
		this.mapper = mapper;
	}

	@Override
	@Transactional
	public WithdrawalRequestRDO create(User teacher, WithdrawalRequestCDO command) {
		TeacherProfile profile = requireTeacherProfile(teacher);
		BigDecimal amount = positiveMoney(command.amount());
		Map<String, Object> payoutDetails = validatedPayoutDetails(command.payoutDetails());
		String currency = resolveCurrency(teacher, command.currency());
		BigDecimal minimumAmount = minimumWithdrawalAmount();
		if (amount.compareTo(minimumAmount) < 0) {
			throw new BusinessException(
					"WITHDRAWAL_BELOW_MINIMUM",
					"Withdrawal amount is below the platform minimum of " + minimumAmount.toPlainString() + ".",
					HttpStatus.BAD_REQUEST);
		}
		WithdrawalRequest request = withdrawalRequestRepository.save(
				WithdrawalRequest.create(profile, amount, currency, payoutDetails));
		teacherWalletService.reserveWithdrawal(profile.getId(), amount, currency, sourceReference(request));
		notifyTeacher(request);
		return mapper.toRDO(request);
	}

	@Override
	@Transactional(readOnly = true)
	public Page<WithdrawalRequestRDO> myWithdrawals(User teacher, String status, Pageable pageable) {
		TeacherProfile profile = requireTeacherProfile(teacher);
		String normalizedStatus = normalizedStatusFilter(status);
		Page<WithdrawalRequest> withdrawals = normalizedStatus == null
				? withdrawalRequestRepository.findByTeacherProfile_Id(profile.getId(), pageable)
				: withdrawalRequestRepository.findByTeacherProfile_IdAndStatus(profile.getId(), normalizedStatus, pageable);
		return withdrawals.map(mapper::toRDO);
	}

	@Override
	@Transactional(readOnly = true)
	public WithdrawalRequestRDO myWithdrawal(User teacher, Long withdrawalRequestId) {
		TeacherProfile profile = requireTeacherProfile(teacher);
		return mapper.toRDO(requireOwnWithdrawal(profile, withdrawalRequestId));
	}

	@Override
	@Transactional
	public WithdrawalRequestRDO cancel(User teacher, Long withdrawalRequestId) {
		TeacherProfile profile = requireTeacherProfile(teacher);
		WithdrawalRequest request = requireOwnWithdrawal(profile, withdrawalRequestId);
		requireStatus(request, WithdrawalRequestStatuses.REQUESTED);
		teacherWalletService.releaseWithdrawalReservation(
				profile.getId(),
				request.getRequestedAmount(),
				request.getCurrency());
		request.markCancelled();
		notifyTeacher(request);
		return mapper.toRDO(request);
	}

	@Override
	@Transactional(readOnly = true)
	public Page<AdminWithdrawalRequestRDO> adminQueue(String status, Long teacherProfileId, Pageable pageable) {
		return withdrawalRequestRepository.adminQueue(normalizedStatusFilter(status), teacherProfileId, pageable)
				.map(mapper::toAdminRDO);
	}

	@Override
	@Transactional(readOnly = true)
	public AdminWithdrawalRequestRDO adminWithdrawal(Long withdrawalRequestId) {
		return mapper.toAdminRDO(requireWithdrawal(withdrawalRequestId));
	}

	@Override
	@Transactional
	public AdminWithdrawalRequestRDO approve(User admin, Long withdrawalRequestId) {
		WithdrawalRequest request = requireWithdrawal(withdrawalRequestId);
		requireStatus(request, WithdrawalRequestStatuses.REQUESTED);
		try {
			teacherWalletService.recordApprovedWithdrawal(
					request.getTeacherProfile().getId(),
					request.getRequestedAmount(),
					request.getCurrency(),
					sourceReference(request));
		}
		catch (BusinessException ex) {
			// A concurrent approval for the same withdrawal already settled the wallet ledger
			// transaction; this request is no longer approvable, so surface the same stable status
			// conflict a duplicate approve attempt would get instead of a wallet-internal code.
			if ("WALLET_LEDGER_ALREADY_SETTLED".equals(ex.getCode())) {
				throw invalidStatus();
			}
			throw ex;
		}
		catch (DataIntegrityViolationException ex) {
			// Defense in depth: if the unique ledger constraint is still hit despite the wallet-side
			// idempotency re-check (e.g. a future change removes it), never surface a raw 500/generic
			// conflict for a business-level double-approve race.
			throw invalidStatus();
		}
		request.markApproved(admin.getId(), now());
		notifyTeacher(request);
		return mapper.toAdminRDO(request);
	}

	@Override
	@Transactional
	public AdminWithdrawalRequestRDO reject(User admin, Long withdrawalRequestId, String reason) {
		String normalizedReason = trimToNull(reason);
		if (normalizedReason == null) {
			throw validationFailed("Rejection reason is required.");
		}
		WithdrawalRequest request = requireWithdrawal(withdrawalRequestId);
		requireStatus(request, WithdrawalRequestStatuses.REQUESTED);
		teacherWalletService.releaseWithdrawalReservation(
				request.getTeacherProfile().getId(),
				request.getRequestedAmount(),
				request.getCurrency());
		request.markRejected(admin.getId(), now(), normalizedReason);
		notifyTeacher(request);
		return mapper.toAdminRDO(request);
	}

	@Override
	@Transactional
	public AdminWithdrawalRequestRDO markPaid(User admin, Long withdrawalRequestId) {
		WithdrawalRequest request = requireWithdrawal(withdrawalRequestId);
		requireStatus(request, WithdrawalRequestStatuses.APPROVED);
		request.markPaid(now());
		notifyTeacher(request);
		return mapper.toAdminRDO(request);
	}

	private void notifyTeacher(WithdrawalRequest request) {
		notificationService.notifyWithdrawalStatus(
				request.getTeacherProfile().getUser().getId(),
				request.getId(),
				request.getStatus(),
				request.getRequestedAmount(),
				request.getCurrency(),
				request.getRejectionReason());
	}

	private TeacherProfile requireTeacherProfile(User teacher) {
		if (teacher == null || teacher.getId() == null) {
			throw teacherProfileMissing();
		}
		return teacherProfileRepository.findByUserId(teacher.getId()).orElseThrow(this::teacherProfileMissing);
	}

	private WithdrawalRequest requireOwnWithdrawal(TeacherProfile profile, Long withdrawalRequestId) {
		return withdrawalRequestRepository.findByIdAndTeacherProfile_Id(withdrawalRequestId, profile.getId())
				.orElseThrow(this::withdrawalNotFound);
	}

	private WithdrawalRequest requireWithdrawal(Long withdrawalRequestId) {
		return withdrawalRequestRepository.findById(withdrawalRequestId).orElseThrow(this::withdrawalNotFound);
	}

	private void requireStatus(WithdrawalRequest request, String expectedStatus) {
		if (!expectedStatus.equals(request.getStatus())) {
			throw invalidStatus();
		}
	}

	private BusinessException invalidStatus() {
		return new BusinessException(
				"WITHDRAWAL_INVALID_STATUS",
				"Withdrawal request status does not allow this operation.",
				HttpStatus.CONFLICT);
	}

	private String normalizedStatusFilter(String status) {
		String normalized = trimToNull(status);
		if (normalized == null) {
			return null;
		}
		normalized = normalized.toLowerCase(Locale.ROOT);
		if (!WithdrawalRequestStatuses.isSupported(normalized)) {
			throw validationFailed("Unsupported withdrawal status filter.");
		}
		return normalized;
	}

	private String resolveCurrency(User teacher, String currency) {
		String normalized = trimToNull(currency);
		if (normalized == null) {
			return teacherWalletService.myWallet(teacher).currency();
		}
		if (normalized.length() != 3) {
			throw validationFailed("Currency is invalid.");
		}
		return normalized.toUpperCase(Locale.ROOT);
	}

	private BigDecimal minimumWithdrawalAmount() {
		return platformSettingService.withdrawalMinimumAmount().setScale(2, RoundingMode.HALF_UP);
	}

	private Map<String, Object> validatedPayoutDetails(Map<String, Object> payoutDetails) {
		if (payoutDetails == null || payoutDetails.isEmpty()) {
			throw payoutDetailsInvalid("Payout details are required.");
		}
		String method = payoutTextField(payoutDetails, "method");
		if (PAYOUT_METHOD_CARD.equals(method)) {
			requireExactKeys(payoutDetails, CARD_PAYOUT_KEYS);
			String cardNumber = payoutTextField(payoutDetails, "cardNumber").replace(" ", "");
			if (!CARD_NUMBER_PATTERN.matcher(cardNumber).matches()) {
				throw payoutDetailsInvalid("Card number must contain 12 to 19 digits.");
			}
			String cardHolder = payoutTextField(payoutDetails, "cardHolder");
			Map<String, Object> normalized = new LinkedHashMap<>();
			normalized.put("method", PAYOUT_METHOD_CARD);
			normalized.put("cardNumber", cardNumber);
			normalized.put("cardHolder", cardHolder);
			return normalized;
		}
		if (PAYOUT_METHOD_BANK_TRANSFER.equals(method)) {
			requireExactKeys(payoutDetails, BANK_PAYOUT_KEYS);
			String bankName = payoutTextField(payoutDetails, "bankName");
			String accountNumber = payoutTextField(payoutDetails, "accountNumber").replace(" ", "");
			if (!ACCOUNT_NUMBER_PATTERN.matcher(accountNumber).matches()) {
				throw payoutDetailsInvalid("Account number must contain 6 to 34 letters or digits.");
			}
			String accountHolder = payoutTextField(payoutDetails, "accountHolder");
			Map<String, Object> normalized = new LinkedHashMap<>();
			normalized.put("method", PAYOUT_METHOD_BANK_TRANSFER);
			normalized.put("bankName", bankName);
			normalized.put("accountNumber", accountNumber);
			normalized.put("accountHolder", accountHolder);
			return normalized;
		}
		throw payoutDetailsInvalid("Payout method must be card or bank_transfer.");
	}

	private void requireExactKeys(Map<String, Object> payoutDetails, Set<String> allowedKeys) {
		if (!allowedKeys.containsAll(payoutDetails.keySet())) {
			throw payoutDetailsInvalid("Payout details contain unsupported fields.");
		}
	}

	private String payoutTextField(Map<String, Object> payoutDetails, String key) {
		Object value = payoutDetails.get(key);
		if (!(value instanceof String text) || text.isBlank() || text.trim().length() > MAX_PAYOUT_TEXT_LENGTH) {
			throw payoutDetailsInvalid("Payout details field '" + key + "' is missing or invalid.");
		}
		return text.trim();
	}

	private String sourceReference(WithdrawalRequest request) {
		return "withdrawal:" + request.getId();
	}

	private BigDecimal positiveMoney(BigDecimal amount) {
		if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
			throw validationFailed("Withdrawal amount must be greater than zero.");
		}
		return amount.setScale(2, RoundingMode.HALF_UP);
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

	private BusinessException payoutDetailsInvalid(String message) {
		return new BusinessException("WITHDRAWAL_PAYOUT_DETAILS_INVALID", message, HttpStatus.BAD_REQUEST);
	}

	private BusinessException withdrawalNotFound() {
		return new BusinessException("WITHDRAWAL_NOT_FOUND", "Withdrawal request was not found.", HttpStatus.NOT_FOUND);
	}

	private BusinessException teacherProfileMissing() {
		return new BusinessException("TEACHER_PROFILE_NOT_FOUND", "Teacher profile was not found.", HttpStatus.NOT_FOUND);
	}

}
