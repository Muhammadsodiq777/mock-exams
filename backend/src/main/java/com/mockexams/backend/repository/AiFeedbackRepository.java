package com.mockexams.backend.repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import jakarta.persistence.LockModeType;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import com.mockexams.backend.dto.AdminAiUsageAggregateRowRDO;
import com.mockexams.backend.dto.AdminAiUsageReportTotalsRDO;
import com.mockexams.backend.dto.AdminProviderFailureReportRowRDO;
import com.mockexams.backend.entity.AiFeedback;

public interface AiFeedbackRepository extends JpaRepository<AiFeedback, Long> {

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	Optional<AiFeedback> findForUpdateById(Long id);

	List<AiFeedback> findByWritingSubmissionIdAndFeedbackTypeOrderByCreatedAtDescIdDesc(
			Long writingSubmissionId,
			String feedbackType);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	List<AiFeedback> findForUpdateByWritingSubmissionIdAndFeedbackTypeOrderByCreatedAtDescIdDesc(
			Long writingSubmissionId,
			String feedbackType);

	List<AiFeedback> findBySpeakingSubmissionIdAndFeedbackTypeOrderByCreatedAtDescIdDesc(
			Long speakingSubmissionId,
			String feedbackType);

	List<AiFeedback> findTop5ByStudentUserIdAndFeedbackTypeAndStatusAndOverallBandIsNotNullOrderByCreatedAtDescIdDesc(
			Long studentUserId,
			String feedbackType,
			String status);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	List<AiFeedback> findForUpdateBySpeakingSubmissionIdAndFeedbackTypeOrderByCreatedAtDescIdDesc(
			Long speakingSubmissionId,
			String feedbackType);

	@Query("""
			select feedback
			from AiFeedback feedback
			join feedback.studentUser studentUser
			where (:studentUserId is null or studentUser.id = :studentUserId)
			  and (:feedbackType is null or feedback.feedbackType = :feedbackType)
			  and (:status is null or feedback.status = :status)
			  and (:provider is null or feedback.provider = :provider)
			  and (:from is null or feedback.createdAt >= :from)
			  and (:to is null or feedback.createdAt <= :to)
			""")
	Page<AiFeedback> searchReport(
			Long studentUserId,
			String feedbackType,
			String status,
			String provider,
			OffsetDateTime from,
			OffsetDateTime to,
			Pageable pageable);

	@Query("""
			select new com.mockexams.backend.dto.AdminAiUsageReportTotalsRDO(
				count(feedback),
				coalesce(sum(case when feedback.status = 'completed' then 1 else 0 end), 0),
				coalesce(sum(case
					when feedback.status = 'failed'
					 and (feedback.failureCode is null
					  or feedback.failureCode not in ('VALIDATION_FAILED', 'AI_USAGE_LIMIT_EXCEEDED'))
					then 1 else 0 end), 0),
				coalesce(sum(case when feedback.failureCode = 'VALIDATION_FAILED' then 1 else 0 end), 0),
				coalesce(sum(case when feedback.failureCode = 'AI_USAGE_LIMIT_EXCEEDED' then 1 else 0 end), 0),
				coalesce(sum(feedback.promptTokens), 0),
				coalesce(sum(feedback.completionTokens), 0),
				coalesce(sum(feedback.estimatedCost), 0)
			)
			from AiFeedback feedback
			join feedback.studentUser studentUser
			where (:studentUserId is null or studentUser.id = :studentUserId)
			  and (:feedbackType is null or feedback.feedbackType = :feedbackType)
			  and (:status is null or feedback.status = :status)
			  and (:provider is null or feedback.provider = :provider)
			  and (:from is null or feedback.createdAt >= :from)
			  and (:to is null or feedback.createdAt <= :to)
			""")
	AdminAiUsageReportTotalsRDO summarizeReport(
			Long studentUserId,
			String feedbackType,
			String status,
			String provider,
			OffsetDateTime from,
			OffsetDateTime to);

	@Query("""
			select new com.mockexams.backend.dto.AdminAiUsageAggregateRowRDO(
				cast(feedback.createdAt as LocalDate),
				studentUser.id,
				feedback.feedbackType,
				feedback.status,
				feedback.provider,
				feedback.model,
				feedback.failureCode,
				count(feedback),
				coalesce(sum(case when feedback.failureCode = 'VALIDATION_FAILED' then 1 else 0 end), 0),
				coalesce(sum(case when feedback.failureCode = 'AI_USAGE_LIMIT_EXCEEDED' then 1 else 0 end), 0),
				coalesce(sum(feedback.promptTokens), 0),
				coalesce(sum(feedback.completionTokens), 0),
				coalesce(sum(feedback.estimatedCost), 0)
			)
			from AiFeedback feedback
			join feedback.studentUser studentUser
			where (:studentUserId is null or studentUser.id = :studentUserId)
			  and (:feedbackType is null or feedback.feedbackType = :feedbackType)
			  and (:status is null or feedback.status = :status)
			  and (:provider is null or feedback.provider = :provider)
			  and (:model is null or feedback.model = :model)
			  and (:from is null or feedback.createdAt >= :from)
			  and (:to is null or feedback.createdAt <= :to)
			group by cast(feedback.createdAt as LocalDate), studentUser.id, feedback.feedbackType,
				feedback.status, feedback.provider, feedback.model, feedback.failureCode
			order by cast(feedback.createdAt as LocalDate) asc, studentUser.id asc, feedback.feedbackType asc,
				feedback.status asc, feedback.provider asc, feedback.model asc, feedback.failureCode asc
			""")
	List<AdminAiUsageAggregateRowRDO> aggregateReport(
			Long studentUserId,
			String feedbackType,
			String status,
			String provider,
			String model,
			OffsetDateTime from,
			OffsetDateTime to);

	@Query("""
			select new com.mockexams.backend.dto.AdminProviderFailureReportRowRDO(
				cast(feedback.createdAt as LocalDate),
				'ai',
				feedback.provider,
				feedback.status,
				feedback.failureCode,
				feedback.model,
				null,
				null,
				count(feedback),
				0,
				coalesce(sum(feedback.estimatedCost), 0)
			)
			from AiFeedback feedback
			where feedback.status = 'failed'
			  and (feedback.failureCode is null or feedback.failureCode <> 'AI_USAGE_LIMIT_EXCEEDED')
			  and (:provider is null or feedback.provider = :provider)
			  and (:model is null or feedback.model = :model)
			  and (:status is null or feedback.status = :status)
			  and (:from is null or feedback.createdAt >= :from)
			  and (:to is null or feedback.createdAt <= :to)
			group by cast(feedback.createdAt as LocalDate), feedback.provider, feedback.status,
				feedback.failureCode, feedback.model
			order by cast(feedback.createdAt as LocalDate) asc, feedback.provider asc, feedback.failureCode asc,
				feedback.model asc
			""")
	List<AdminProviderFailureReportRowRDO> aiFailureReportRows(
			String provider,
			String model,
			String status,
			OffsetDateTime from,
			OffsetDateTime to);
}
