package com.mockexams.backend.teachers.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.mockexams.backend.dto.TeacherWalletRDO;

public record AdminTeacherProfileDetailRDO(
		TeacherProfileRDO profile,
		long totalTestCount,
		long publishedTestCount,
		@JsonInclude(JsonInclude.Include.NON_NULL) TeacherWalletRDO wallet,
		List<AdminTeacherRecentTestRDO> recentTests
) {
}
