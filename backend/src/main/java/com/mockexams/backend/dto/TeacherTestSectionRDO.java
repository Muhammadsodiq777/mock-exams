package com.mockexams.backend.dto;

import java.time.OffsetDateTime;
import java.util.List;

public record TeacherTestSectionRDO(
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
		List<TeacherListeningSegmentRDO> listeningSegments,
		List<TeacherQuestionRDO> questions,
		OffsetDateTime createdAt,
		OffsetDateTime updatedAt
) {
}
