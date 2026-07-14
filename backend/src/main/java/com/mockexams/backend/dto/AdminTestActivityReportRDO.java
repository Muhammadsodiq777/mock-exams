package com.mockexams.backend.dto;

import java.time.OffsetDateTime;
import java.util.List;

public record AdminTestActivityReportRDO(
		OffsetDateTime generatedAt,
		AdminReportFiltersRDO filters,
		AdminTestActivityReportTotalsRDO totals,
		List<AdminTestActivityReportRowRDO> rows
) {
	public AdminTestActivityReportRDO {
		rows = rows == null ? List.of() : List.copyOf(rows);
	}
}
