package com.mockexams.backend.dto;

import java.util.List;

public record TestDetailSectionRDO(
		Long id,
		String title,
		String instructions,
		RichTextContentRDO instructionsContent,
		String passageText,
		RichTextContentRDO passageTextContent,
		Long audioFileId,
		Integer sectionOrder,
		Integer durationMinutes,
		List<TeacherReadingParagraphRDO> paragraphs,
		List<StudentListeningSegmentRDO> listeningSegments,
		List<StudentQuestionRDO> questions
) {
}
