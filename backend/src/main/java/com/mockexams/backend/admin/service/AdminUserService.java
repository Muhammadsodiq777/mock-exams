package com.mockexams.backend.admin.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.mockexams.backend.admin.dto.AdminUserRDO;
import com.mockexams.backend.admin.dto.AdminUserStatusUDO;

public interface AdminUserService {

	Page<AdminUserRDO> listUsers(String status, String role, String teacherStatus, String email, Pageable pageable);

	AdminUserRDO getUser(Long userId);

	AdminUserRDO updateStatus(Long userId, AdminUserStatusUDO command);
}
