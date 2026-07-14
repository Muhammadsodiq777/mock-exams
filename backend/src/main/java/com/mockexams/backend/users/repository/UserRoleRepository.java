package com.mockexams.backend.users.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.mockexams.backend.users.entity.UserRole;
import com.mockexams.backend.users.entity.UserRoleId;

public interface UserRoleRepository extends JpaRepository<UserRole, UserRoleId> {
}
