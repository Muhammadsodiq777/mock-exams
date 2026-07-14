package com.mockexams.backend.repository;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.mockexams.backend.dto.AdminDashboardStatusCountRDO;
import com.mockexams.backend.entity.WithdrawalRequest;

public interface WithdrawalRequestRepository extends JpaRepository<WithdrawalRequest, Long> {

	@Query("""
			select new com.mockexams.backend.dto.AdminDashboardStatusCountRDO(request.status, count(request))
			from WithdrawalRequest request
			where (:from is null or request.createdAt >= :from)
			  and (:to is null or request.createdAt <= :to)
			group by request.status
			""")
	List<AdminDashboardStatusCountRDO> countGroupedByStatusInWindow(OffsetDateTime from, OffsetDateTime to);

	@Query("""
			select coalesce(sum(request.requestedAmount), 0)
			from WithdrawalRequest request
			where (:from is null or request.createdAt >= :from)
			  and (:to is null or request.createdAt <= :to)
			""")
	BigDecimal sumRequestedAmountInWindow(OffsetDateTime from, OffsetDateTime to);

	long countByStatus(String status);

	List<WithdrawalRequest> findTop5ByStatusOrderByCreatedAtDescIdDesc(String status);

	Optional<WithdrawalRequest> findByIdAndTeacherProfile_Id(Long id, Long teacherProfileId);

	Page<WithdrawalRequest> findByTeacherProfile_Id(Long teacherProfileId, Pageable pageable);

	Page<WithdrawalRequest> findByTeacherProfile_IdAndStatus(Long teacherProfileId, String status, Pageable pageable);

	@Query(value = """
			select w from WithdrawalRequest w
			join fetch w.teacherProfile p
			join fetch p.user
			where (:status is null or w.status = :status)
			and (:teacherProfileId is null or p.id = :teacherProfileId)
			""",
			countQuery = """
					select count(w) from WithdrawalRequest w
					where (:status is null or w.status = :status)
					and (:teacherProfileId is null or w.teacherProfile.id = :teacherProfileId)
					""")
	Page<WithdrawalRequest> adminQueue(
			@Param("status") String status,
			@Param("teacherProfileId") Long teacherProfileId,
			Pageable pageable);
}
