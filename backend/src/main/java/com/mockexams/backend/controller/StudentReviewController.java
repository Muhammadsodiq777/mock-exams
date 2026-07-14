package com.mockexams.backend.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mockexams.backend.api.ApiEnvelope;
import com.mockexams.backend.auth.service.AuthorizationService;
import com.mockexams.backend.dto.ReviewCDO;
import com.mockexams.backend.dto.ReviewUDO;
import com.mockexams.backend.dto.StudentReviewRDO;
import com.mockexams.backend.service.ReviewService;
import com.mockexams.backend.users.RoleCodes;
import com.mockexams.backend.users.entity.User;

@RestController
@Validated
@RequestMapping("/api/v1/student")
class StudentReviewController {

	private final AuthorizationService authorizationService;
	private final ReviewService reviewService;

	StudentReviewController(AuthorizationService authorizationService, ReviewService reviewService) {
		this.authorizationService = authorizationService;
		this.reviewService = reviewService;
	}

	@PostMapping("/tests/{testId}/reviews")
	ResponseEntity<ApiEnvelope<StudentReviewRDO>> createReview(
			@RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
			@PathVariable @Min(1) Long testId,
			@Valid @RequestBody ReviewCDO command
	) {
		User student = authorizationService.requireRole(authorizationHeader, RoleCodes.STUDENT);
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(ApiEnvelope.of(reviewService.createTestReview(student, testId, command)));
	}

	@PatchMapping("/reviews/{reviewId}")
	ResponseEntity<ApiEnvelope<StudentReviewRDO>> updateReview(
			@RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
			@PathVariable @Min(1) Long reviewId,
			@Valid @RequestBody ReviewUDO command
	) {
		User student = authorizationService.requireRole(authorizationHeader, RoleCodes.STUDENT);
		return ResponseEntity.ok(ApiEnvelope.of(reviewService.updateOwnReview(student, reviewId, command)));
	}
}
