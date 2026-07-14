package com.mockexams.backend.repository;

import java.time.OffsetDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.mockexams.backend.entity.Subscription;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

	@EntityGraph(attributePaths = { "student", "listing" })
	List<Subscription> findByStudent_IdAndListing_Id(Long studentId, Long listingId);

	@Query("""
			select count(subscription) > 0
			from Subscription subscription
			where subscription.student.id = :studentId
			  and subscription.listing.id = :listingId
			  and subscription.status = 'active'
			  and subscription.endsAt <= :now
			""")
	boolean hasExpiredActiveSubscription(
			@Param("studentId") Long studentId,
			@Param("listingId") Long listingId,
			@Param("now") OffsetDateTime now);
}
