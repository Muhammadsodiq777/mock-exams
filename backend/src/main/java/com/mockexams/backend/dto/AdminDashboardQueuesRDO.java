package com.mockexams.backend.dto;

public record AdminDashboardQueuesRDO(
		long pendingTeacherApprovals,
		long pendingTestModeration,
		long pendingWithdrawals,
		long failedPayments,
		long amountMismatchPayments
) {
}
