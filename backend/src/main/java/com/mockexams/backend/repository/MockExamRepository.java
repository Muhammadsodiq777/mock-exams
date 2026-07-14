package com.mockexams.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.mockexams.backend.entity.MockExam;

public interface MockExamRepository extends JpaRepository<MockExam, Long> {
}
