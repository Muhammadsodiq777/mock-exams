package com.mockexams.backend.mapper;

import org.springframework.stereotype.Component;

import com.mockexams.backend.dto.AccessStatusRDO;
import com.mockexams.backend.dto.PurchaseRDO;
import com.mockexams.backend.entity.MarketplaceListing;
import com.mockexams.backend.entity.Purchase;

@Component
public class PurchaseMapper {

	public PurchaseRDO toRDO(Purchase purchase, String paymentStatus, AccessStatusRDO accessStatus) {
		MarketplaceListing listing = purchase.getListing();
		return new PurchaseRDO(
				purchase.getId(),
				purchase.getStudent().getId(),
				listing.getId(),
				listing.getTitle(),
				listing.getListingType(),
				purchase.getAmount(),
				purchase.getCurrency(),
				purchase.getStatus(),
				purchase.getSourceType(),
				purchase.getSourceReference(),
				purchase.getPaymentId(),
				paymentStatus,
				purchase.getPurchasedAt(),
				purchase.getAccessExpiresAt(),
				accessStatus);
	}
}
