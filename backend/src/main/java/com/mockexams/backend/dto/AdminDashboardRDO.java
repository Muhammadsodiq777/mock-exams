package com.mockexams.backend.dto;

import java.util.List;

public record AdminDashboardRDO(
		AdminDashboardUsersRDO users,
		AdminDashboardTeachersRDO teachers,
		AdminDashboardTestsRDO tests,
		AdminDashboardPaymentsRDO payments,
		AdminDashboardWithdrawalsRDO withdrawals,
		AdminAiUsageReportTotalsRDO aiUsage,
		AdminDashboardQueuesRDO queues,
		List<AdminDashboardActivityRDO> recentActivity
) {
	public AdminDashboardRDO {
		if (recentActivity == null) {
			recentActivity = List.of();
		}
		else {
			recentActivity = List.copyOf(recentActivity);
		}
	}
}
