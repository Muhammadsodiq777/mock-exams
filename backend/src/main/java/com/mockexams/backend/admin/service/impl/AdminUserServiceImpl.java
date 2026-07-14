package com.mockexams.backend.admin.service.impl;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mockexams.backend.admin.dto.AdminUserRDO;
import com.mockexams.backend.admin.dto.AdminUserStatusUDO;
import com.mockexams.backend.admin.mapper.AdminUserMapper;
import com.mockexams.backend.admin.service.AdminUserService;
import com.mockexams.backend.error.BusinessException;
import com.mockexams.backend.service.NotificationService;
import com.mockexams.backend.teachers.TeacherProfileStatuses;
import com.mockexams.backend.teachers.entity.TeacherProfile;
import com.mockexams.backend.teachers.repository.TeacherProfileRepository;
import com.mockexams.backend.users.RoleCodes;
import com.mockexams.backend.users.UserStatuses;
import com.mockexams.backend.users.entity.User;
import com.mockexams.backend.users.repository.UserRepository;

@Service
class AdminUserServiceImpl implements AdminUserService {

	private final UserRepository userRepository;
	private final TeacherProfileRepository teacherProfileRepository;
	private final AdminUserMapper adminUserMapper;
	private final NotificationService notificationService;

	AdminUserServiceImpl(
			UserRepository userRepository,
			TeacherProfileRepository teacherProfileRepository,
			AdminUserMapper adminUserMapper,
			NotificationService notificationService
	) {
		this.userRepository = userRepository;
		this.teacherProfileRepository = teacherProfileRepository;
		this.adminUserMapper = adminUserMapper;
		this.notificationService = notificationService;
	}

	@Override
	@Transactional(readOnly = true)
	public Page<AdminUserRDO> listUsers(String status, String role, String teacherStatus, String email, Pageable pageable) {
		String normalizedStatus = trimToNull(status);
		if (normalizedStatus != null && !UserStatuses.isSupported(normalizedStatus)) {
			throw validationFailed("Unsupported user status filter.");
		}
		String normalizedRole = trimToNull(role);
		if (normalizedRole != null && !RoleCodes.isSupported(normalizedRole)) {
			throw validationFailed("Unsupported role filter.");
		}
		String normalizedTeacherStatus = trimToNull(teacherStatus);
		if (normalizedTeacherStatus != null && !TeacherProfileStatuses.isSupported(normalizedTeacherStatus)) {
			throw validationFailed("Unsupported teacher profile status filter.");
		}
		String trimmedEmail = trimToNull(email);
		String emailPattern = trimmedEmail == null ? null : "%" + trimmedEmail.toLowerCase() + "%";

		Page<Long> idPage = userRepository.searchAdminUserIds(
				normalizedStatus,
				normalizedRole,
				normalizedTeacherStatus,
				emailPattern,
				pageable);
		if (idPage.isEmpty()) {
			return new PageImpl<>(List.of(), pageable, idPage.getTotalElements());
		}
		Map<Long, User> usersById = userRepository.findByIdIn(idPage.getContent()).stream()
				.collect(Collectors.toMap(User::getId, Function.identity()));
		Map<Long, String> teacherStatusesByUserId = teacherStatusesByUserId(idPage.getContent());
		List<AdminUserRDO> ordered = idPage.getContent().stream()
				.map(usersById::get)
				.filter(Objects::nonNull)
				.map(user -> adminUserMapper.toRDO(user, teacherStatusesByUserId.get(user.getId())))
				.toList();
		return new PageImpl<>(ordered, pageable, idPage.getTotalElements());
	}

	@Override
	@Transactional(readOnly = true)
	public AdminUserRDO getUser(Long userId) {
		User user = userRepository.findWithRolesById(userId).orElseThrow(this::notFound);
		return adminUserMapper.toRDO(user, teacherStatusByUserId(userId));
	}

	@Override
	@Transactional
	public AdminUserRDO updateStatus(Long userId, AdminUserStatusUDO command) {
		String targetStatus = command.status();

		// Cheap, unlocked read used only to decide which lock acquisition strategy applies. This
		// intentionally avoids taking a write lock on the target row before knowing whether the
		// last-admin protection path (and its ordered active-admin lock set) will also be needed:
		// locking the target row on its own first and only afterwards locking the ordered active-admin
		// set is what previously caused a lock-order-inversion deadlock between two concurrent
		// demotions (e.g. Tx1 locks admin A then waits for [A, B]; Tx2 locks admin B then waits for
		// [A, B]).
		User snapshot = userRepository.findWithRolesById(userId).orElseThrow(this::notFound);
		boolean mayLeaveActiveAdmin = snapshot.isActiveAdmin() && !UserStatuses.ACTIVE.equals(targetStatus);

		User user;
		List<User> activeAdmins = List.of();
		boolean leavingActiveAdmin = false;

		if (mayLeaveActiveAdmin) {
			// Acquire every lock this call could need in a single ascending-id-ordered pass. The
			// target is, by definition, a member of "all active admins" here, so it is locked as
			// part of that ordered set instead of being locked separately, out of order, beforehand.
			activeAdmins = userRepository.findActiveByRoleForUpdate(RoleCodes.ADMIN, UserStatuses.ACTIVE);
			User lockedTarget = activeAdmins.stream()
					.filter(admin -> admin.getId().equals(userId))
					.findFirst()
					.orElse(null);
			if (lockedTarget != null) {
				user = lockedTarget;
				leavingActiveAdmin = true;
			} else {
				// A concurrent transaction already changed the target (role or status) between the
				// snapshot read and this lock attempt, so it is no longer part of the active-admin
				// set and the protection no longer applies. Fall back to locking only the target row;
				// this is safe because no other concurrent caller needs to hold this specific row as
				// part of an ordered active-admin set once it has left that set.
				user = userRepository.findForUpdateWithRolesById(userId).orElseThrow(this::notFound);
			}
		} else {
			// The last-admin invariant is irrelevant here (not an admin, not active, or not leaving
			// active status), so only the target row itself needs a lock.
			user = userRepository.findForUpdateWithRolesById(userId).orElseThrow(this::notFound);
		}

		String previousStatus = user.getStatus();

		if (leavingActiveAdmin && activeAdmins.size() <= 1) {
			throw lastAdminProtected();
		}

		applyStatus(user, targetStatus);
		User saved = userRepository.saveAndFlush(user);
		if (!previousStatus.equals(targetStatus)) {
			notificationService.notifyAccountStatusChange(saved.getId(), targetStatus, trimToNull(command.reason()));
		}
		return adminUserMapper.toRDO(saved, teacherStatusByUserId(saved.getId()));
	}

	private Map<Long, String> teacherStatusesByUserId(List<Long> userIds) {
		return teacherProfileRepository.findByUserIdsWithUser(userIds).stream()
				.collect(Collectors.toMap(profile -> profile.getUser().getId(), TeacherProfile::getStatus));
	}

	private String teacherStatusByUserId(Long userId) {
		return teacherProfileRepository.findByUserId(userId)
				.map(TeacherProfile::getStatus)
				.orElse(null);
	}

	private void applyStatus(User user, String status) {
		switch (status) {
			case UserStatuses.ACTIVE -> user.reactivate();
			case UserStatuses.SUSPENDED -> user.suspend();
			case UserStatuses.BANNED -> user.ban();
			case UserStatuses.DELETED -> user.softDelete();
			default -> throw validationFailed("Unsupported user status.");
		}
	}

	private BusinessException notFound() {
		return new BusinessException("USER_NOT_FOUND", "User was not found.", HttpStatus.NOT_FOUND);
	}

	private BusinessException lastAdminProtected() {
		return new BusinessException(
				"LAST_ADMIN_PROTECTED",
				"The last active admin cannot be suspended, banned, or deleted.",
				HttpStatus.CONFLICT);
	}

	private BusinessException validationFailed(String message) {
		return new BusinessException("VALIDATION_FAILED", message, HttpStatus.BAD_REQUEST);
	}

	private String trimToNull(String value) {
		if (value == null || value.isBlank()) {
			return null;
		}
		return value.trim();
	}
}
