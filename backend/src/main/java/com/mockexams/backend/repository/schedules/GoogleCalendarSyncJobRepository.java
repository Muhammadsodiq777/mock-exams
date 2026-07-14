package com.mockexams.backend.repository.schedules;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.mockexams.backend.entity.schedules.GoogleCalendarSyncJob;

public interface GoogleCalendarSyncJobRepository extends JpaRepository<GoogleCalendarSyncJob, Long> {

	boolean existsByBooking_IdAndOperation(Long bookingId, String operation);

	@EntityGraph(attributePaths = { "booking", "booking.slot", "booking.student", "teacherProfile", "teacherProfile.user" })
	Optional<GoogleCalendarSyncJob> findFirstByBooking_IdAndOperationOrderByCreatedAtDesc(Long bookingId, String operation);

	@EntityGraph(attributePaths = { "booking", "booking.slot", "booking.student", "teacherProfile", "teacherProfile.user" })
	Page<GoogleCalendarSyncJob> findByStatus(String status, Pageable pageable);
}
