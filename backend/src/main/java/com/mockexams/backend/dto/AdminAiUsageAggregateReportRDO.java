package com.mockexams.backend.dto;

import java.time.OffsetDateTime;
import java.util.List;

public record AdminAiUsageAggregateReportRDO(
		OffsetDateTime generatedAt,
		AdminReportFiltersRDO filters,
		AdminAiUsageReportTotalsRDO totals,
		List<AdminAiUsageAggregateRowRDO> rows
) {
	public AdminAiUsageAggregateReportRDO {
		rows = rows == null ? List.of() : List.copyOf(rows);
	}
}
