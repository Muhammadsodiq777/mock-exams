package com.mockexams.backend.users.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.mockexams.backend.users.entity.Role;

public interface RoleRepository extends JpaRepository<Role, Short> {

	Optional<Role> findByCode(String code);
}
