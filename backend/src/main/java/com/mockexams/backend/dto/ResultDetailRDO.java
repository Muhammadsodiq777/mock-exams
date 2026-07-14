package com.mockexams.backend.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

public record ResultDetailRDO(
		Long id,
		Long attemptId,
		Long testId,
		String testTitle,
		String testType,
		BigDecimal rawScore,
		BigDecimal maxScore,
		BigDecimal estimatedBand,
		Integer correctCount,
		Integer incorrectCount,
		OffsetDateTime startedAt,
		OffsetDateTime submittedAt,
		Integer timeSpentSeconds,
		List<ResultSectionRDO> sections,
		List<ResultAnswerReviewRDO> answers,
		List<ResultWeakAreaRDO> weakAreas
) {
}
