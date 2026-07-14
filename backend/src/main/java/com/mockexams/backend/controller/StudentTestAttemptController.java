package com.mockexams.backend.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
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
import com.mockexams.backend.dto.ResultDetailRDO;
import com.mockexams.backend.dto.StudentAnswerRDO;
import com.mockexams.backend.dto.StudentAnswerUDO;
import com.mockexams.backend.dto.StudentTestAttemptRDO;
import com.mockexams.backend.service.ScoringService;
import com.mockexams.backend.service.StudentTestAttemptService;
import com.mockexams.backend.users.RoleCodes;
import com.mockexams.backend.users.entity.User;

@RestController
@Validated
@RequestMapping("/api/v1/student")
class StudentTestAttemptController {

	private final AuthorizationService authorizationService;
	private final StudentTestAttemptService studentTestAttemptService;
	private final ScoringService scoringService;

	StudentTestAttemptController(
			AuthorizationService authorizationService,
			StudentTestAttemptService studentTestAttemptService,
			ScoringService scoringService
	) {
		this.authorizationService = authorizationService;
		this.studentTestAttemptService = studentTestAttemptService;
		this.scoringService = scoringService;
	}

	@PostMapping("/tests/{testId}/attempts")
	ResponseEntity<ApiEnvelope<StudentTestAttemptRDO>> startAttempt(
			@RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
			@PathVariable @Min(1) Long testId
	) {
		User student = authorizationService.requireRole(authorizationHeader, RoleCodes.STUDENT);
		return ResponseEntity.status(201)
				.body(ApiEnvelope.of(studentTestAttemptService.startAttempt(student, testId)));
	}

	@PatchMapping("/attempts/{attemptId}/answers")
	ResponseEntity<ApiEnvelope<StudentAnswerRDO>> saveAnswer(
			@RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
			@PathVariable @Min(1) Long attemptId,
			@Valid @RequestBody StudentAnswerUDO request
	) {
		User student = authorizationService.requireRole(authorizationHeader, RoleCodes.STUDENT);
		return ResponseEntity.ok(ApiEnvelope.of(studentTestAttemptService.saveAnswer(student, attemptId, request)));
	}

	@PostMapping("/attempts/{attemptId}/submit")
	ResponseEntity<ApiEnvelope<StudentTestAttemptRDO>> submitAttempt(
			@RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
			@PathVariable @Min(1) Long attemptId
	) {
		User student = authorizationService.requireRole(authorizationHeader, RoleCodes.STUDENT);
		return ResponseEntity.ok(ApiEnvelope.of(studentTestAttemptService.submitAttempt(student, attemptId)));
	}

	@GetMapping("/attempts/{attemptId}/result")
	ResponseEntity<ApiEnvelope<ResultDetailRDO>> getAttemptResult(
			@RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
			@PathVariable @Min(1) Long attemptId
	) {
		User student = authorizationService.requireRole(authorizationHeader, RoleCodes.STUDENT);
		return ResponseEntity.ok(ApiEnvelope.of(scoringService.getOrCreateResultForAttempt(student, attemptId)));
	}

	@GetMapping("/results/{resultId}")
	ResponseEntity<ApiEnvelope<ResultDetailRDO>> getResult(
			@RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
			@PathVariable @Min(1) Long resultId
	) {
		User student = authorizationService.requireRole(authorizationHeader, RoleCodes.STUDENT);
		return ResponseEntity.ok(ApiEnvelope.of(scoringService.getResult(student, resultId)));
	}
}
