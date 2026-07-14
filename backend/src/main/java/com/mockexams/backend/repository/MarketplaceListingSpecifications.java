package com.mockexams.backend.repository;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import com.mockexams.backend.entity.CatalogTest;
import com.mockexams.backend.entity.MarketplaceListing;
import com.mockexams.backend.entity.MarketplaceListingItem;
import com.mockexams.backend.entity.MarketplaceListingTypes;
import com.mockexams.backend.entity.TestCategory;
import com.mockexams.backend.entity.TestPrices;
import com.mockexams.backend.entity.TestTag;
import com.mockexams.backend.entity.TestVisibilities;
import com.mockexams.backend.service.MarketplaceSearchCriteria;
import com.mockexams.backend.teachers.TeacherProfileStatuses;
import com.mockexams.backend.teachers.entity.TeacherProfile;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;

import org.springframework.data.jpa.domain.Specification;

public final class MarketplaceListingSpecifications {

	private MarketplaceListingSpecifications() {
	}

	public static Specification<MarketplaceListing> publicMarketplace(MarketplaceSearchCriteria criteria) {
		return (root, query, criteriaBuilder) -> {
			if (query != null) {
				query.distinct(true);
			}
			Join<MarketplaceListing, TeacherProfile> teacherProfile = root.join("teacherProfile", JoinType.INNER);
			Join<MarketplaceListing, CatalogTest> test = root.join("test", JoinType.LEFT);
			List<Predicate> predicates = new ArrayList<>();
			predicates.add(criteriaBuilder.isTrue(root.get("active")));
			predicates.add(criteriaBuilder.equal(teacherProfile.get("status"), TeacherProfileStatuses.APPROVED));
			predicates.add(criteriaBuilder.isNotNull(root.get("title")));
			predicates.add(criteriaBuilder.notEqual(root.get("title"), ""));
			predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("priceAmount"), BigDecimal.ZERO));
			predicates.add(criteriaBuilder.or(
					directTestListingIsValid(root, test, criteriaBuilder),
					itemListingHasOnlyValidItems(root, query, criteriaBuilder)));

			if (criteria.listingType() != null) {
				predicates.add(criteriaBuilder.equal(root.get("listingType"), criteria.listingType()));
			}
			if (criteria.teacherId() != null) {
				predicates.add(criteriaBuilder.equal(teacherProfile.get("id"), criteria.teacherId()));
			}
			if (criteria.language() != null) {
				predicates.add(criteriaBuilder.equal(root.get("language"), criteria.language()));
			}
			if (criteria.price() != null) {
				if (TestPrices.FREE.equals(criteria.price())) {
					predicates.add(criteriaBuilder.equal(root.get("priceAmount"), BigDecimal.ZERO));
				}
				else {
					predicates.add(criteriaBuilder.greaterThan(root.get("priceAmount"), BigDecimal.ZERO));
				}
			}
			if (criteria.minPrice() != null) {
				predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("priceAmount"), criteria.minPrice()));
			}
			if (criteria.maxPrice() != null) {
				predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("priceAmount"), criteria.maxPrice()));
			}
			if (!criteria.tags().isEmpty()) {
				Join<CatalogTest, TestTag> tag = test.join("tags", JoinType.INNER);
				predicates.add(tag.get("slug").in(criteria.tags()));
			}
			if (!criteria.categories().isEmpty()) {
				Join<CatalogTest, TestCategory> category = test.join("categories", JoinType.INNER);
				predicates.add(category.get("slug").in(criteria.categories()));
				predicates.add(criteriaBuilder.isTrue(category.get("active")));
			}
			if (criteria.query() != null) {
				String pattern = "%" + criteria.query().toLowerCase() + "%";
				predicates.add(criteriaBuilder.or(
						criteriaBuilder.like(criteriaBuilder.lower(root.get("title")), pattern),
						criteriaBuilder.like(criteriaBuilder.lower(root.get("description")), pattern),
						criteriaBuilder.like(criteriaBuilder.lower(teacherProfile.get("displayName")), pattern),
						criteriaBuilder.like(criteriaBuilder.lower(test.get("title")), pattern)));
			}
			return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
		};
	}

	public static Specification<MarketplaceListing> teacherOwned(Long teacherProfileId) {
		return (root, query, criteriaBuilder) -> criteriaBuilder.equal(
				root.get("teacherProfile").get("id"),
				teacherProfileId);
	}

	private static Predicate directTestListingIsValid(
			Root<MarketplaceListing> root,
			Join<MarketplaceListing, CatalogTest> test,
			CriteriaBuilder criteriaBuilder
	) {
		return criteriaBuilder.and(
				root.get("listingType").in(
						MarketplaceListingTypes.SINGLE_TEST,
						MarketplaceListingTypes.FULL_MOCK_EXAM),
				criteriaBuilder.isNotNull(test.get("id")),
				criteriaBuilder.equal(test.get("visibility"), TestVisibilities.PUBLISHED),
				criteriaBuilder.isNull(test.get("deletedAt")));
	}

	private static Predicate itemListingHasOnlyValidItems(
			Root<MarketplaceListing> root,
			CriteriaQuery<?> query,
			CriteriaBuilder criteriaBuilder
	) {
		Subquery<Integer> itemExists = query.subquery(Integer.class);
		Root<MarketplaceListingItem> item = itemExists.from(MarketplaceListingItem.class);
		itemExists.select(criteriaBuilder.literal(1))
				.where(
						criteriaBuilder.equal(item.get("listing").get("id"), root.get("id")));

		Subquery<Integer> invalidItemExists = query.subquery(Integer.class);
		Root<MarketplaceListingItem> invalidItem = invalidItemExists.from(MarketplaceListingItem.class);
		Join<MarketplaceListingItem, CatalogTest> invalidItemTest = invalidItem.join("test", JoinType.LEFT);
		var invalidItemFile = invalidItem.join("file", JoinType.LEFT);
		invalidItemExists.select(criteriaBuilder.literal(1))
				.where(
						criteriaBuilder.equal(invalidItem.get("listing").get("id"), root.get("id")),
						criteriaBuilder.isNull(invalidItemFile.get("id")),
						criteriaBuilder.or(
								criteriaBuilder.isNull(invalidItemTest.get("id")),
								criteriaBuilder.notEqual(invalidItemTest.get("visibility"), TestVisibilities.PUBLISHED),
								criteriaBuilder.isNotNull(invalidItemTest.get("deletedAt"))));
		return criteriaBuilder.and(
				root.get("listingType").in(
						MarketplaceListingTypes.TEST_PACK,
						MarketplaceListingTypes.WRITING_COURSE,
						MarketplaceListingTypes.SPEAKING_QUESTION_PACK,
						MarketplaceListingTypes.VOCABULARY_PACK,
						MarketplaceListingTypes.SUBSCRIPTION_PLAN),
				criteriaBuilder.exists(itemExists),
				criteriaBuilder.not(criteriaBuilder.exists(invalidItemExists)));
	}
}
