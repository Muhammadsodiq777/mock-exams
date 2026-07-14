package com.mockexams.backend.api;

import org.springframework.data.domain.Page;

public record ApiPageMeta(
		int number,
		int size,
		long totalElements,
		int totalPages,
		boolean first,
		boolean last
) {

	public static ApiPageMeta from(Page<?> page) {
		return new ApiPageMeta(
				page.getNumber(),
				page.getSize(),
				page.getTotalElements(),
				page.getTotalPages(),
				page.isFirst(),
				page.isLast());
	}
}
