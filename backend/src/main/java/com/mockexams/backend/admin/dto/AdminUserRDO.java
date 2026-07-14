package com.mockexams.backend.admin.dto;

import java.time.OffsetDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

public record AdminUserRDO(
		Long id,
		String email,
		String fullName,
		String phone,
		String status,
		@JsonInclude(JsonInclude.Include.NON_NULL) String teacherStatus,
		String preferredLanguage,
		List<String> roles,
		OffsetDateTime createdAt,
		OffsetDateTime updatedAt,
		@JsonInclude(JsonInclude.Include.NON_NULL) OffsetDateTime deletedAt
) {
}
