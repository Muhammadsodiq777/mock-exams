package com.mockexams.backend.dto;

import java.time.OffsetDateTime;
import java.util.List;

public record AdminSalesReportRDO(
		OffsetDateTime generatedAt,
		AdminReportFiltersRDO filters,
		AdminSalesReportTotalsRDO totals,
		List<AdminSalesReportRowRDO> rows,
		String liveSpeakingStatus
) {
	public AdminSalesReportRDO {
		rows = rows == null ? List.of() : List.copyOf(rows);
	}
}
