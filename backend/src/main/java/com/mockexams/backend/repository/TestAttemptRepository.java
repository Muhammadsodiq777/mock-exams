package com.mockexams.backend.repository;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import jakarta.persistence.LockModeType;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.mockexams.backend.dto.AdminTestActivityReportRowRDO;
import com.mockexams.backend.entity.TestAttempt;

public interface TestAttemptRepository extends JpaRepository<TestAttempt, Long> {

	boolean existsByStudent_IdAndTest_IdAndStatus(Long studentId, Long testId, String status);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@EntityGraph(attributePaths = { "test", "student" })
	@Query("select attempt from TestAttempt attempt where attempt.id = :id and attempt.student.id = :studentId")
	Optional<TestAttempt> findByIdAndStudentIdForUpdate(@Param("id") Long id, @Param("studentId") Long studentId);

	@EntityGraph(attributePaths = { "test", "student" })
	@Query("""
			select attempt
			from TestAttempt attempt
			where attempt.student.id = :studentId
			  and attempt.status in :statuses
			  and attempt.test.deletedAt is null
			  and attempt.test.visibility <> 'draft'
			order by coalesce(attempt.submittedAt, attempt.updatedAt, attempt.startedAt) desc, attempt.id desc
			""")
	List<TestAttempt> findRecentCompletedAttempts(
			@Param("studentId") Long studentId,
			@Param("statuses") Collection<String> statuses,
			Pageable pageable);

	@Query("""
			select new com.mockexams.backend.dto.AdminTestActivityReportRowRDO(
				cast(attempt.startedAt as LocalDate),
				teacherProfile.id,
				test.id,
				test.type,
				attempt.status,
				count(attempt),
				coalesce(sum(case when attempt.status = 'submitted' then 1 else 0 end), 0),
				coalesce(sum(case when attempt.status = 'expired' then 1 else 0 end), 0),
				coalesce(sum(case when attempt.status = 'in_progress' then 1 else 0 end), 0),
				coalesce(sum(attempt.timeSpentSeconds), 0),
				coalesce(avg(attempt.timeSpentSeconds), 0)
			)
			from TestAttempt attempt
			join attempt.test test
			left join test.teacherProfile teacherProfile
			where (:teacherProfileId is null or teacherProfile.id = :teacherProfileId)
			  and (:testType is null or test.type = :testType)
			  and (:from is null or attempt.startedAt >= :from)
			  and (:to is null or attempt.startedAt <= :to)
			  and test.deletedAt is null
			group by cast(attempt.startedAt as LocalDate), teacherProfile.id, test.id, test.type, attempt.status
			order by cast(attempt.startedAt as LocalDate) asc, teacherProfile.id asc, test.id asc, attempt.status asc
			""")
	List<AdminTestActivityReportRowRDO> activityReportRows(
			Long teacherProfileId,
			String testType,
			OffsetDateTime from,
			OffsetDateTime to);
}
