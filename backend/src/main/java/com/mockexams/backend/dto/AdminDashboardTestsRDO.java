package com.mockexams.backend.dto;

public record AdminDashboardTestsRDO(
		long total,
		long draft,
		long submitted,
		long published,
		long rejected,
		long archived
) {
}
