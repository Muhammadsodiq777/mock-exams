package com.mockexams.backend.dto;

public record AdminDashboardTeachersRDO(
		long total,
		long draft,
		long pendingApproval,
		long approved,
		long rejected,
		long suspended
) {
}
