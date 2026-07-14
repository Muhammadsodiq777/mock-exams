package com.mockexams.backend.admin.mapper;

import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Component;

import com.mockexams.backend.admin.dto.AdminUserRDO;
import com.mockexams.backend.users.entity.User;

@Component
public class AdminUserMapper {

	public AdminUserRDO toRDO(User user) {
		return toRDO(user, null);
	}

	public AdminUserRDO toRDO(User user, String teacherStatus) {
		List<String> roles = user.getUserRoles().stream()
				.map(userRole -> userRole.getRole().getCode())
				.sorted(Comparator.naturalOrder())
				.toList();
		return new AdminUserRDO(
				user.getId(),
				user.getEmail(),
				user.getFullName(),
				user.getPhone(),
				user.getStatus(),
				teacherStatus,
				user.getPreferredLanguage(),
				roles,
				user.getCreatedAt(),
				user.getUpdatedAt(),
				user.getDeletedAt());
	}
}
