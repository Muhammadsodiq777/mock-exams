package com.mockexams.backend.repository;

import java.util.ArrayList;
import java.util.List;

import com.mockexams.backend.entity.CatalogTest;
import com.mockexams.backend.entity.TestCategory;
import com.mockexams.backend.entity.TestPrices;
import com.mockexams.backend.entity.TestTag;
import com.mockexams.backend.entity.TestVisibilities;
import com.mockexams.backend.service.TestCatalogSearchCriteria;
import com.mockexams.backend.teachers.TeacherProfileStatuses;
import com.mockexams.backend.teachers.entity.TeacherProfile;
import com.mockexams.backend.users.entity.Role;
import com.mockexams.backend.users.entity.UserRole;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;

import org.springframework.data.jpa.domain.Specification;

public final class TestCatalogSpecifications {

	private TestCatalogSpecifications() {
	}

	public static Specification<CatalogTest> publicCatalog(TestCatalogSearchCriteria criteria) {
		return (root, query, criteriaBuilder) -> {
			if (query != null) {
				query.distinct(true);
			}
			Join<CatalogTest, TeacherProfile> teacherProfile = root.join("teacherProfile", JoinType.LEFT);
			List<Predicate> predicates = new ArrayList<>();
			Predicate approvedTeacherTest = criteriaBuilder.equal(
					teacherProfile.get("status"),
					TeacherProfileStatuses.APPROVED);
			Predicate officialPlatformTest = criteriaBuilder.and(
					criteriaBuilder.isNull(root.get("teacherProfile")),
					createdByAdmin(root, query, criteriaBuilder));
			predicates.add(criteriaBuilder.and(
					criteriaBuilder.equal(root.get("visibility"), TestVisibilities.PUBLISHED),
					criteriaBuilder.isNull(root.get("deletedAt")),
					criteriaBuilder.or(approvedTeacherTest, officialPlatformTest)));

			if (criteria.type() != null) {
				predicates.add(criteriaBuilder.equal(root.get("type"), criteria.type()));
			}
			if (criteria.difficulty() != null) {
				predicates.add(criteriaBuilder.equal(root.get("difficulty"), criteria.difficulty()));
			}
			if (criteria.language() != null) {
				predicates.add(criteriaBuilder.equal(root.get("language"), criteria.language()));
			}
			if (criteria.price() != null) {
				predicates.add(criteriaBuilder.equal(root.get("free"), TestPrices.FREE.equals(criteria.price())));
			}
			if (criteria.teacherId() != null) {
				predicates.add(criteriaBuilder.equal(teacherProfile.get("id"), criteria.teacherId()));
				predicates.add(criteriaBuilder.equal(teacherProfile.get("status"), TeacherProfileStatuses.APPROVED));
			}
			if (criteria.bandMin() != null) {
				predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("estimatedBandMax"), criteria.bandMin()));
			}
			if (criteria.bandMax() != null) {
				predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("estimatedBandMin"), criteria.bandMax()));
			}
			if (!criteria.tags().isEmpty()) {
				Join<CatalogTest, TestTag> tag = root.join("tags", JoinType.INNER);
				predicates.add(tag.get("slug").in(criteria.tags()));
			}
			if (!criteria.categories().isEmpty()) {
				Join<CatalogTest, TestCategory> category = root.join("categories", JoinType.INNER);
				predicates.add(category.get("slug").in(criteria.categories()));
				predicates.add(criteriaBuilder.isTrue(category.get("active")));
			}
			return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
		};
	}

	private static Predicate createdByAdmin(
			Root<CatalogTest> root,
			CriteriaQuery<?> query,
			CriteriaBuilder criteriaBuilder
	) {
		Subquery<Integer> adminCreator = query.subquery(Integer.class);
		Root<UserRole> userRole = adminCreator.from(UserRole.class);
		Join<UserRole, Role> role = userRole.join("role");
		adminCreator.select(criteriaBuilder.literal(1))
				.where(
						criteriaBuilder.equal(userRole.get("id").get("userId"), root.get("createdByUserId")),
						criteriaBuilder.equal(role.get("code"), "admin"));
		return criteriaBuilder.exists(adminCreator);
	}
}
