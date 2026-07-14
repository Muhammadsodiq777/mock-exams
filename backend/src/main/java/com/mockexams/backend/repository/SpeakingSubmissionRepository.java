package com.mockexams.backend.repository;

import java.util.List;
import java.util.Optional;

import jakarta.persistence.LockModeType;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.mockexams.backend.entity.SpeakingSubmission;

public interface SpeakingSubmissionRepository extends JpaRepository<SpeakingSubmission, Long> {

	@EntityGraph(attributePaths = { "studentUser", "testAttempt", "audioFile" })
	@Query("""
			select submission
			from SpeakingSubmission submission
			where submission.studentUser.id = :studentUserId
			  and submission.audioFile.id = :audioFileId
			  and submission.partType = :partType
			  and submission.prompt = :prompt
			  and (
			    (:testAttemptId is null and submission.testAttempt is null)
			    or submission.testAttempt.id = :testAttemptId
			  )
			order by submission.createdAt desc, submission.id desc
			""")
	List<SpeakingSubmission> findMatchingSubmission(
			@Param("studentUserId") Long studentUserId,
			@Param("audioFileId") Long audioFileId,
			@Param("partType") String partType,
			@Param("prompt") String prompt,
			@Param("testAttemptId") Long testAttemptId,
			Pageable pageable);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@EntityGraph(attributePaths = { "studentUser", "testAttempt", "audioFile" })
	@Query("select submission from SpeakingSubmission submission where submission.id = :id and submission.studentUser.id = :studentUserId")
	Optional<SpeakingSubmission> findByIdAndStudentUserIdForUpdate(@Param("id") Long id,
			@Param("studentUserId") Long studentUserId);

	@EntityGraph(attributePaths = { "studentUser", "testAttempt", "audioFile" })
	@Query("select submission from SpeakingSubmission submission where submission.id = :id and submission.studentUser.id = :studentUserId")
	Optional<SpeakingSubmission> findByIdAndStudentUserId(@Param("id") Long id,
			@Param("studentUserId") Long studentUserId);
}
