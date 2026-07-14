package com.mockexams.backend.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.mockexams.backend.entity.StudyPlan;

public interface StudyPlanRepository extends JpaRepository<StudyPlan, Long> {

	@EntityGraph(attributePaths = "studentUser")
	Optional<StudyPlan> findByIdAndStudentUserId(Long id, Long studentUserId);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@EntityGraph(attributePaths = "studentUser")
	Optional<StudyPlan> findForUpdateByIdAndStudentUserId(Long id, Long studentUserId);

	@Query("""
			select plan
			from StudyPlan plan
			where plan.studentUser.id = :studentUserId
			  and plan.startsAt <= :today
			  and plan.endsAt >= :today
			order by plan.createdAt desc, plan.id desc
			""")
	List<StudyPlan> findActive(
			@Param("studentUserId") Long studentUserId,
			@Param("today") LocalDate today);

	@Query("""
			select plan
			from StudyPlan plan
			where plan.studentUser.id = :studentUserId
			  and (plan.startsAt > :today or plan.endsAt < :today)
			order by plan.createdAt desc, plan.id desc
			""")
	List<StudyPlan> findHistory(
			@Param("studentUserId") Long studentUserId,
			@Param("today") LocalDate today);
}
