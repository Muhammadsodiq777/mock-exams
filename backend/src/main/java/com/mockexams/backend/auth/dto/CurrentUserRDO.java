package com.mockexams.backend.auth.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

public record CurrentUserRDO(
		Long id,
		String email,
		String fullName,
		String phone,
		String status,
		String preferredLanguage,
		List<String> roles,
		@JsonInclude(JsonInclude.Include.NON_NULL) TeacherProfileRDO teacherProfile
) {
}
