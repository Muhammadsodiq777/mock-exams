package com.mockexams.backend.dto;

public record TeacherListeningSegmentRDO(
		Long id,
		String title,
		Integer startSeconds,
		Integer endSeconds,
		String transcript,
		Integer segmentOrder
) {
}
