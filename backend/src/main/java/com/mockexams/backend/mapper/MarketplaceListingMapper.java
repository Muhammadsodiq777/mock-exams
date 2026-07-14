package com.mockexams.backend.mapper;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Component;

import com.mockexams.backend.dto.AccessStatusRDO;
import com.mockexams.backend.dto.MarketplaceListingDetailRDO;
import com.mockexams.backend.dto.MarketplaceListingItemRDO;
import com.mockexams.backend.dto.MarketplaceListingSummaryRDO;
import com.mockexams.backend.dto.TestCategoryRDO;
import com.mockexams.backend.dto.TestTagRDO;
import com.mockexams.backend.dto.TestTeacherRDO;
import com.mockexams.backend.entity.CatalogTest;
import com.mockexams.backend.entity.MarketplaceListing;
import com.mockexams.backend.entity.MarketplaceListingItem;
import com.mockexams.backend.entity.TestCategory;
import com.mockexams.backend.entity.TestTag;
import com.mockexams.backend.teachers.entity.TeacherProfile;

@Component
public class MarketplaceListingMapper {

	public MarketplaceListingSummaryRDO toSummaryRDO(MarketplaceListing listing, int itemCount) {
		return toSummaryRDO(listing, itemCount, null);
	}

	public MarketplaceListingSummaryRDO toSummaryRDO(
			MarketplaceListing listing,
			int itemCount,
			AccessStatusRDO accessStatus
	) {
		CatalogTest test = listing.getTest();
		return new MarketplaceListingSummaryRDO(
				listing.getId(),
				listing.getTitle(),
				listing.getDescription(),
				listing.getListingType(),
				listing.getTestId(),
				listing.getPriceAmount(),
				listing.getCurrency(),
				isFree(listing),
				listing.isActive(),
				listing.getLanguage(),
				listing.getCreatedAt(),
				listing.getUpdatedAt(),
				toTeacherRDO(listing.getTeacherProfile()),
				toCategoryRDOs(test),
				toTagRDOs(test),
				itemCount,
				accessStatus);
	}

	public MarketplaceListingDetailRDO toDetailRDO(
			MarketplaceListing listing,
			List<MarketplaceListingItem> items
	) {
		return toDetailRDO(listing, items, null);
	}

	public MarketplaceListingDetailRDO toDetailRDO(
			MarketplaceListing listing,
			List<MarketplaceListingItem> items,
			AccessStatusRDO accessStatus
	) {
		CatalogTest test = listing.getTest();
		return new MarketplaceListingDetailRDO(
				listing.getId(),
				listing.getTitle(),
				listing.getDescription(),
				listing.getListingType(),
				listing.getTestId(),
				listing.getPriceAmount(),
				listing.getCurrency(),
				isFree(listing),
				listing.isActive(),
				listing.getLanguage(),
				listing.getCreatedAt(),
				listing.getUpdatedAt(),
				toTeacherRDO(listing.getTeacherProfile()),
				toCategoryRDOs(test),
				toTagRDOs(test),
				toItemRDOs(items),
				accessStatus);
	}

	private Boolean isFree(MarketplaceListing listing) {
		return listing.getPriceAmount() == null || BigDecimal.ZERO.compareTo(listing.getPriceAmount()) == 0;
	}

	private TestTeacherRDO toTeacherRDO(TeacherProfile teacherProfile) {
		if (teacherProfile == null) {
			return null;
		}
		return new TestTeacherRDO(
				teacherProfile.getId(),
				teacherProfile.getDisplayName(),
				teacherProfile.getHeadline(),
				teacherProfile.getRatingAverage(),
				teacherProfile.getRatingCount());
	}

	private List<TestCategoryRDO> toCategoryRDOs(CatalogTest test) {
		if (test == null) {
			return List.of();
		}
		return test.getCategories().stream()
				.filter(category -> Boolean.TRUE.equals(category.isActive()))
				.sorted(Comparator.comparing(TestCategory::getSortOrder).thenComparing(TestCategory::getName))
				.map(category -> new TestCategoryRDO(
						category.getId(),
						category.getParent() == null ? null : category.getParent().getId(),
						category.getName(),
						category.getSlug(),
						category.getSortOrder()))
				.toList();
	}

	private List<TestTagRDO> toTagRDOs(CatalogTest test) {
		if (test == null) {
			return List.of();
		}
		return test.getTags().stream()
				.sorted(Comparator.comparing(TestTag::getName))
				.map(tag -> new TestTagRDO(tag.getId(), tag.getName(), tag.getSlug()))
				.toList();
	}

	public List<MarketplaceListingItemRDO> toItemRDOs(List<MarketplaceListingItem> items) {
		return items.stream()
				.sorted(Comparator.comparing(MarketplaceListingItem::getItemOrder)
						.thenComparing(MarketplaceListingItem::getId))
				.map(item -> new MarketplaceListingItemRDO(
						item.getId(),
						item.getTestId(),
						item.getFileId(),
						item.getTitle(),
						item.getDescription(),
						item.getItemOrder()))
				.toList();
	}
}
