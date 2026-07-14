package com.mockexams.backend.teachers.service.impl;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mockexams.backend.entity.CatalogTest;
import com.mockexams.backend.entity.TeacherWallet;
import com.mockexams.backend.entity.TestVisibilities;
import com.mockexams.backend.error.BusinessException;
import com.mockexams.backend.mapper.TeacherWalletMapper;
import com.mockexams.backend.repository.TeacherWalletRepository;
import com.mockexams.backend.repository.TestCatalogRepository;
import com.mockexams.backend.service.NotificationService;
import com.mockexams.backend.teachers.TeacherProfileStatuses;
import com.mockexams.backend.teachers.dto.AdminTeacherProfileDetailRDO;
import com.mockexams.backend.teachers.dto.AdminTeacherRecentTestRDO;
import com.mockexams.backend.teachers.dto.TeacherProfileCDO;
import com.mockexams.backend.teachers.dto.TeacherProfileModerationUDO;
import com.mockexams.backend.teachers.dto.TeacherProfilePublicRDO;
import com.mockexams.backend.teachers.dto.TeacherProfileRDO;
import com.mockexams.backend.teachers.dto.TeacherProfileSuspensionUDO;
import com.mockexams.backend.teachers.dto.TeacherProfileUDO;
import com.mockexams.backend.teachers.entity.TeacherProfile;
import com.mockexams.backend.teachers.mapper.TeacherProfileMapper;
import com.mockexams.backend.teachers.repository.TeacherProfileRepository;
import com.mockexams.backend.teachers.service.TeacherProfileService;
import com.mockexams.backend.users.entity.User;

@Service
class TeacherProfileServiceImpl implements TeacherProfileService {

	private final TeacherProfileRepository teacherProfileRepository;
	private final TeacherProfileMapper teacherProfileMapper;
	private final NotificationService notificationService;
	private final TestCatalogRepository testCatalogRepository;
	private final TeacherWalletRepository teacherWalletRepository;
	private final TeacherWalletMapper teacherWalletMapper;

	TeacherProfileServiceImpl(
			TeacherProfileRepository teacherProfileRepository,
			TeacherProfileMapper teacherProfileMapper,
			NotificationService notificationService,
			TestCatalogRepository testCatalogRepository,
			TeacherWalletRepository teacherWalletRepository,
			TeacherWalletMapper teacherWalletMapper
	) {
		this.teacherProfileRepository = teacherProfileRepository;
		this.teacherProfileMapper = teacherProfileMapper;
		this.notificationService = notificationService;
		this.testCatalogRepository = testCatalogRepository;
		this.teacherWalletRepository = teacherWalletRepository;
		this.teacherWalletMapper = teacherWalletMapper;
	}

	@Override
	@Transactional
	public TeacherProfileRDO createMyProfile(User teacher, TeacherProfileCDO command) {
		if (teacherProfileRepository.findByUserId(teacher.getId()).isPresent()) {
			throw statusConflict("Teacher profile already exists.");
		}
		TeacherProfile teacherProfile = new TeacherProfile(
				teacher,
				command.displayName().trim(),
				trimToNull(command.headline()),
				trimToNull(command.bio()));
		teacherProfile.updateProfile(
				command.displayName().trim(),
				trimToNull(command.headline()),
				trimToNull(command.bio()),
				command.avatarFileId());
		teacherProfile.markDraft();
		return teacherProfileMapper.toRDO(teacherProfileRepository.saveAndFlush(teacherProfile));
	}

	@Override
	@Transactional(readOnly = true)
	public TeacherProfileRDO myProfile(User teacher) {
		return teacherProfileMapper.toRDO(findByTeacher(teacher));
	}

	@Override
	@Transactional
	public TeacherProfileRDO updateMyProfile(User teacher, TeacherProfileUDO command) {
		TeacherProfile teacherProfile = findByTeacher(teacher);
		if (!TeacherProfileStatuses.DRAFT.equals(teacherProfile.getStatus())
				&& !TeacherProfileStatuses.REJECTED.equals(teacherProfile.getStatus())) {
			throw statusConflict("Teacher profile cannot be edited in its current status.");
		}
		teacherProfile.updateProfile(
				command.displayName().trim(),
				trimToNull(command.headline()),
				trimToNull(command.bio()),
				command.avatarFileId());
		return teacherProfileMapper.toRDO(teacherProfileRepository.saveAndFlush(teacherProfile));
	}

	@Override
	@Transactional
	public TeacherProfileRDO submitMyProfile(User teacher) {
		TeacherProfile teacherProfile = findByTeacher(teacher);
		if (!TeacherProfileStatuses.DRAFT.equals(teacherProfile.getStatus())
				&& !TeacherProfileStatuses.REJECTED.equals(teacherProfile.getStatus())) {
			throw statusConflict("Teacher profile cannot be submitted in its current status.");
		}
		teacherProfile.submitForApproval();
		return teacherProfileMapper.toRDO(teacherProfileRepository.saveAndFlush(teacherProfile));
	}

	@Override
	@Transactional(readOnly = true)
	public Page<TeacherProfilePublicRDO> listApprovedPublicProfiles(Pageable pageable) {
		return teacherProfileRepository.findByStatus(TeacherProfileStatuses.APPROVED, pageable)
				.map(teacherProfileMapper::toPublicRDO);
	}

	@Override
	@Transactional(readOnly = true)
	public TeacherProfilePublicRDO publicProfile(Long profileId) {
		TeacherProfile teacherProfile = teacherProfileRepository
				.findByIdAndStatus(profileId, TeacherProfileStatuses.APPROVED)
				.orElseThrow(this::notFound);
		return teacherProfileMapper.toPublicRDO(teacherProfile);
	}

	@Override
	@Transactional(readOnly = true)
	public Page<TeacherProfileRDO> listAdminProfiles(String status, Pageable pageable) {
		if (status == null || status.isBlank()) {
			return teacherProfileRepository.findAll(pageable).map(teacherProfileMapper::toRDO);
		}
		String normalizedStatus = status.trim();
		if (!TeacherProfileStatuses.isSupported(normalizedStatus)) {
			throw new BusinessException("VALIDATION_FAILED", "Unsupported teacher profile status.", HttpStatus.BAD_REQUEST);
		}
		return teacherProfileRepository.findByStatus(normalizedStatus, pageable).map(teacherProfileMapper::toRDO);
	}

	@Override
	@Transactional(readOnly = true)
	public AdminTeacherProfileDetailRDO adminProfileDetail(Long profileId) {
		TeacherProfile teacherProfile = findById(profileId);
		long totalTestCount = testCatalogRepository.countByTeacherProfile_IdAndDeletedAtIsNull(profileId);
		long publishedTestCount = testCatalogRepository.countByTeacherProfile_IdAndVisibilityAndDeletedAtIsNull(
				profileId, TestVisibilities.PUBLISHED);
		TeacherWallet wallet = teacherWalletRepository.findForReadByTeacherProfileId(profileId).orElse(null);
		List<AdminTeacherRecentTestRDO> recentTests = testCatalogRepository
				.findTop5ByTeacherProfile_IdAndDeletedAtIsNullOrderByUpdatedAtDescIdDesc(profileId).stream()
				.map(this::toRecentTestRDO)
				.toList();
		return new AdminTeacherProfileDetailRDO(
				teacherProfileMapper.toRDO(teacherProfile),
				totalTestCount,
				publishedTestCount,
				wallet == null ? null : teacherWalletMapper.toRDO(wallet),
				recentTests);
	}

	private AdminTeacherRecentTestRDO toRecentTestRDO(CatalogTest test) {
		return new AdminTeacherRecentTestRDO(test.getId(), test.getTitle(), test.getVisibility(), test.getUpdatedAt());
	}

	@Override
	@Transactional
	public TeacherProfileRDO approve(Long profileId, User reviewer) {
		TeacherProfile teacherProfile = findById(profileId);
		if (!TeacherProfileStatuses.PENDING_APPROVAL.equals(teacherProfile.getStatus())) {
			throw statusConflict("Only pending teacher profiles can be approved.");
		}
		teacherProfile.approve(reviewer.getId(), OffsetDateTime.now(ZoneOffset.UTC));
		TeacherProfile savedProfile = teacherProfileRepository.saveAndFlush(teacherProfile);
		notificationService.notifyTeacherProfileApproved(savedProfile.getUser().getId(), savedProfile.getId());
		return teacherProfileMapper.toRDO(savedProfile);
	}

	@Override
	@Transactional
	public TeacherProfileRDO reject(Long profileId, TeacherProfileModerationUDO command) {
		TeacherProfile teacherProfile = findById(profileId);
		if (!TeacherProfileStatuses.PENDING_APPROVAL.equals(teacherProfile.getStatus())) {
			throw statusConflict("Only pending teacher profiles can be rejected.");
		}
		String rejectionReason = trimToNull(command.rejectionReason());
		if (rejectionReason == null) {
			throw new BusinessException("VALIDATION_FAILED", "Rejection reason is required.", HttpStatus.BAD_REQUEST);
		}
		teacherProfile.reject(rejectionReason);
		TeacherProfile savedProfile = teacherProfileRepository.saveAndFlush(teacherProfile);
		notificationService.notifyTeacherProfileRejected(savedProfile.getUser().getId(), savedProfile.getId(), rejectionReason);
		return teacherProfileMapper.toRDO(savedProfile);
	}

	@Override
	@Transactional
	public TeacherProfileRDO suspend(Long profileId, TeacherProfileSuspensionUDO command) {
		TeacherProfile teacherProfile = findById(profileId);
		if (!TeacherProfileStatuses.APPROVED.equals(teacherProfile.getStatus())) {
			throw statusConflict("Only approved teacher profiles can be suspended.");
		}
		teacherProfile.suspend();
		TeacherProfile savedProfile = teacherProfileRepository.saveAndFlush(teacherProfile);
		String reason = trimToNull(command == null ? null : command.reason());
		notificationService.notifyTeacherProfileStatusChange(
				savedProfile.getUser().getId(), savedProfile.getId(), TeacherProfileStatuses.SUSPENDED, reason);
		return teacherProfileMapper.toRDO(savedProfile);
	}

	@Override
	@Transactional
	public TeacherProfileRDO reactivate(Long profileId) {
		TeacherProfile teacherProfile = findById(profileId);
		if (!TeacherProfileStatuses.SUSPENDED.equals(teacherProfile.getStatus())) {
			throw statusConflict("Only suspended teacher profiles can be reactivated.");
		}
		teacherProfile.reactivate();
		TeacherProfile savedProfile = teacherProfileRepository.saveAndFlush(teacherProfile);
		notificationService.notifyTeacherProfileStatusChange(
				savedProfile.getUser().getId(), savedProfile.getId(), TeacherProfileStatuses.APPROVED, null);
		return teacherProfileMapper.toRDO(savedProfile);
	}

	private TeacherProfile findByTeacher(User teacher) {
		return teacherProfileRepository.findByUserId(teacher.getId()).orElseThrow(this::notFound);
	}

	private TeacherProfile findById(Long profileId) {
		return teacherProfileRepository.findById(profileId).orElseThrow(this::notFound);
	}

	private BusinessException notFound() {
		return new BusinessException("TEACHER_PROFILE_NOT_FOUND", "Teacher profile was not found.", HttpStatus.NOT_FOUND);
	}

	private BusinessException statusConflict(String message) {
		return new BusinessException("TEACHER_PROFILE_STATUS_CONFLICT", message, HttpStatus.CONFLICT);
	}

	private String trimToNull(String value) {
		if (value == null || value.isBlank()) {
			return null;
		}
		return value.trim();
	}
}
