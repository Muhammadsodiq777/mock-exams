package com.mockexams.backend.dto;

public record StudentListeningSegmentRDO(
		Long id,
		String title,
		Integer startSeconds,
		Integer endSeconds,
		Integer segmentOrder
) {
}
