package com.mockexams.backend.teachers.service;

import com.mockexams.backend.users.entity.User;

public interface TeacherApprovalService {

	Long requireApprovedForPublishing(User teacher);
}
