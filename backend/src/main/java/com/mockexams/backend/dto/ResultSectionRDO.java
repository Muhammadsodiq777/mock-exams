package com.mockexams.backend.dto;

import java.math.BigDecimal;

public record ResultSectionRDO(
		Long id,
		String title,
		Integer sectionOrder,
		BigDecimal rawScore,
		BigDecimal maxScore,
		Integer correctCount,
		Integer incorrectCount
) {
}
