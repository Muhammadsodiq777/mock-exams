package com.mockexams.backend.teachers.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.mockexams.backend.teachers.dto.AdminTeacherProfileDetailRDO;
import com.mockexams.backend.teachers.dto.TeacherProfileCDO;
import com.mockexams.backend.teachers.dto.TeacherProfileModerationUDO;
import com.mockexams.backend.teachers.dto.TeacherProfilePublicRDO;
import com.mockexams.backend.teachers.dto.TeacherProfileRDO;
import com.mockexams.backend.teachers.dto.TeacherProfileSuspensionUDO;
import com.mockexams.backend.teachers.dto.TeacherProfileUDO;
import com.mockexams.backend.users.entity.User;

public interface TeacherProfileService {

	TeacherProfileRDO createMyProfile(User teacher, TeacherProfileCDO command);

	TeacherProfileRDO myProfile(User teacher);

	TeacherProfileRDO updateMyProfile(User teacher, TeacherProfileUDO command);

	TeacherProfileRDO submitMyProfile(User teacher);

	Page<TeacherProfilePublicRDO> listApprovedPublicProfiles(Pageable pageable);

	TeacherProfilePublicRDO publicProfile(Long profileId);

	Page<TeacherProfileRDO> listAdminProfiles(String status, Pageable pageable);

	AdminTeacherProfileDetailRDO adminProfileDetail(Long profileId);

	TeacherProfileRDO approve(Long profileId, User reviewer);

	TeacherProfileRDO reject(Long profileId, TeacherProfileModerationUDO command);

	TeacherProfileRDO suspend(Long profileId, TeacherProfileSuspensionUDO command);

	TeacherProfileRDO reactivate(Long profileId);
}
