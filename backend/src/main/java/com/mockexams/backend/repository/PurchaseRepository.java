package com.mockexams.backend.repository;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.mockexams.backend.entity.Purchase;

public interface PurchaseRepository extends JpaRepository<Purchase, Long> {

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@EntityGraph(attributePaths = { "student", "listing", "listing.teacherProfile", "listing.test" })
	Optional<Purchase> findByStudent_IdAndListing_Id(Long studentId, Long listingId);

	@EntityGraph(attributePaths = { "student", "listing", "listing.teacherProfile", "listing.test" })
	Optional<Purchase> findByIdAndStudent_Id(Long id, Long studentId);

	@EntityGraph(attributePaths = { "student", "listing", "listing.teacherProfile", "listing.test" })
	List<Purchase> findByStudent_IdOrderByPurchasedAtDescIdDesc(Long studentId);

	@EntityGraph(attributePaths = { "student", "listing", "listing.teacherProfile", "listing.test" })
	@Query("""
			select purchase
			from Purchase purchase
			where purchase.student.id = :studentId
			  and purchase.listing.id = :listingId
			""")
	Optional<Purchase> findGrantForAccessCheck(
			@Param("studentId") Long studentId,
			@Param("listingId") Long listingId);

	boolean existsByStudent_IdAndListing_IdInAndStatus(Long studentId, Collection<Long> listingIds, String status);

	@Query("""
			select count(purchase) > 0
			from Purchase purchase
			where purchase.student.id = :studentId
			  and purchase.listing.id = :listingId
			  and purchase.status = 'granted'
			  and purchase.accessExpiresAt is not null
			  and purchase.accessExpiresAt <= :now
			""")
	boolean hasExpiredGrant(
			@Param("studentId") Long studentId,
			@Param("listingId") Long listingId,
			@Param("now") OffsetDateTime now);
}
