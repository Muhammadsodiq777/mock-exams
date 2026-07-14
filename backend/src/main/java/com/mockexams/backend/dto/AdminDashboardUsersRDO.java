package com.mockexams.backend.dto;

public record AdminDashboardUsersRDO(
		long total,
		long active,
		long suspended,
		long banned,
		long deleted,
		long newInWindow
) {
}
