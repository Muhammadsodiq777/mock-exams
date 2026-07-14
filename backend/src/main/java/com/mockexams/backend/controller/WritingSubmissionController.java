package com.mockexams.backend.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mockexams.backend.api.ApiEnvelope;
import com.mockexams.backend.auth.service.AuthorizationService;
import com.mockexams.backend.dto.WritingFeedbackRDO;
import com.mockexams.backend.dto.WritingSubmissionCDO;
import com.mockexams.backend.dto.WritingSubmissionRDO;
import com.mockexams.backend.service.WritingSubmissionService;
import com.mockexams.backend.users.RoleCodes;
import com.mockexams.backend.users.entity.User;

@RestController
@Validated
@RequestMapping("/api/v1/student/writing-submissions")
class WritingSubmissionController {

	private final AuthorizationService authorizationService;
	private final WritingSubmissionService writingSubmissionService;

	WritingSubmissionController(
			AuthorizationService authorizationService,
			WritingSubmissionService writingSubmissionService
	) {
		this.authorizationService = authorizationService;
		this.writingSubmissionService = writingSubmissionService;
	}

	@PostMapping
	ResponseEntity<ApiEnvelope<WritingSubmissionRDO>> submit(
			@RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
			@Valid @RequestBody WritingSubmissionCDO command
	) {
		User student = authorizationService.requireRole(authorizationHeader, RoleCodes.STUDENT);
		return ResponseEntity.status(HttpStatus.ACCEPTED)
				.body(ApiEnvelope.of(writingSubmissionService.submit(student, command)));
	}

	@GetMapping("/{submissionId}")
	ResponseEntity<ApiEnvelope<WritingSubmissionRDO>> getSubmission(
			@RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
			@PathVariable @Min(1) Long submissionId
	) {
		User student = authorizationService.requireRole(authorizationHeader, RoleCodes.STUDENT);
		return ResponseEntity.ok(ApiEnvelope.of(writingSubmissionService.getSubmission(student, submissionId)));
	}

	@GetMapping("/{submissionId}/feedback")
	ResponseEntity<ApiEnvelope<WritingFeedbackRDO>> getFeedback(
			@RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
			@PathVariable @Min(1) Long submissionId
	) {
		User student = authorizationService.requireRole(authorizationHeader, RoleCodes.STUDENT);
		return ResponseEntity.ok(ApiEnvelope.of(writingSubmissionService.getFeedback(student, submissionId)));
	}

	@PostMapping("/{submissionId}/feedback/retry")
	ResponseEntity<ApiEnvelope<WritingFeedbackRDO>> retryFeedback(
			@RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
			@PathVariable @Min(1) Long submissionId
	) {
		User student = authorizationService.requireRole(authorizationHeader, RoleCodes.STUDENT);
		return ResponseEntity.status(HttpStatus.ACCEPTED)
				.body(ApiEnvelope.of(writingSubmissionService.retryFeedback(student, submissionId)));
	}
}
