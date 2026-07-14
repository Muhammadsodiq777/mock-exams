package com.mockexams.backend.dto;

public record TestCategoryRDO(
		Long id,
		Long parentId,
		String name,
		String slug,
		Integer sortOrder
) {
}
