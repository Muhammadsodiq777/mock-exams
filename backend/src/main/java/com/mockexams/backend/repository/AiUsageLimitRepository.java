package com.mockexams.backend.repository;

import java.time.LocalDate;
import java.util.Optional;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import com.mockexams.backend.entity.AiUsageLimit;

public interface AiUsageLimitRepository extends JpaRepository<AiUsageLimit, Long> {

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	Optional<AiUsageLimit> findForUpdateById(Long id);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("""
			select limit
			from AiUsageLimit limit
			where limit.user.id = :userId
			  and limit.periodStart = :periodStart
			  and limit.periodEnd = :periodEnd
			""")
	Optional<AiUsageLimit> findActiveForUpdate(Long userId, LocalDate periodStart, LocalDate periodEnd);
}
