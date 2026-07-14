package com.mockexams.backend.service.impl;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mockexams.backend.dto.NotificationCDO;
import com.mockexams.backend.dto.NotificationRDO;
import com.mockexams.backend.entity.Notification;
import com.mockexams.backend.entity.NotificationTypes;
import com.mockexams.backend.entity.PaymentDisputeStatuses;
import com.mockexams.backend.entity.TestVisibilities;
import com.mockexams.backend.entity.WithdrawalRequestStatuses;
import com.mockexams.backend.error.BusinessException;
import com.mockexams.backend.mapper.NotificationMapper;
import com.mockexams.backend.repository.NotificationRepository;
import com.mockexams.backend.service.NotificationService;
import com.mockexams.backend.teachers.TeacherProfileStatuses;
import com.mockexams.backend.users.UserStatuses;
import com.mockexams.backend.users.entity.User;
import com.mockexams.backend.users.repository.UserRepository;

@Service
class NotificationServiceImpl implements NotificationService {

	private final NotificationRepository notificationRepository;
	private final UserRepository userRepository;
	private final NotificationMapper notificationMapper;

	NotificationServiceImpl(
			NotificationRepository notificationRepository,
			UserRepository userRepository,
			NotificationMapper notificationMapper
	) {
		this.notificationRepository = notificationRepository;
		this.userRepository = userRepository;
		this.notificationMapper = notificationMapper;
	}

	@Override
	@Transactional
	public NotificationRDO create(NotificationCDO command) {
		validate(command);
		User user = userRepository.findById(command.userId())
				.orElseThrow(() -> new BusinessException("NOT_FOUND", "Notification recipient was not found.", HttpStatus.NOT_FOUND));
		Notification notification = new Notification(
				user,
				command.type().trim(),
				command.title().trim(),
				command.body().trim(),
				command.payload());
		return notificationMapper.toRDO(notificationRepository.saveAndFlush(notification));
	}

	@Override
	@Transactional
	public NotificationRDO notifyTeacherProfileApproved(Long teacherUserId, Long teacherProfileId) {
		return create(new NotificationCDO(
				teacherUserId,
				NotificationTypes.TEST_MODERATION,
				"Teacher profile approved",
				"Your teacher profile has been approved. You can now publish teacher content.",
				Map.of(
						"teacherProfileId", teacherProfileId,
						"status", "approved",
						"targetPath", "/teacher/profile")));
	}

	@Override
	@Transactional
	public NotificationRDO notifyTeacherProfileRejected(Long teacherUserId, Long teacherProfileId, String rejectionReason) {
		return create(new NotificationCDO(
				teacherUserId,
				NotificationTypes.TEST_MODERATION,
				"Teacher profile rejected",
				"Your teacher profile was rejected. Please update your profile and submit it again.",
				Map.of(
						"teacherProfileId", teacherProfileId,
						"status", "rejected",
						"rejectionReason", rejectionReason,
						"targetPath", "/teacher/profile")));
	}

	@Override
	@Transactional
	public NotificationRDO notifyWithdrawalStatus(
			Long teacherUserId,
			Long withdrawalRequestId,
			String status,
			BigDecimal amount,
			String currency,
			String rejectionReason
	) {
		String amountLabel = amount.toPlainString() + " " + currency;
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("withdrawalRequestId", withdrawalRequestId);
		payload.put("status", status);
		payload.put("amount", amount.toPlainString());
		payload.put("currency", currency);
		if (rejectionReason != null && !rejectionReason.isBlank()) {
			payload.put("rejectionReason", rejectionReason);
		}
		payload.put("targetPath", "/teacher/wallet");
		return create(new NotificationCDO(
				teacherUserId,
				NotificationTypes.WITHDRAWAL,
				withdrawalTitle(status),
				withdrawalBody(status, amountLabel),
				payload));
	}

	@Override
	@Transactional
	public NotificationRDO notifyTestModerationDecision(
			Long teacherUserId,
			Long testId,
			String testTitle,
			String decision,
			String rejectionReason
	) {
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("testId", testId);
		payload.put("status", decision);
		if (rejectionReason != null && !rejectionReason.isBlank()) {
			payload.put("rejectionReason", rejectionReason);
		}
		payload.put("targetPath", "/teacher/tests/" + testId);
		return create(new NotificationCDO(
				teacherUserId,
				NotificationTypes.TEST_MODERATION,
				testModerationTitle(decision),
				testModerationBody(decision, testTitle),
				payload));
	}

	private String testModerationTitle(String decision) {
		return switch (decision) {
			case TestVisibilities.PUBLISHED -> "Test approved";
			case TestVisibilities.REJECTED -> "Test rejected";
			default -> throw validationFailed("Unsupported test moderation notification decision.");
		};
	}

	private String testModerationBody(String decision, String testTitle) {
		return switch (decision) {
			case TestVisibilities.PUBLISHED ->
					"Your test \"" + testTitle + "\" was approved and is now visible in the public catalog.";
			case TestVisibilities.REJECTED ->
					"Your test \"" + testTitle + "\" was rejected. Please review the moderation note and resubmit.";
			default -> throw validationFailed("Unsupported test moderation notification decision.");
		};
	}

	private String withdrawalTitle(String status) {
		return switch (status) {
			case WithdrawalRequestStatuses.REQUESTED -> "Withdrawal request submitted";
			case WithdrawalRequestStatuses.APPROVED -> "Withdrawal approved";
			case WithdrawalRequestStatuses.REJECTED -> "Withdrawal rejected";
			case WithdrawalRequestStatuses.PAID -> "Withdrawal paid";
			case WithdrawalRequestStatuses.CANCELLED -> "Withdrawal cancelled";
			default -> throw validationFailed("Unsupported withdrawal notification status.");
		};
	}

	private String withdrawalBody(String status, String amountLabel) {
		return switch (status) {
			case WithdrawalRequestStatuses.REQUESTED ->
					"Your withdrawal request for " + amountLabel + " was submitted and is awaiting review.";
			case WithdrawalRequestStatuses.APPROVED ->
					"Your withdrawal request for " + amountLabel + " was approved.";
			case WithdrawalRequestStatuses.REJECTED ->
					"Your withdrawal request for " + amountLabel + " was rejected.";
			case WithdrawalRequestStatuses.PAID ->
					"Your withdrawal request for " + amountLabel + " was paid out.";
			case WithdrawalRequestStatuses.CANCELLED ->
					"Your withdrawal request for " + amountLabel + " was cancelled.";
			default -> throw validationFailed("Unsupported withdrawal notification status.");
		};
	}

	@Override
	@Transactional
	public NotificationRDO notifyAccountStatusChange(Long userId, String status, String reason) {
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("status", status);
		if (reason != null && !reason.isBlank()) {
			payload.put("reason", reason);
		}
		payload.put("targetPath", "/account");
		return create(new NotificationCDO(
				userId,
				NotificationTypes.ACCOUNT_STATUS,
				accountStatusTitle(status),
				accountStatusBody(status, reason),
				payload));
	}

	@Override
	@Transactional
	public NotificationRDO notifyTeacherProfileStatusChange(
			Long teacherUserId,
			Long teacherProfileId,
			String status,
			String reason
	) {
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("teacherProfileId", teacherProfileId);
		payload.put("status", status);
		if (reason != null && !reason.isBlank()) {
			payload.put("reason", reason);
		}
		payload.put("targetPath", "/teacher/profile");
		return create(new NotificationCDO(
				teacherUserId,
				NotificationTypes.ACCOUNT_STATUS,
				teacherProfileStatusTitle(status),
				teacherProfileStatusBody(status, reason),
				payload));
	}

	private String accountStatusTitle(String status) {
		return switch (status) {
			case UserStatuses.ACTIVE -> "Account reactivated";
			case UserStatuses.SUSPENDED -> "Account suspended";
			case UserStatuses.BANNED -> "Account banned";
			case UserStatuses.DELETED -> "Account deleted";
			default -> throw validationFailed("Unsupported account status notification.");
		};
	}

	private String accountStatusBody(String status, String reason) {
		String suffix = (reason != null && !reason.isBlank()) ? " Reason: " + reason + "." : "";
		return switch (status) {
			case UserStatuses.ACTIVE -> "Your account has been reactivated. You can sign in again." + suffix;
			case UserStatuses.SUSPENDED -> "Your account has been suspended. Access is temporarily restricted." + suffix;
			case UserStatuses.BANNED -> "Your account has been banned. Access has been permanently revoked." + suffix;
			case UserStatuses.DELETED -> "Your account has been deleted." + suffix;
			default -> throw validationFailed("Unsupported account status notification.");
		};
	}

	private String teacherProfileStatusTitle(String status) {
		return switch (status) {
			case TeacherProfileStatuses.SUSPENDED -> "Teacher profile suspended";
			case TeacherProfileStatuses.APPROVED -> "Teacher profile reactivated";
			default -> throw validationFailed("Unsupported teacher profile status notification.");
		};
	}

	private String teacherProfileStatusBody(String status, String reason) {
		String suffix = (reason != null && !reason.isBlank()) ? " Reason: " + reason + "." : "";
		return switch (status) {
			case TeacherProfileStatuses.SUSPENDED ->
					"Your teacher profile has been suspended. Your marketplace content is no longer publicly visible." + suffix;
			case TeacherProfileStatuses.APPROVED ->
					"Your teacher profile has been reactivated. Your marketplace content is visible again." + suffix;
			default -> throw validationFailed("Unsupported teacher profile status notification.");
		};
	}

	@Override
	@Transactional
	public NotificationRDO notifyPaymentDisputeStatus(
			Long studentUserId,
			Long disputeId,
			Long paymentId,
			String status,
			String resolutionNote
	) {
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("disputeId", disputeId);
		payload.put("paymentId", paymentId);
		payload.put("status", status);
		if (resolutionNote != null && !resolutionNote.isBlank()) {
			payload.put("resolutionNote", resolutionNote);
		}
		payload.put("targetPath", "/student/payments/" + paymentId + "/disputes/" + disputeId);
		return create(new NotificationCDO(
				studentUserId,
				NotificationTypes.PAYMENT,
				paymentDisputeTitle(status),
				paymentDisputeBody(status, resolutionNote),
				payload));
	}

	private String paymentDisputeTitle(String status) {
		return switch (status) {
			case PaymentDisputeStatuses.OPEN -> "Payment dispute opened";
			case PaymentDisputeStatuses.UNDER_REVIEW -> "Payment dispute under review";
			case PaymentDisputeStatuses.REFUNDED -> "Payment dispute resolved with a refund marker";
			case PaymentDisputeStatuses.REJECTED -> "Payment dispute rejected";
			case PaymentDisputeStatuses.RESOLVED -> "Payment dispute resolved";
			default -> throw validationFailed("Unsupported payment dispute notification status.");
		};
	}

	private String paymentDisputeBody(String status, String resolutionNote) {
		String suffix = (resolutionNote != null && !resolutionNote.isBlank()) ? " Note: " + resolutionNote + "." : "";
		return switch (status) {
			case PaymentDisputeStatuses.OPEN ->
					"Your payment dispute was submitted and is awaiting review." + suffix;
			case PaymentDisputeStatuses.UNDER_REVIEW ->
					"Your payment dispute is now under review by support." + suffix;
			case PaymentDisputeStatuses.REFUNDED ->
					"Your payment dispute was resolved and marked as refunded." + suffix;
			case PaymentDisputeStatuses.REJECTED ->
					"Your payment dispute was reviewed and rejected." + suffix;
			case PaymentDisputeStatuses.RESOLVED ->
					"Your payment dispute was resolved." + suffix;
			default -> throw validationFailed("Unsupported payment dispute notification status.");
		};
	}

	@Override
	@Transactional(readOnly = true)
	public Page<NotificationRDO> listForUser(Long userId, Pageable pageable) {
		return notificationRepository.findByUserId(userId, pageable)
				.map(notificationMapper::toRDO);
	}

	@Override
	@Transactional
	public NotificationRDO markRead(Long userId, Long notificationId) {
		Notification notification = notificationRepository.findByIdAndUserId(notificationId, userId)
				.orElseThrow(this::notFound);
		if (notification.markRead(OffsetDateTime.now(ZoneOffset.UTC))) {
			notificationRepository.saveAndFlush(notification);
		}
		return notificationMapper.toRDO(notification);
	}

	@Override
	@Transactional(readOnly = true)
	public long unreadCountForUser(Long userId) {
		return notificationRepository.countByUserIdAndReadAtIsNull(userId);
	}

	private void validate(NotificationCDO command) {
		if (command.userId() == null) {
			throw validationFailed("Notification recipient is required.");
		}
		if (command.type() == null || !NotificationTypes.isSupported(command.type().trim())) {
			throw validationFailed("Unsupported notification type.");
		}
		if (command.title() == null || command.title().isBlank() || command.title().trim().length() > 160) {
			throw validationFailed("Notification title is required and must be at most 160 characters.");
		}
		if (command.body() == null || command.body().isBlank()) {
			throw validationFailed("Notification body is required.");
		}
	}

	private BusinessException notFound() {
		return new BusinessException("NOTIFICATION_NOT_FOUND", "Notification was not found.", HttpStatus.NOT_FOUND);
	}

	private BusinessException validationFailed(String message) {
		return new BusinessException("VALIDATION_FAILED", message, HttpStatus.BAD_REQUEST);
	}
}
