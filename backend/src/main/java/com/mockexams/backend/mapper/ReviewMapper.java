package com.mockexams.backend.mapper;

import org.springframework.stereotype.Component;

import com.mockexams.backend.dto.AdminReviewRDO;
import com.mockexams.backend.dto.PublicReviewRDO;
import com.mockexams.backend.dto.StudentReviewRDO;
import com.mockexams.backend.dto.TeacherReviewRDO;
import com.mockexams.backend.entity.CatalogTest;
import com.mockexams.backend.entity.Review;

@Component
public class ReviewMapper {

	public StudentReviewRDO toStudentRDO(Review review) {
		return new StudentReviewRDO(
				review.getId(),
				testId(review),
				ratingOf(review),
				review.getComment(),
				review.getCreatedAt(),
				review.getUpdatedAt());
	}

	public PublicReviewRDO toPublicRDO(Review review) {
		return new PublicReviewRDO(
				review.getId(),
				testId(review),
				testTitle(review),
				teacherProfileId(review),
				ratingOf(review),
				review.getComment(),
				review.getStudent().getFullName(),
				review.getCreatedAt());
	}

	public TeacherReviewRDO toTeacherRDO(Review review) {
		return new TeacherReviewRDO(
				review.getId(),
				testId(review),
				testTitle(review),
				ratingOf(review),
				review.getComment(),
				review.getStudent().getFullName(),
				review.getCreatedAt());
	}

	public AdminReviewRDO toAdminRDO(Review review) {
		return new AdminReviewRDO(
				review.getId(),
				testId(review),
				testTitle(review),
				teacherProfileId(review),
				review.getStudent().getId(),
				review.getStudent().getFullName(),
				ratingOf(review),
				review.getComment(),
				review.isHidden(),
				review.getHiddenAt(),
				review.getHiddenByUserId(),
				review.getCreatedAt(),
				review.getUpdatedAt());
	}

	private Integer ratingOf(Review review) {
		return review.getRating() == null ? null : review.getRating().intValue();
	}

	private Long testId(Review review) {
		return review.getTest() == null ? null : review.getTest().getId();
	}

	private String testTitle(Review review) {
		return review.getTest() == null ? null : review.getTest().getTitle();
	}

	private Long teacherProfileId(Review review) {
		CatalogTest test = review.getTest();
		return test == null || test.getTeacherProfile() == null ? null : test.getTeacherProfile().getId();
	}
}
