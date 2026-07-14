package com.mockexams.backend.teachers.mapper;

import org.springframework.stereotype.Component;

import com.mockexams.backend.teachers.dto.TeacherProfilePublicRDO;
import com.mockexams.backend.teachers.dto.TeacherProfileRDO;
import com.mockexams.backend.teachers.entity.TeacherProfile;

@Component
public class TeacherProfileMapper {

	public TeacherProfileRDO toRDO(TeacherProfile teacherProfile) {
		return new TeacherProfileRDO(
				teacherProfile.getId(),
				teacherProfile.getUser().getId(),
				teacherProfile.getDisplayName(),
				teacherProfile.getHeadline(),
				teacherProfile.getBio(),
				teacherProfile.getAvatarFileId(),
				teacherProfile.getStatus(),
				teacherProfile.getApprovedByUserId(),
				teacherProfile.getApprovedAt(),
				teacherProfile.getRejectionReason(),
				teacherProfile.getRatingAverage(),
				teacherProfile.getRatingCount(),
				teacherProfile.getCreatedAt(),
				teacherProfile.getUpdatedAt());
	}

	public TeacherProfilePublicRDO toPublicRDO(TeacherProfile teacherProfile) {
		return new TeacherProfilePublicRDO(
				teacherProfile.getId(),
				teacherProfile.getDisplayName(),
				teacherProfile.getHeadline(),
				teacherProfile.getBio(),
				teacherProfile.getAvatarFileId(),
				teacherProfile.getStatus(),
				teacherProfile.getRatingAverage(),
				teacherProfile.getRatingCount());
	}
}
