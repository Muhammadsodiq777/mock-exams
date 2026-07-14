package com.mockexams.backend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.mockexams.backend.entity.MarketplaceListing;

public interface MarketplaceListingRepository
		extends JpaRepository<MarketplaceListing, Long>, JpaSpecificationExecutor<MarketplaceListing> {

	@Query(value = """
			select listing.*
			from marketplace_listings listing
			join teacher_profiles teacher_profile on teacher_profile.id = listing.teacher_profile_id
			where listing.id = :listingId
			  and listing.is_active = true
			  and teacher_profile.status = 'approved'
			  and (
			    listing.price_amount = 0
			    or exists (
			      select 1
			      from purchases purchase
			      where purchase.listing_id = listing.id
			        and purchase.student_user_id = :studentUserId
			        and purchase.status = 'granted'
			        and (purchase.access_expires_at is null or purchase.access_expires_at > current_timestamp)
			    )
			    or exists (
			      select 1
			      from subscriptions subscription
			      where subscription.listing_id = listing.id
			        and subscription.student_user_id = :studentUserId
			        and subscription.status = 'active'
			        and subscription.starts_at <= current_timestamp
			        and subscription.ends_at > current_timestamp
			    )
			  )
			""", nativeQuery = true)
	Optional<MarketplaceListing> findAccessibleRecommendation(
			@Param("listingId") Long listingId,
			@Param("studentUserId") Long studentUserId);

	@Query("""
			select listing
			from MarketplaceListing listing
			join listing.teacherProfile teacherProfile
			where listing.active = true
			  and listing.priceAmount = 0
			  and teacherProfile.status = 'approved'
			order by listing.createdAt desc, listing.id desc
			""")
	List<MarketplaceListing> findActiveFree(Pageable pageable);

	@EntityGraph(attributePaths = { "teacherProfile", "test", "test.categories", "test.tags" })
	Optional<MarketplaceListing> findByIdAndTeacherProfile_Id(Long id, Long teacherProfileId);

	@EntityGraph(attributePaths = { "teacherProfile", "test", "test.categories", "test.tags" })
	@Query("""
			select listing
			from MarketplaceListing listing
			join listing.teacherProfile teacherProfile
			where listing.id = :listingId
			  and listing.active = true
			  and teacherProfile.status = 'approved'
			""")
	Optional<MarketplaceListing> findActivePublicById(@Param("listingId") Long listingId);

	@EntityGraph(attributePaths = { "teacherProfile", "test", "test.categories", "test.tags" })
	@Query("""
			select distinct listing
			from MarketplaceListing listing
			where listing.id in :listingIds
			""")
	List<MarketplaceListing> findReadModelsByIdIn(@Param("listingIds") List<Long> listingIds);

	@EntityGraph(attributePaths = { "teacherProfile", "test" })
	@Query("""
			select distinct listing
			from MarketplaceListing listing
			left join MarketplaceListingItem item on item.listing = listing
			where listing.test.id = :testId
			   or item.test.id = :testId
			order by listing.active desc, listing.createdAt desc, listing.id desc
			""")
	List<MarketplaceListing> findByReferencedTestId(@Param("testId") Long testId);
}
