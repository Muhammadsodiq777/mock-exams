package com.mockexams.backend.auth.mapper;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.mockexams.backend.auth.dto.CurrentUserRDO;
import com.mockexams.backend.auth.dto.TeacherProfileRDO;
import com.mockexams.backend.teachers.entity.TeacherProfile;
import com.mockexams.backend.users.entity.User;

@Component
public class CurrentUserMapper {

	public CurrentUserRDO toRDO(User user, Optional<TeacherProfile> teacherProfile) {
		List<String> roles = user.getUserRoles().stream()
				.map(userRole -> userRole.getRole().getCode())
				.sorted(Comparator.naturalOrder())
				.toList();
		return new CurrentUserRDO(
				user.getId(),
				user.getEmail(),
				user.getFullName(),
				user.getPhone(),
				user.getStatus(),
				user.getPreferredLanguage(),
				roles,
				teacherProfile.map(this::toTeacherProfileRDO).orElse(null));
	}

	private TeacherProfileRDO toTeacherProfileRDO(TeacherProfile teacherProfile) {
		return new TeacherProfileRDO(
				teacherProfile.getId(),
				teacherProfile.getDisplayName(),
				teacherProfile.getHeadline(),
				teacherProfile.getBio(),
				teacherProfile.getAvatarFileId(),
				teacherProfile.getStatus());
	}
}
