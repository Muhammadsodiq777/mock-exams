package com.mockexams.backend.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.mockexams.backend.entity.WritingSubmission;

public interface WritingSubmissionRepository extends JpaRepository<WritingSubmission, Long> {

	@EntityGraph(attributePaths = { "studentUser", "testAttempt" })
	Optional<WritingSubmission> findByIdAndStudentUserId(Long id, Long studentUserId);
}
