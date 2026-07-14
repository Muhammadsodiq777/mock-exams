package com.mockexams.backend.dto;

import java.math.BigDecimal;
import java.util.List;

public record ResultAnswerReviewRDO(
		Long questionId,
		Long sectionId,
		String sectionTitle,
		Integer questionOrder,
		String questionType,
		String questionText,
		String questionRenderedHtml,
		String studentAnswer,
		Long selectedOptionId,
		String selectedOptionLabel,
		List<String> correctAnswers,
		Boolean correct,
		BigDecimal scoreAwarded,
		BigDecimal maxScore,
		Boolean unanswered,
		String explanation,
		RichTextContentRDO explanationContent
) {
}
