package com.mockexams.backend.teachers.service.impl;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mockexams.backend.error.BusinessException;
import com.mockexams.backend.teachers.TeacherProfileStatuses;
import com.mockexams.backend.teachers.entity.TeacherProfile;
import com.mockexams.backend.teachers.repository.TeacherProfileRepository;
import com.mockexams.backend.teachers.service.TeacherApprovalService;
import com.mockexams.backend.users.entity.User;

@Service
class TeacherApprovalServiceImpl implements TeacherApprovalService {

	private final TeacherProfileRepository teacherProfileRepository;

	TeacherApprovalServiceImpl(TeacherProfileRepository teacherProfileRepository) {
		this.teacherProfileRepository = teacherProfileRepository;
	}

	@Override
	@Transactional(readOnly = true)
	public Long requireApprovedForPublishing(User teacher) {
		TeacherProfile teacherProfile = teacherProfileRepository.findByUserId(teacher.getId())
				.orElseThrow(this::approvalRequired);
		if (!TeacherProfileStatuses.APPROVED.equals(teacherProfile.getStatus())) {
			throw approvalRequired();
		}
		return teacherProfile.getId();
	}

	private BusinessException approvalRequired() {
		return new BusinessException(
				"TEACHER_APPROVAL_REQUIRED",
				"Teacher profile must be approved before publishing content.",
				HttpStatus.FORBIDDEN);
	}
}
