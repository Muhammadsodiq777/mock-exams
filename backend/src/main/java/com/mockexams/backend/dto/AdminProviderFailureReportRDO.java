package com.mockexams.backend.dto;

import java.time.OffsetDateTime;
import java.util.List;

public record AdminProviderFailureReportRDO(
		OffsetDateTime generatedAt,
		AdminReportFiltersRDO filters,
		AdminProviderFailureReportTotalsRDO totals,
		List<AdminProviderFailureReportRowRDO> rows,
		String liveSpeakingStatus
) {
	public AdminProviderFailureReportRDO {
		rows = rows == null ? List.of() : List.copyOf(rows);
	}
}
