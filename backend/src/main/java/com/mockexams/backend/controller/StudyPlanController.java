package com.mockexams.backend.controller;

import java.util.List;

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
import com.mockexams.backend.dto.StudyPlanCDO;
import com.mockexams.backend.dto.StudyPlanRDO;
import com.mockexams.backend.dto.StudyPlanTaskRDO;
import com.mockexams.backend.service.StudyPlanService;
import com.mockexams.backend.users.RoleCodes;
import com.mockexams.backend.users.entity.User;

@RestController
@Validated
@RequestMapping("/api/v1/student/study-plans")
class StudyPlanController {

	private final AuthorizationService authorizationService;
	private final StudyPlanService studyPlanService;

	StudyPlanController(
			AuthorizationService authorizationService,
			StudyPlanService studyPlanService
	) {
		this.authorizationService = authorizationService;
		this.studyPlanService = studyPlanService;
	}

	@PostMapping
	ResponseEntity<ApiEnvelope<StudyPlanRDO>> create(
			@RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
			@Valid @RequestBody StudyPlanCDO command
	) {
		User student = authorizationService.requireRole(authorizationHeader, RoleCodes.STUDENT);
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(ApiEnvelope.of(studyPlanService.create(student, command)));
	}

	@GetMapping("/{planId}")
	ResponseEntity<ApiEnvelope<StudyPlanRDO>> get(
			@RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
			@PathVariable @Min(1) Long planId
	) {
		User student = authorizationService.requireRole(authorizationHeader, RoleCodes.STUDENT);
		return ResponseEntity.ok(ApiEnvelope.of(studyPlanService.get(student, planId)));
	}

	@GetMapping("/active")
	ResponseEntity<ApiEnvelope<List<StudyPlanRDO>>> active(
			@RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader
	) {
		User student = authorizationService.requireRole(authorizationHeader, RoleCodes.STUDENT);
		return ResponseEntity.ok(ApiEnvelope.of(studyPlanService.activePlans(student)));
	}

	@GetMapping("/history")
	ResponseEntity<ApiEnvelope<List<StudyPlanRDO>>> history(
			@RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader
	) {
		User student = authorizationService.requireRole(authorizationHeader, RoleCodes.STUDENT);
		return ResponseEntity.ok(ApiEnvelope.of(studyPlanService.history(student)));
	}

	@PostMapping("/{planId}/regenerate")
	ResponseEntity<ApiEnvelope<StudyPlanRDO>> regenerate(
			@RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
			@PathVariable @Min(1) Long planId
	) {
		User student = authorizationService.requireRole(authorizationHeader, RoleCodes.STUDENT);
		return ResponseEntity.accepted()
				.body(ApiEnvelope.of(studyPlanService.regenerate(student, planId)));
	}

	@PostMapping("/{planId}/tasks/{taskId}/complete")
	ResponseEntity<ApiEnvelope<StudyPlanTaskRDO>> completeTask(
			@RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
			@PathVariable @Min(1) Long planId,
			@PathVariable @Min(1) Long taskId
	) {
		User student = authorizationService.requireRole(authorizationHeader, RoleCodes.STUDENT);
		return ResponseEntity.ok(ApiEnvelope.of(studyPlanService.completeTask(student, planId, taskId)));
	}

	@PostMapping("/{planId}/tasks/{taskId}/uncomplete")
	ResponseEntity<ApiEnvelope<StudyPlanTaskRDO>> uncompleteTask(
			@RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
			@PathVariable @Min(1) Long planId,
			@PathVariable @Min(1) Long taskId
	) {
		User student = authorizationService.requireRole(authorizationHeader, RoleCodes.STUDENT);
		return ResponseEntity.ok(ApiEnvelope.of(studyPlanService.uncompleteTask(student, planId, taskId)));
	}
}
