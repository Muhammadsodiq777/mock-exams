package com.mockexams.backend.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record TeacherWalletReportAggregateRDO(
		LocalDate transactionDate,
		String sourceType,
		long transactionCount,
		BigDecimal grossTotal,
		BigDecimal platformCommissionTotal,
		BigDecimal netTotal
) {
}
