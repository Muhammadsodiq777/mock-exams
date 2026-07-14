package com.mockexams.backend.dto;

public record ResultWeakAreaRDO(
		String questionType,
		Long sectionId,
		String sectionTitle,
		Integer incorrectCount,
		Integer totalCount
) {
}
