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
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.mockexams.backend.api.ApiEnvelope;
import com.mockexams.backend.api.ApiMeta;
import com.mockexams.backend.api.ApiPageMeta;
import com.mockexams.backend.api.ApiSortMeta;
import com.mockexams.backend.auth.service.AuthorizationService;
import com.mockexams.backend.dto.AdminReviewRDO;
import com.mockexams.backend.service.AdminReviewSearchCriteria;
import com.mockexams.backend.service.ReviewService;
import com.mockexams.backend.users.RoleCodes;
import com.mockexams.backend.users.entity.User;

@RestController
@Validated
@RequestMapping("/api/v1/admin/reviews")
class AdminReviewController {

	private static final int DEFAULT_PAGE_SIZE = 20;
	private static final int MAX_PAGE_SIZE = 100;
	private static final String SORT_FIELD = "createdAt";
	private static final Sort.Direction SORT_DIRECTION = Sort.Direction.DESC;

	private final AuthorizationService authorizationService;
	private final ReviewService reviewService;

	AdminReviewController(AuthorizationService authorizationService, ReviewService reviewService) {
		this.authorizationService = authorizationService;
		this.reviewService = reviewService;
	}

	@GetMapping
	ResponseEntity<ApiEnvelope<List<AdminReviewRDO>>> reviews(
			@RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
			@RequestParam(required = false) @Min(1) Long teacherProfileId,
			@RequestParam(required = false) @Min(1) Long testId,
			@RequestParam(required = false) @Min(1) @Max(5) Integer rating,
			@RequestParam(required = false) Boolean hidden,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to,
			@RequestParam(defaultValue = "0") @Min(0) int page,
			@RequestParam(defaultValue = "" + DEFAULT_PAGE_SIZE) @Min(1) @Max(MAX_PAGE_SIZE) int size
	) {
		authorizationService.requireRole(authorizationHeader, RoleCodes.ADMIN);
		Page<AdminReviewRDO> reviews = reviewService.adminReviews(
				new AdminReviewSearchCriteria(teacherProfileId, testId, rating, hidden, from, to),
				PageRequest.of(page, size, Sort.by(Sort.Order.desc(SORT_FIELD), Sort.Order.desc("id"))));
		ApiMeta meta = ApiMeta.create(
				ApiPageMeta.from(reviews),
				List.of(new ApiSortMeta(SORT_FIELD, SORT_DIRECTION.name().toLowerCase())),
				filters(teacherProfileId, testId, rating, hidden, from, to));
		return ResponseEntity.ok(ApiEnvelope.of(reviews.getContent(), meta));
	}

	@PostMapping("/{reviewId}/hide")
	ResponseEntity<ApiEnvelope<AdminReviewRDO>> hide(
			@RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
			@PathVariable @Min(1) Long reviewId
	) {
		User admin = authorizationService.requireRole(authorizationHeader, RoleCodes.ADMIN);
		return ResponseEntity.ok(ApiEnvelope.of(reviewService.adminHide(admin, reviewId)));
	}

	@PostMapping("/{reviewId}/unhide")
	ResponseEntity<ApiEnvelope<AdminReviewRDO>> unhide(
			@RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
			@PathVariable @Min(1) Long reviewId
	) {
		User admin = authorizationService.requireRole(authorizationHeader, RoleCodes.ADMIN);
		return ResponseEntity.ok(ApiEnvelope.of(reviewService.adminUnhide(admin, reviewId)));
	}

	@DeleteMapping("/{reviewId}")
	ResponseEntity<ApiEnvelope<Void>> delete(
			@RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
			@PathVariable @Min(1) Long reviewId
	) {
		User admin = authorizationService.requireRole(authorizationHeader, RoleCodes.ADMIN);
		reviewService.adminDelete(admin, reviewId);
		return ResponseEntity.ok(ApiEnvelope.of(null));
	}

	private Map<String, Object> filters(
			Long teacherProfileId,
			Long testId,
			Integer rating,
			Boolean hidden,
			OffsetDateTime from,
			OffsetDateTime to
	) {
		Map<String, Object> filters = new LinkedHashMap<>();
		if (teacherProfileId != null) {
			filters.put("teacherProfileId", teacherProfileId);
		}
		if (testId != null) {
			filters.put("testId", testId);
		}
		if (rating != null) {
			filters.put("rating", rating);
		}
		if (hidden != null) {
			filters.put("hidden", hidden);
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
