package com.mockexams.backend.controller;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.mockexams.backend.api.ApiEnvelope;
import com.mockexams.backend.api.ApiMeta;
import com.mockexams.backend.auth.service.AuthorizationService;
import com.mockexams.backend.dto.StudentProgressDashboardRDO;
import com.mockexams.backend.service.StudentProgressDashboard;
import com.mockexams.backend.service.StudentProgressService;
import com.mockexams.backend.users.RoleCodes;
import com.mockexams.backend.users.entity.User;

@RestController
@Validated
class StudentProgressController {

	private static final int DEFAULT_WINDOW_DAYS = 180;
	private static final int MAX_WINDOW_DAYS = 365;

	private final AuthorizationService authorizationService;
	private final StudentProgressService studentProgressService;

	StudentProgressController(
			AuthorizationService authorizationService,
			StudentProgressService studentProgressService
	) {
		this.authorizationService = authorizationService;
		this.studentProgressService = studentProgressService;
	}

	@GetMapping("/api/v1/student/progress")
	ResponseEntity<ApiEnvelope<StudentProgressDashboardRDO>> progress(
			@RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
			@RequestParam(defaultValue = "" + DEFAULT_WINDOW_DAYS) @Min(1) @Max(MAX_WINDOW_DAYS) int days
	) {
		User student = authorizationService.requireRole(authorizationHeader, RoleCodes.STUDENT);
		StudentProgressDashboard dashboard = studentProgressService.dashboard(student, days);
		return ResponseEntity.ok(ApiEnvelope.of(
				dashboard.data(),
				ApiMeta.dashboard(dashboard.asOf(), dashboard.window())));
	}
}
