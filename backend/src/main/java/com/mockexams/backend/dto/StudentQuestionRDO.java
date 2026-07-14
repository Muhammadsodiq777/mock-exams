package com.mockexams.backend.dto;

import java.util.List;

public record StudentQuestionRDO(
		Long id,
		String questionType,
		String questionText,
		RichTextContentRDO questionTextContent,
		String instructions,
		RichTextContentRDO instructionsContent,
		Integer questionOrder,
		String relatedParagraphLabel,
		Long listeningSegmentId,
		List<StudentQuestionOptionRDO> options
) {
}
