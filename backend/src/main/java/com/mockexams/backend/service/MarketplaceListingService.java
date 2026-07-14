package com.mockexams.backend.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.mockexams.backend.dto.MarketplaceListingActivationRDO;
import com.mockexams.backend.dto.MarketplaceListingCDO;
import com.mockexams.backend.dto.MarketplaceListingDetailRDO;
import com.mockexams.backend.dto.MarketplaceListingItemCDO;
import com.mockexams.backend.dto.MarketplaceListingItemUDO;
import com.mockexams.backend.dto.MarketplaceListingSummaryRDO;
import com.mockexams.backend.dto.MarketplaceListingUDO;
import com.mockexams.backend.users.entity.User;

public interface MarketplaceListingService {

	Page<MarketplaceListingSummaryRDO> listPublic(MarketplaceSearchCriteria criteria, Pageable pageable);

	MarketplaceListingDetailRDO publicDetail(Long listingId);

	Page<MarketplaceListingSummaryRDO> listTeacherListings(User teacher, Pageable pageable);

	MarketplaceListingDetailRDO create(User teacher, MarketplaceListingCDO command);

	MarketplaceListingDetailRDO update(User teacher, Long listingId, MarketplaceListingUDO command);

	MarketplaceListingDetailRDO preview(User teacher, Long listingId);

	MarketplaceListingDetailRDO addItem(User teacher, Long listingId, MarketplaceListingItemCDO command);

	MarketplaceListingDetailRDO updateItem(
			User teacher,
			Long listingId,
			Long itemId,
			MarketplaceListingItemUDO command);

	MarketplaceListingDetailRDO deleteItem(User teacher, Long listingId, Long itemId);

	MarketplaceListingActivationRDO activationStatus(User teacher, Long listingId);

	MarketplaceListingDetailRDO activate(User teacher, Long listingId);

	MarketplaceListingDetailRDO deactivate(User teacher, Long listingId);

	void delete(User teacher, Long listingId);
}
