package com.mockexams.backend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.mockexams.backend.entity.MarketplaceListingItem;

public interface MarketplaceListingItemRepository extends JpaRepository<MarketplaceListingItem, Long> {

	@EntityGraph(attributePaths = { "test", "file" })
	List<MarketplaceListingItem> findByListingIdOrderByItemOrderAscIdAsc(Long listingId);

	@EntityGraph(attributePaths = { "test", "file" })
	List<MarketplaceListingItem> findByListingIdInOrderByListingIdAscItemOrderAscIdAsc(List<Long> listingIds);

	Optional<MarketplaceListingItem> findByIdAndListingId(Long id, Long listingId);

	long countByListingId(Long listingId);

	@EntityGraph(attributePaths = { "listing", "listing.teacherProfile", "test", "file" })
	List<MarketplaceListingItem> findByFile_IdOrderByItemOrderAscIdAsc(Long fileId);
}
