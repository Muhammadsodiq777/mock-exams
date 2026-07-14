package com.mockexams.backend.service;

import java.math.BigDecimal;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.mockexams.backend.dto.NotificationCDO;
import com.mockexams.backend.dto.NotificationRDO;

public interface NotificationService {

	NotificationRDO create(NotificationCDO command);

	NotificationRDO notifyTeacherProfileApproved(Long teacherUserId, Long teacherProfileId);

	NotificationRDO notifyTeacherProfileRejected(Long teacherUserId, Long teacherProfileId, String rejectionReason);

	NotificationRDO notifyWithdrawalStatus(
			Long teacherUserId,
			Long withdrawalRequestId,
			String status,
			BigDecimal amount,
			String currency,
			String rejectionReason);

	NotificationRDO notifyTestModerationDecision(
			Long teacherUserId,
			Long testId,
			String testTitle,
			String decision,
			String rejectionReason);

	NotificationRDO notifyAccountStatusChange(Long userId, String status, String reason);

	NotificationRDO notifyTeacherProfileStatusChange(Long teacherUserId, Long teacherProfileId, String status, String reason);

	NotificationRDO notifyPaymentDisputeStatus(
			Long studentUserId,
			Long disputeId,
			Long paymentId,
			String status,
			String resolutionNote);

	Page<NotificationRDO> listForUser(Long userId, Pageable pageable);

	NotificationRDO markRead(Long userId, Long notificationId);

	long unreadCountForUser(Long userId);
}
