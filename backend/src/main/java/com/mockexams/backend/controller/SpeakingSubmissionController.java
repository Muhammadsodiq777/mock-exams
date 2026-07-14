package com.mockexams.backend.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;

import org.springframework.http.HttpHeaders;
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
import com.mockexams.backend.dto.SpeakingFeedbackRDO;
import com.mockexams.backend.dto.SpeakingSubmissionCDO;
import com.mockexams.backend.dto.SpeakingSubmissionRDO;
import com.mockexams.backend.service.SpeakingSubmissionService;
import com.mockexams.backend.users.RoleCodes;
import com.mockexams.backend.users.entity.User;

@RestController
@Validated
@RequestMapping("/api/v1/student/speaking-submissions")
class SpeakingSubmissionController {

	private final AuthorizationService authorizationService;
	private final SpeakingSubmissionService speakingSubmissionService;

	SpeakingSubmissionController(
			AuthorizationService authorizationService,
			SpeakingSubmissionService speakingSubmissionService
	) {
		this.authorizationService = authorizationService;
		this.speakingSubmissionService = speakingSubmissionService;
	}

	@PostMapping
	ResponseEntity<ApiEnvelope<SpeakingSubmissionRDO>> submit(
			@RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
			@Valid @RequestBody SpeakingSubmissionCDO request
	) {
		User student = authorizationService.requireRole(authorizationHeader, RoleCodes.STUDENT);
		SpeakingSubmissionService.SubmissionResult result = speakingSubmissionService.submit(student, request);
		return ResponseEntity.status(result.created() ? 201 : 200).body(ApiEnvelope.of(result.submission()));
	}

	@GetMapping("/{submissionId}")
	ResponseEntity<ApiEnvelope<SpeakingSubmissionRDO>> get(
			@RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
			@PathVariable @Min(1) Long submissionId
	) {
		User student = authorizationService.requireRole(authorizationHeader, RoleCodes.STUDENT);
		return ResponseEntity.ok(ApiEnvelope.of(speakingSubmissionService.get(student, submissionId)));
	}

	@PostMapping("/{submissionId}/transcription")
	ResponseEntity<ApiEnvelope<SpeakingSubmissionRDO>> startTranscription(
			@RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
			@PathVariable @Min(1) Long submissionId
	) {
		User student = authorizationService.requireRole(authorizationHeader, RoleCodes.STUDENT);
		return ResponseEntity.accepted()
				.body(ApiEnvelope.of(speakingSubmissionService.startTranscription(student, submissionId)));
	}

	@GetMapping("/{submissionId}/feedback")
	ResponseEntity<ApiEnvelope<SpeakingFeedbackRDO>> getFeedback(
			@RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
			@PathVariable @Min(1) Long submissionId
	) {
		User student = authorizationService.requireRole(authorizationHeader, RoleCodes.STUDENT);
		return ResponseEntity.ok(ApiEnvelope.of(speakingSubmissionService.getFeedback(student, submissionId)));
	}

	@PostMapping("/{submissionId}/feedback")
	ResponseEntity<ApiEnvelope<SpeakingFeedbackRDO>> startFeedback(
			@RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
			@PathVariable @Min(1) Long submissionId
	) {
		User student = authorizationService.requireRole(authorizationHeader, RoleCodes.STUDENT);
		return ResponseEntity.accepted()
				.body(ApiEnvelope.of(speakingSubmissionService.startFeedback(student, submissionId)));
	}

	@PostMapping("/{submissionId}/feedback/retry")
	ResponseEntity<ApiEnvelope<SpeakingFeedbackRDO>> retryFeedback(
			@RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
			@PathVariable @Min(1) Long submissionId
	) {
		User student = authorizationService.requireRole(authorizationHeader, RoleCodes.STUDENT);
		return ResponseEntity.accepted()
				.body(ApiEnvelope.of(speakingSubmissionService.retryFeedback(student, submissionId)));
	}
}
