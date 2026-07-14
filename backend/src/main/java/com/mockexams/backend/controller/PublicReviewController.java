package com.mockexams.backend.controller;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.mockexams.backend.api.ApiEnvelope;
import com.mockexams.backend.api.ApiMeta;
import com.mockexams.backend.api.ApiPageMeta;
import com.mockexams.backend.api.ApiSortMeta;
import com.mockexams.backend.dto.PublicReviewRDO;
import com.mockexams.backend.service.ReviewSearchCriteria;
import com.mockexams.backend.service.ReviewService;

@RestController
@Validated
@RequestMapping("/api/v1/public")
class PublicReviewController {

	private static final int DEFAULT_PAGE_SIZE = 20;
	private static final int MAX_PAGE_SIZE = 100;
	private static final String SORT_FIELD = "createdAt";
	private static final Sort.Direction SORT_DIRECTION = Sort.Direction.DESC;

	private final ReviewService reviewService;

	PublicReviewController(ReviewService reviewService) {
		this.reviewService = reviewService;
	}

	@GetMapping("/tests/{testId}/reviews")
	ResponseEntity<ApiEnvelope<List<PublicReviewRDO>>> testReviews(
			@PathVariable @Min(1) Long testId,
			@RequestParam(required = false) @Min(1) @Max(5) Integer rating,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to,
			@RequestParam(defaultValue = "0") @Min(0) int page,
			@RequestParam(defaultValue = "" + DEFAULT_PAGE_SIZE) @Min(1) @Max(MAX_PAGE_SIZE) int size
	) {
		ReviewSearchCriteria criteria = new ReviewSearchCriteria(null, rating, from, to);
		Page<PublicReviewRDO> reviews = reviewService.publicTestReviews(testId, criteria, pageRequest(page, size));
		return ResponseEntity.ok(ApiEnvelope.of(reviews.getContent(), meta(reviews, filters(null, rating, from, to))));
	}

	@GetMapping("/teacher-profiles/{profileId}/reviews")
	ResponseEntity<ApiEnvelope<List<PublicReviewRDO>>> teacherReviews(
			@PathVariable @Min(1) Long profileId,
			@RequestParam(required = false) @Min(1) Long testId,
			@RequestParam(required = false) @Min(1) @Max(5) Integer rating,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to,
			@RequestParam(defaultValue = "0") @Min(0) int page,
			@RequestParam(defaultValue = "" + DEFAULT_PAGE_SIZE) @Min(1) @Max(MAX_PAGE_SIZE) int size
	) {
		ReviewSearchCriteria criteria = new ReviewSearchCriteria(testId, rating, from, to);
		Page<PublicReviewRDO> reviews = reviewService.publicTeacherReviews(profileId, criteria, pageRequest(page, size));
		return ResponseEntity.ok(ApiEnvelope.of(reviews.getContent(), meta(reviews, filters(testId, rating, from, to))));
	}

	private PageRequest pageRequest(int page, int size) {
		return PageRequest.of(page, size, Sort.by(Sort.Order.desc(SORT_FIELD), Sort.Order.desc("id")));
	}

	private ApiMeta meta(Page<?> reviews, Map<String, Object> filters) {
		return ApiMeta.create(
				ApiPageMeta.from(reviews),
				List.of(new ApiSortMeta(SORT_FIELD, SORT_DIRECTION.name().toLowerCase())),
				filters);
	}

	private Map<String, Object> filters(Long testId, Integer rating, OffsetDateTime from, OffsetDateTime to) {
		Map<String, Object> filters = new LinkedHashMap<>();
		if (testId != null) {
			filters.put("testId", testId);
		}
		if (rating != null) {
			filters.put("rating", rating);
		}
		if (from != null) {
			filters.put("from", from);
		}
		if (to != null) {
			filters.put("to", to);
		}
		return filters;
	}
}
