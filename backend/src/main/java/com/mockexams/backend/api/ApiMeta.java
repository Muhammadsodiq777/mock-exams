package com.mockexams.backend.api;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonInclude;

public record ApiMeta(
		String requestId,
		@JsonInclude(JsonInclude.Include.NON_NULL) ApiPageMeta page,
		@JsonInclude(JsonInclude.Include.NON_EMPTY) List<ApiSortMeta> sort,
		@JsonInclude(JsonInclude.Include.NON_EMPTY) Map<String, Object> filters,
		@JsonInclude(JsonInclude.Include.NON_NULL) OffsetDateTime asOf,
		@JsonInclude(JsonInclude.Include.NON_EMPTY) Map<String, Object> window
) {

	public ApiMeta {
		if (sort == null) {
			sort = List.of();
		}
		else {
			sort = List.copyOf(sort);
		}
		if (filters == null) {
			filters = Map.of();
		}
		else {
			filters = Map.copyOf(filters);
		}
		if (window == null) {
			window = Map.of();
		}
		else {
			window = Map.copyOf(window);
		}
	}

	public ApiMeta(String requestId) {
		this(requestId, null, List.of(), Map.of(), null, Map.of());
	}

	public static ApiMeta create() {
		return new ApiMeta(UUID.randomUUID().toString());
	}

	public static ApiMeta create(ApiPageMeta page, List<ApiSortMeta> sort, Map<String, Object> filters) {
		return new ApiMeta(UUID.randomUUID().toString(), page, sort, filters, null, Map.of());
	}

	public static ApiMeta dashboard(OffsetDateTime asOf, Map<String, Object> window) {
		return new ApiMeta(UUID.randomUUID().toString(), null, List.of(), Map.of(), asOf, window);
	}
}
