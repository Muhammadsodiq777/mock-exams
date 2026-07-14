package com.mockexams.backend.dto;

import java.math.BigDecimal;
import java.util.List;

public record TeacherQuestionRDO(
		Long id,
		String questionType,
		String questionText,
		RichTextContentRDO questionTextContent,
		String instructions,
		RichTextContentRDO instructionsContent,
		BigDecimal score,
		Integer questionOrder,
		String relatedParagraphLabel,
		Long listeningSegmentId,
		List<TeacherQuestionOptionRDO> options,
		List<TeacherQuestionAnswerRDO> answerKeys
) {
}
