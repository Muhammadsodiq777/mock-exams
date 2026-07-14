package com.mockexams.backend.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

public record ProgressSummaryRDO(
		BigDecimal overallBandEstimate,
		Map<String, BigDecimal> skillBands,
		List<String> missingSkills,
		long completedAttemptCount,
		OffsetDateTime latestResultAt
) {
}
