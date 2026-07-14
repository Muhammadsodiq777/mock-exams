package com.mockexams.backend.students.controller;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mockexams.backend.api.ApiEnvelope;
import com.mockexams.backend.auth.dto.CurrentUserRDO;
import com.mockexams.backend.auth.service.AuthService;
import com.mockexams.backend.auth.service.AuthorizationService;
import com.mockexams.backend.users.RoleCodes;

@RestController
@Validated
@RequestMapping("/api/v1/student")
class StudentSessionController {

	private final AuthorizationService authorizationService;
	private final AuthService authService;

	StudentSessionController(AuthorizationService authorizationService, AuthService authService) {
		this.authorizationService = authorizationService;
		this.authService = authService;
	}

	@GetMapping("/session")
	ResponseEntity<ApiEnvelope<CurrentUserRDO>> session(
			@RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader
	) {
		return ResponseEntity.ok(ApiEnvelope.of(
				authService.currentUser(authorizationService.requireRole(authorizationHeader, RoleCodes.STUDENT))));
	}
}
