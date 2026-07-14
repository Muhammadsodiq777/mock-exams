package com.mockexams.backend.repository;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.mockexams.backend.entity.TestResult;

public interface TestResultRepository extends JpaRepository<TestResult, Long> {

	@EntityGraph(attributePaths = { "attempt", "attempt.test", "attempt.student" })
	Optional<TestResult> findByAttemptId(Long attemptId);

	@EntityGraph(attributePaths = { "attempt", "attempt.test", "attempt.student" })
	@Query("""
			select result
			from TestResult result
			where result.id = :id
			  and result.attempt.student.id = :studentId
			""")
	Optional<TestResult> findByIdAndStudentId(@Param("id") Long id, @Param("studentId") Long studentId);

	@EntityGraph(attributePaths = { "attempt", "attempt.test", "attempt.student" })
	@Query("""
			select result
			from TestResult result
			where result.attempt.student.id = :studentId
			  and result.attempt.test.type in :testTypes
			  and result.createdAt >= :from
			order by result.createdAt desc, result.id desc
			""")
	List<TestResult> findRecentObjectiveResults(
			@Param("studentId") Long studentId,
			@Param("testTypes") Collection<String> testTypes,
			@Param("from") OffsetDateTime from,
			Pageable pageable);

	@EntityGraph(attributePaths = { "attempt", "attempt.test", "attempt.student" })
	@Query("""
			select result
			from TestResult result
			where result.attempt.id in :attemptIds
			""")
	List<TestResult> findByAttemptIdIn(@Param("attemptIds") Collection<Long> attemptIds);

	@Query("""
			select count(result)
			from TestResult result
			where result.attempt.student.id = :studentId
			  and result.attempt.test.type in :testTypes
			  and result.createdAt >= :from
			""")
	long countObjectiveResults(
			@Param("studentId") Long studentId,
			@Param("testTypes") Collection<String> testTypes,
			@Param("from") OffsetDateTime from);
}
