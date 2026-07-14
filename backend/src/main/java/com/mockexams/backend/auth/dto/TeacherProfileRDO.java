package com.mockexams.backend.auth.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record TeacherProfileRDO(
		Long id,
		String displayName,
		String headline,
		String bio,
		Long avatarFileId,
		String status
) {
}
