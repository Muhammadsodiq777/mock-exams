package com.mockexams.backend.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.mockexams.backend.entity.StudyPlanTask;

public interface StudyPlanTaskRepository extends JpaRepository<StudyPlanTask, Long> {

	List<StudyPlanTask> findByStudyPlanIdOrderByTaskDateAscTaskOrderAscIdAsc(Long studyPlanId);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	Optional<StudyPlanTask> findForUpdateByIdAndStudyPlanId(Long id, Long studyPlanId);

	@Modifying
	@Query("""
			delete from StudyPlanTask task
			where task.studyPlan.id = :studyPlanId
			  and task.completedAt is null
			  and task.taskDate >= :fromDate
			""")
	int deleteIncompleteFromDate(
			@Param("studyPlanId") Long studyPlanId,
			@Param("fromDate") LocalDate fromDate);

	@Query("""
			select coalesce(max(task.taskOrder), 0)
			from StudyPlanTask task
			where task.studyPlan.id = :studyPlanId
			  and task.taskDate = :taskDate
			""")
	int maxOrderForDate(
			@Param("studyPlanId") Long studyPlanId,
			@Param("taskDate") LocalDate taskDate);
}
