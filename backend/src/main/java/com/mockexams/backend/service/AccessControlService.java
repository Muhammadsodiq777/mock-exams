package com.mockexams.backend.service;

import java.util.List;

import com.mockexams.backend.dto.AccessStatusRDO;
import com.mockexams.backend.dto.MarketplaceListingItemRDO;
import com.mockexams.backend.dto.PurchaseRDO;
import com.mockexams.backend.entity.CatalogTest;
import com.mockexams.backend.entity.FileMetadata;
import com.mockexams.backend.users.entity.User;

public interface AccessControlService {

	PurchaseRDO grantFreeListingAccess(User student, Long listingId);

	PurchaseRDO grantInternalAccess(Long studentUserId, Long listingId, AccessGrantRequest request);

	AccessStatusRDO listingAccessStatus(User student, Long listingId);

	void requireListingAccess(User student, Long listingId);

	List<MarketplaceListingItemRDO> accessibleListingItems(User student, Long listingId);

	void requireTestAccess(User student, CatalogTest test);

	void requireFileDownloadAccess(User requester, FileMetadata file);

	List<PurchaseRDO> purchaseHistory(User student);

	PurchaseRDO purchaseDetail(User student, Long purchaseId);
}
