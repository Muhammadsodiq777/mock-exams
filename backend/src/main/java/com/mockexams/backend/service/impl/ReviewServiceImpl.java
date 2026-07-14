package com.mockexams.backend.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mockexams.backend.dto.AdminReviewRDO;
import com.mockexams.backend.dto.PublicReviewRDO;
import com.mockexams.backend.dto.ReviewCDO;
import com.mockexams.backend.dto.ReviewUDO;
import com.mockexams.backend.dto.StudentReviewRDO;
import com.mockexams.backend.dto.TeacherReviewRDO;
import com.mockexams.backend.dto.TeacherReviewSummaryRDO;
import com.mockexams.backend.entity.CatalogTest;
import com.mockexams.backend.entity.MarketplaceListing;
import com.mockexams.backend.entity.PurchaseStatuses;
import com.mockexams.backend.entity.Review;
import com.mockexams.backend.entity.TestAttemptStatuses;
import com.mockexams.backend.error.BusinessException;
import com.mockexams.backend.mapper.ReviewMapper;
import com.mockexams.backend.repository.MarketplaceListingRepository;
import com.mockexams.backend.repository.PurchaseRepository;
import com.mockexams.backend.repository.ReviewRepository;
import com.mockexams.backend.repository.ReviewSpecifications;
import com.mockexams.backend.repository.TestAttemptRepository;
import com.mockexams.backend.repository.TestCatalogRepository;
import com.mockexams.backend.service.AdminReviewSearchCriteria;
import com.mockexams.backend.service.ReviewSearchCriteria;
import com.mockexams.backend.service.ReviewService;
import com.mockexams.backend.teachers.TeacherProfileStatuses;
import com.mockexams.backend.teachers.entity.TeacherProfile;
import com.mockexams.backend.teachers.repository.TeacherProfileRepository;
import com.mockexams.backend.users.entity.User;

/**
 * Review eligibility currently accepts a granted marketplace purchase for a listing referencing the
 * test, or a submitted attempt for the test. Live speaking session eligibility is deferred until
 * plan 0067 lands and should be added to {@link #isEligibleToReview(User, CatalogTest)}.
 *
 * <p>Teacher rating aggregates are recalculated from the countable reviews (non-hidden reviews of
 * non-deleted tests) inside the same transaction as every review mutation, so create, update, hide,
 * unhide, and delete always leave {@code teacher_profiles.rating_average/rating_count} consistent.
 */
@Service
class ReviewServiceImpl implements ReviewService {

	private static final int RATING_SCALE = 2;

	private final ReviewRepository reviewRepository;
	private final TestCatalogRepository testCatalogRepository;
	private final TestAttemptRepository testAttemptRepository;
	private final PurchaseRepository purchaseRepository;
	private final MarketplaceListingRepository listingRepository;
	private final TeacherProfileRepository teacherProfileRepository;
	private final ReviewMapper reviewMapper;

	ReviewServiceImpl(
			ReviewRepository reviewRepository,
			TestCatalogRepository testCatalogRepository,
			TestAttemptRepository testAttemptRepository,
			PurchaseRepository purchaseRepository,
			MarketplaceListingRepository listingRepository,
			TeacherProfileRepository teacherProfileRepository,
			ReviewMapper reviewMapper
	) {
		this.reviewRepository = reviewRepository;
		this.testCatalogRepository = testCatalogRepository;
		this.testAttemptRepository = testAttemptRepository;
		this.purchaseRepository = purchaseRepository;
		this.listingRepository = listingRepository;
		this.teacherProfileRepository = teacherProfileRepository;
		this.reviewMapper = reviewMapper;
	}

	@Override
	@Transactional
	public StudentReviewRDO createTestReview(User student, Long testId, ReviewCDO command) {
		CatalogTest test = testCatalogRepository.findVisibleDetailById(testId)
				.orElseThrow(this::testNotFound);
		if (!isEligibleToReview(student, test)) {
			throw reviewNotEligible();
		}
		if (reviewRepository.findByStudent_IdAndTest_Id(student.getId(), test.getId()).isPresent()) {
			throw reviewAlreadyExists();
		}
		Review review;
		try {
			review = reviewRepository.saveAndFlush(
					Review.forTest(student, test, command.rating(), trimToNull(command.comment())));
		}
		catch (DataIntegrityViolationException ex) {
			throw reviewAlreadyExists();
		}
		recalculateTeacherRating(test.getTeacherProfile());
		return reviewMapper.toStudentRDO(review);
	}

	@Override
	@Transactional
	public StudentReviewRDO updateOwnReview(User student, Long reviewId, ReviewUDO command) {
		Review review = reviewRepository.findByIdAndStudent_Id(reviewId, student.getId())
				.orElseThrow(this::reviewNotFound);
		review.updateContent(command.rating(), trimToNull(command.comment()));
		reviewRepository.saveAndFlush(review);
		recalculateTeacherRating(review.getTest() == null ? null : review.getTest().getTeacherProfile());
		return reviewMapper.toStudentRDO(review);
	}

	@Override
	@Transactional(readOnly = true)
	public Page<PublicReviewRDO> publicTestReviews(Long testId, ReviewSearchCriteria criteria, Pageable pageable) {
		CatalogTest test = testCatalogRepository.findVisibleDetailById(testId)
				.orElseThrow(this::testNotFound);
		Specification<Review> specification = ReviewSpecifications.countable()
				.and(ReviewSpecifications.forTest(test.getId()))
				.and(searchSpecification(criteria));
		return reviewRepository.findAll(specification, pageable).map(reviewMapper::toPublicRDO);
	}

	@Override
	@Transactional(readOnly = true)
	public Page<PublicReviewRDO> publicTeacherReviews(
			Long teacherProfileId,
			ReviewSearchCriteria criteria,
			Pageable pageable
	) {
		TeacherProfile profile = teacherProfileRepository
				.findByIdAndStatus(teacherProfileId, TeacherProfileStatuses.APPROVED)
				.orElseThrow(this::teacherProfileNotFound);
		Specification<Review> specification = ReviewSpecifications.countable()
				.and(ReviewSpecifications.forTeacherProfile(profile.getId()))
				.and(ReviewSpecifications.forTest(criteria.testId()))
				.and(searchSpecification(criteria));
		return reviewRepository.findAll(specification, pageable).map(reviewMapper::toPublicRDO);
	}

	@Override
	@Transactional(readOnly = true)
	public Page<TeacherReviewRDO> teacherContentReviews(User teacher, ReviewSearchCriteria criteria, Pageable pageable) {
		TeacherProfile profile = requireTeacherProfile(teacher);
		Specification<Review> specification = ReviewSpecifications.countable()
				.and(ReviewSpecifications.forTeacherProfile(profile.getId()))
				.and(ReviewSpecifications.forTest(criteria.testId()))
				.and(searchSpecification(criteria));
		return reviewRepository.findAll(specification, pageable).map(reviewMapper::toTeacherRDO);
	}

	@Override
	@Transactional(readOnly = true)
	public TeacherReviewSummaryRDO teacherReviewSummary(User teacher) {
		TeacherProfile profile = requireTeacherProfile(teacher);
		RatingSnapshot snapshot = ratingSnapshot(profile.getId());
		return new TeacherReviewSummaryRDO(snapshot.average(), snapshot.count());
	}

	@Override
	@Transactional(readOnly = true)
	public Page<AdminReviewRDO> adminReviews(AdminReviewSearchCriteria criteria, Pageable pageable) {
		Specification<Review> specification = ReviewSpecifications.forTeacherProfile(criteria.teacherProfileId())
				.and(ReviewSpecifications.forTest(criteria.testId()))
				.and(ReviewSpecifications.withRating(criteria.rating()))
				.and(ReviewSpecifications.withHidden(criteria.hidden()))
				.and(ReviewSpecifications.createdFrom(criteria.from()))
				.and(ReviewSpecifications.createdTo(criteria.to()));
		return reviewRepository.findAll(specification, pageable).map(reviewMapper::toAdminRDO);
	}

	@Override
	@Transactional
	public AdminReviewRDO adminHide(User admin, Long reviewId) {
		Review review = reviewRepository.findById(reviewId).orElseThrow(this::reviewNotFound);
		review.hide(admin.getId(), now());
		reviewRepository.saveAndFlush(review);
		recalculateTeacherRating(review.getTest() == null ? null : review.getTest().getTeacherProfile());
		return reviewMapper.toAdminRDO(review);
	}

	@Override
	@Transactional
	public AdminReviewRDO adminUnhide(User admin, Long reviewId) {
		Review review = reviewRepository.findById(reviewId).orElseThrow(this::reviewNotFound);
		review.unhide();
		reviewRepository.saveAndFlush(review);
		recalculateTeacherRating(review.getTest() == null ? null : review.getTest().getTeacherProfile());
		return reviewMapper.toAdminRDO(review);
	}

	@Override
	@Transactional
	public void adminDelete(User admin, Long reviewId) {
		Review review = reviewRepository.findById(reviewId).orElseThrow(this::reviewNotFound);
		TeacherProfile teacherProfile = review.getTest() == null ? null : review.getTest().getTeacherProfile();
		reviewRepository.delete(review);
		reviewRepository.flush();
		recalculateTeacherRating(teacherProfile);
	}

	private boolean isEligibleToReview(User student, CatalogTest test) {
		if (testAttemptRepository.existsByStudent_IdAndTest_IdAndStatus(
				student.getId(), test.getId(), TestAttemptStatuses.SUBMITTED)) {
			return true;
		}
		List<Long> listingIds = listingRepository.findByReferencedTestId(test.getId()).stream()
				.map(MarketplaceListing::getId)
				.toList();
		// Future (plan 0067): completed live speaking sessions should also grant eligibility.
		return !listingIds.isEmpty() && purchaseRepository.existsByStudent_IdAndListing_IdInAndStatus(
				student.getId(), listingIds, PurchaseStatuses.GRANTED);
	}

	private Specification<Review> searchSpecification(ReviewSearchCriteria criteria) {
		return ReviewSpecifications.withRating(criteria.rating())
				.and(ReviewSpecifications.createdFrom(criteria.from()))
				.and(ReviewSpecifications.createdTo(criteria.to()));
	}

	private void recalculateTeacherRating(TeacherProfile teacherProfile) {
		if (teacherProfile == null) {
			return;
		}
		RatingSnapshot snapshot = ratingSnapshot(teacherProfile.getId());
		teacherProfile.updateRating(snapshot.average(), snapshot.count());
		teacherProfileRepository.save(teacherProfile);
	}

	private RatingSnapshot ratingSnapshot(Long teacherProfileId) {
		ReviewRepository.TeacherRatingAggregate aggregate = reviewRepository.aggregateTeacherRating(teacherProfileId);
		long count = aggregate.getReviewCount() == null ? 0 : aggregate.getReviewCount();
		BigDecimal average = count == 0 || aggregate.getAverage() == null
				? BigDecimal.ZERO.setScale(RATING_SCALE, RoundingMode.HALF_UP)
				: BigDecimal.valueOf(aggregate.getAverage()).setScale(RATING_SCALE, RoundingMode.HALF_UP);
		return new RatingSnapshot(average, Math.toIntExact(count));
	}

	private TeacherProfile requireTeacherProfile(User teacher) {
		return teacherProfileRepository.findByUserId(teacher.getId())
				.orElseThrow(this::teacherProfileNotFound);
	}

	private String trimToNull(String value) {
		if (value == null || value.isBlank()) {
			return null;
		}
		return value.trim();
	}

	private OffsetDateTime now() {
		return OffsetDateTime.now(ZoneOffset.UTC);
	}

	private BusinessException testNotFound() {
		return new BusinessException("TEST_NOT_FOUND", "Test was not found.", HttpStatus.NOT_FOUND);
	}

	private BusinessException teacherProfileNotFound() {
		return new BusinessException("TEACHER_PROFILE_NOT_FOUND", "Teacher profile was not found.", HttpStatus.NOT_FOUND);
	}

	private BusinessException reviewNotFound() {
		return new BusinessException("REVIEW_NOT_FOUND", "Review was not found.", HttpStatus.NOT_FOUND);
	}

	private BusinessException reviewNotEligible() {
		return new BusinessException(
				"REVIEW_NOT_ELIGIBLE",
				"Reviews require a purchase or a submitted attempt for this test.",
				HttpStatus.FORBIDDEN);
	}

	private BusinessException reviewAlreadyExists() {
		return new BusinessException(
				"REVIEW_ALREADY_EXISTS",
				"You already reviewed this test. Use the review update endpoint instead.",
				HttpStatus.CONFLICT);
	}

	private record RatingSnapshot(BigDecimal average, int count) {
	}
}
