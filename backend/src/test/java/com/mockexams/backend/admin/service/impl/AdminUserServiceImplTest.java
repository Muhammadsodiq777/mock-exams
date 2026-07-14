package com.mockexams.backend.admin.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;

import com.mockexams.backend.admin.dto.AdminUserStatusUDO;
import com.mockexams.backend.admin.mapper.AdminUserMapper;
import com.mockexams.backend.error.BusinessException;
import com.mockexams.backend.service.NotificationService;
import com.mockexams.backend.teachers.repository.TeacherProfileRepository;
import com.mockexams.backend.users.RoleCodes;
import com.mockexams.backend.users.UserStatuses;
import com.mockexams.backend.users.entity.User;
import com.mockexams.backend.users.repository.UserRepository;

class AdminUserServiceImplTest {

	@Test
	void lastActiveAdminCannotBeBanned() {
		UserRepository userRepository = mock(UserRepository.class);
		TeacherProfileRepository teacherProfileRepository = mock(TeacherProfileRepository.class);
		NotificationService notificationService = mock(NotificationService.class);
		AdminUserServiceImpl service = new AdminUserServiceImpl(
				userRepository, teacherProfileRepository, mock(AdminUserMapper.class), notificationService);

		User admin = mock(User.class);
		when(admin.getId()).thenReturn(1L);
		when(admin.getStatus()).thenReturn(UserStatuses.ACTIVE);
		when(admin.isActiveAdmin()).thenReturn(true);

		when(userRepository.findWithRolesById(1L)).thenReturn(Optional.of(admin));
		when(userRepository.findActiveByRoleForUpdate(RoleCodes.ADMIN, UserStatuses.ACTIVE))
				.thenReturn(List.of(admin));

		assertThatThrownBy(() -> service.updateStatus(1L, new AdminUserStatusUDO(UserStatuses.BANNED, null)))
				.isInstanceOf(BusinessException.class)
				.satisfies(ex -> assertThat(((BusinessException) ex).getCode()).isEqualTo("LAST_ADMIN_PROTECTED"));

		verify(admin, never()).ban();
		verify(userRepository, never()).saveAndFlush(any());
		// The demotion path must lock the target exclusively through the single ascending-id-ordered
		// active-admin pass; taking a separate single-row lock first would reintroduce the
		// lock-order-inversion deadlock between two concurrent admin demotions.
		verify(userRepository, never()).findForUpdateWithRolesById(anyLong());
		verifyNoInteractions(notificationService);
	}

	@Test
	void banSucceedsWhenAnotherActiveAdminRemains() {
		UserRepository userRepository = mock(UserRepository.class);
		TeacherProfileRepository teacherProfileRepository = mock(TeacherProfileRepository.class);
		NotificationService notificationService = mock(NotificationService.class);
		AdminUserServiceImpl service = new AdminUserServiceImpl(
				userRepository, teacherProfileRepository, mock(AdminUserMapper.class), notificationService);

		User admin = mock(User.class);
		when(admin.getId()).thenReturn(1L);
		when(admin.getStatus()).thenReturn(UserStatuses.ACTIVE);
		when(admin.isActiveAdmin()).thenReturn(true);
		User otherAdmin = mock(User.class);
		when(otherAdmin.getId()).thenReturn(2L);

		when(userRepository.findWithRolesById(1L)).thenReturn(Optional.of(admin));
		when(userRepository.findActiveByRoleForUpdate(RoleCodes.ADMIN, UserStatuses.ACTIVE))
				.thenReturn(List.of(admin, otherAdmin));
		when(userRepository.saveAndFlush(admin)).thenReturn(admin);
		when(teacherProfileRepository.findByUserId(anyLong())).thenReturn(Optional.empty());

		service.updateStatus(1L, new AdminUserStatusUDO(UserStatuses.BANNED, "policy violation"));

		verify(admin).ban();
		verify(userRepository).saveAndFlush(admin);
		verify(userRepository, never()).findForUpdateWithRolesById(anyLong());
		verify(notificationService).notifyAccountStatusChange(1L, UserStatuses.BANNED, "policy violation");
	}

	@Test
	void concurrentStatusChangeAwayFromActiveAdminFallsBackToDirectLock() {
		// Between the cheap unlocked pre-check and the ordered lock acquisition, another
		// transaction may have already changed the target so it no longer appears in the
		// locked active-admin set (e.g. it was demoted first and committed). The last-admin
		// invariant no longer applies in that case, so the service must fall back to locking
		// only the target row directly instead of retrying the ordered pass.
		UserRepository userRepository = mock(UserRepository.class);
		TeacherProfileRepository teacherProfileRepository = mock(TeacherProfileRepository.class);
		NotificationService notificationService = mock(NotificationService.class);
		AdminUserServiceImpl service = new AdminUserServiceImpl(
				userRepository, teacherProfileRepository, mock(AdminUserMapper.class), notificationService);

		User admin = mock(User.class);
		when(admin.getId()).thenReturn(1L);
		when(admin.getStatus()).thenReturn(UserStatuses.SUSPENDED);
		when(admin.isActiveAdmin()).thenReturn(true);

		when(userRepository.findWithRolesById(1L)).thenReturn(Optional.of(admin));
		when(userRepository.findActiveByRoleForUpdate(RoleCodes.ADMIN, UserStatuses.ACTIVE))
				.thenReturn(List.of());
		when(userRepository.findForUpdateWithRolesById(1L)).thenReturn(Optional.of(admin));
		when(userRepository.saveAndFlush(admin)).thenReturn(admin);
		when(teacherProfileRepository.findByUserId(anyLong())).thenReturn(Optional.empty());

		service.updateStatus(1L, new AdminUserStatusUDO(UserStatuses.BANNED, "policy violation"));

		verify(admin).ban();
		verify(userRepository).saveAndFlush(admin);
	}

	@Test
	void suspendingNonAdminNeverLocksActiveAdminSet() {
		UserRepository userRepository = mock(UserRepository.class);
		TeacherProfileRepository teacherProfileRepository = mock(TeacherProfileRepository.class);
		NotificationService notificationService = mock(NotificationService.class);
		AdminUserServiceImpl service = new AdminUserServiceImpl(
				userRepository, teacherProfileRepository, mock(AdminUserMapper.class), notificationService);

		User student = mock(User.class);
		when(student.getId()).thenReturn(5L);
		when(student.getStatus()).thenReturn(UserStatuses.ACTIVE);
		when(student.isActiveAdmin()).thenReturn(false);

		when(userRepository.findWithRolesById(5L)).thenReturn(Optional.of(student));
		when(userRepository.findForUpdateWithRolesById(5L)).thenReturn(Optional.of(student));
		when(userRepository.saveAndFlush(student)).thenReturn(student);
		when(teacherProfileRepository.findByUserId(anyLong())).thenReturn(Optional.empty());

		service.updateStatus(5L, new AdminUserStatusUDO(UserStatuses.SUSPENDED, "reported by students"));

		verify(student).suspend();
		verify(userRepository, never()).findActiveByRoleForUpdate(anyString(), anyString());
		verify(notificationService).notifyAccountStatusChange(5L, UserStatuses.SUSPENDED, "reported by students");
	}

	@Test
	void reactivatingAnAdminNeverLocksActiveAdminSet() {
		UserRepository userRepository = mock(UserRepository.class);
		TeacherProfileRepository teacherProfileRepository = mock(TeacherProfileRepository.class);
		NotificationService notificationService = mock(NotificationService.class);
		AdminUserServiceImpl service = new AdminUserServiceImpl(
				userRepository, teacherProfileRepository, mock(AdminUserMapper.class), notificationService);

		User admin = mock(User.class);
		when(admin.getId()).thenReturn(1L);
		when(admin.getStatus()).thenReturn(UserStatuses.SUSPENDED);
		when(admin.isActiveAdmin()).thenReturn(false);

		when(userRepository.findWithRolesById(1L)).thenReturn(Optional.of(admin));
		when(userRepository.findForUpdateWithRolesById(1L)).thenReturn(Optional.of(admin));
		when(userRepository.saveAndFlush(admin)).thenReturn(admin);
		when(teacherProfileRepository.findByUserId(anyLong())).thenReturn(Optional.empty());

		service.updateStatus(1L, new AdminUserStatusUDO(UserStatuses.ACTIVE, null));

		verify(admin).reactivate();
		verify(userRepository, never()).findActiveByRoleForUpdate(anyString(), anyString());
	}

	@Test
	void updateStatusThrowsWhenUserMissing() {
		UserRepository userRepository = mock(UserRepository.class);
		when(userRepository.findWithRolesById(99L)).thenReturn(Optional.empty());
		AdminUserServiceImpl service = new AdminUserServiceImpl(
				userRepository,
				mock(TeacherProfileRepository.class),
				mock(AdminUserMapper.class),
				mock(NotificationService.class));

		assertThatThrownBy(() -> service.updateStatus(99L, new AdminUserStatusUDO(UserStatuses.SUSPENDED, null)))
				.isInstanceOf(BusinessException.class)
				.satisfies(ex -> assertThat(((BusinessException) ex).getCode()).isEqualTo("USER_NOT_FOUND"));
	}

	@Test
	void listUsersRejectsUnsupportedStatusFilter() {
		UserRepository userRepository = mock(UserRepository.class);
		TeacherProfileRepository teacherProfileRepository = mock(TeacherProfileRepository.class);
		AdminUserServiceImpl service = new AdminUserServiceImpl(
				userRepository, teacherProfileRepository, mock(AdminUserMapper.class), mock(NotificationService.class));

		assertThatThrownBy(() -> service.listUsers("bogus", null, null, null, PageRequest.of(0, 20)))
				.isInstanceOf(BusinessException.class)
				.satisfies(ex -> assertThat(((BusinessException) ex).getCode()).isEqualTo("VALIDATION_FAILED"));
		verifyNoInteractions(userRepository, teacherProfileRepository);
	}

	@Test
	void listUsersRejectsUnsupportedRoleFilter() {
		UserRepository userRepository = mock(UserRepository.class);
		TeacherProfileRepository teacherProfileRepository = mock(TeacherProfileRepository.class);
		AdminUserServiceImpl service = new AdminUserServiceImpl(
				userRepository, teacherProfileRepository, mock(AdminUserMapper.class), mock(NotificationService.class));

		assertThatThrownBy(() -> service.listUsers(null, "bogus", null, null, PageRequest.of(0, 20)))
				.isInstanceOf(BusinessException.class)
				.satisfies(ex -> assertThat(((BusinessException) ex).getCode()).isEqualTo("VALIDATION_FAILED"));
		verifyNoInteractions(userRepository, teacherProfileRepository);
	}

	@Test
	void listUsersRejectsUnsupportedTeacherStatusFilter() {
		UserRepository userRepository = mock(UserRepository.class);
		TeacherProfileRepository teacherProfileRepository = mock(TeacherProfileRepository.class);
		AdminUserServiceImpl service = new AdminUserServiceImpl(
				userRepository, teacherProfileRepository, mock(AdminUserMapper.class), mock(NotificationService.class));

		assertThatThrownBy(() -> service.listUsers(null, null, "bogus", null, PageRequest.of(0, 20)))
				.isInstanceOf(BusinessException.class)
				.satisfies(ex -> assertThat(((BusinessException) ex).getCode()).isEqualTo("VALIDATION_FAILED"));
		verifyNoInteractions(userRepository, teacherProfileRepository);
	}
}
