package com.mockexams.backend.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.mockexams.backend.entity.Notification;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

	Page<Notification> findByUserId(Long userId, Pageable pageable);

	Optional<Notification> findByIdAndUserId(Long id, Long userId);

	long countByUserIdAndReadAtIsNull(Long userId);
}
