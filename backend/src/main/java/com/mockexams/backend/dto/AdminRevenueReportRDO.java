package com.mockexams.backend.dto;

import java.time.OffsetDateTime;
import java.util.List;

public record AdminRevenueReportRDO(
		OffsetDateTime generatedAt,
		AdminReportFiltersRDO filters,
		AdminRevenueReportTotalsRDO totals,
		List<AdminRevenueReportRowRDO> rows,
		String liveSpeakingStatus
) {
	public AdminRevenueReportRDO {
		rows = rows == null ? List.of() : List.copyOf(rows);
	}
}
