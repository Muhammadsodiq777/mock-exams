package com.mockexams.backend.mapper;

import org.springframework.stereotype.Component;

import com.mockexams.backend.dto.AdminModerationTestRDO;
import com.mockexams.backend.entity.CatalogTest;
import com.mockexams.backend.teachers.entity.TeacherProfile;

@Component
public class AdminModerationMapper {

	public AdminModerationTestRDO toQueueRDO(CatalogTest test) {
		TeacherProfile teacherProfile = test.getTeacherProfile();
		return new AdminModerationTestRDO(
				test.getId(),
				teacherProfile == null ? null : teacherProfile.getId(),
				teacherProfile == null ? null : teacherProfile.getDisplayName(),
				test.getTitle(),
				test.getType(),
				test.getDifficulty(),
				test.getDurationMinutes(),
				test.getPriceAmount(),
				test.getCurrency(),
				test.isFree(),
				test.getVisibility(),
				test.getUpdatedAt(),
				test.getCreatedAt());
	}
}
