package com.mockexams.backend.repository.schedules;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.mockexams.backend.entity.schedules.GoogleCalendarConnection;

public interface GoogleCalendarConnectionRepository extends JpaRepository<GoogleCalendarConnection, Long> {

	Optional<GoogleCalendarConnection> findByTeacherProfile_Id(Long teacherProfileId);
}
