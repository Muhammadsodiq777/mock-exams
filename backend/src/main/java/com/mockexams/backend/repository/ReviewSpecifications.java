package com.mockexams.backend.repository;

import java.time.OffsetDateTime;

import org.springframework.data.jpa.domain.Specification;

import com.mockexams.backend.entity.Review;

public final class ReviewSpecifications {

	private ReviewSpecifications() {
	}

	/**
	 * Reviews that count for public lists and teacher rating aggregates: not hidden by moderation
	 * and not attached to a soft-deleted test.
	 */
	public static Specification<Review> countable() {
		return (root, query, builder) -> builder.and(
				builder.isNull(root.get("hiddenAt")),
				builder.isNull(root.get("test").get("deletedAt")));
	}

	public static Specification<Review> forTest(Long testId) {
		return (root, query, builder) -> testId == null
				? null
				: builder.equal(root.get("test").get("id"), testId);
	}

	public static Specification<Review> forTeacherProfile(Long teacherProfileId) {
		return (root, query, builder) -> teacherProfileId == null
				? null
				: builder.equal(root.get("test").get("teacherProfile").get("id"), teacherProfileId);
	}

	public static Specification<Review> withRating(Integer rating) {
		return (root, query, builder) -> rating == null
				? null
				: builder.equal(root.get("rating"), rating.shortValue());
	}

	public static Specification<Review> createdFrom(OffsetDateTime from) {
		return (root, query, builder) -> from == null
				? null
				: builder.greaterThanOrEqualTo(root.get("createdAt"), from);
	}

	public static Specification<Review> createdTo(OffsetDateTime to) {
		return (root, query, builder) -> to == null
				? null
				: builder.lessThanOrEqualTo(root.get("createdAt"), to);
	}

	public static Specification<Review> withHidden(Boolean hidden) {
		return (root, query, builder) -> {
			if (hidden == null) {
				return null;
			}
			return hidden ? builder.isNotNull(root.get("hiddenAt")) : builder.isNull(root.get("hiddenAt"));
		};
	}
}
