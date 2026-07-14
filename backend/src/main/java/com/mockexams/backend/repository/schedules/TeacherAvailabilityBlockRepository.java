package com.mockexams.backend.repository.schedules;

import org.springframework.data.jpa.repository.JpaRepository;

import com.mockexams.backend.entity.schedules.TeacherAvailabilityBlock;

public interface TeacherAvailabilityBlockRepository extends JpaRepository<TeacherAvailabilityBlock, Long> {
}
