package com.mockexams.backend.auth.controller;

import jakarta.validation.Valid;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mockexams.backend.api.ApiEnvelope;
import com.mockexams.backend.auth.dto.AuthSessionRDO;
import com.mockexams.backend.auth.dto.CurrentUserRDO;
import com.mockexams.backend.auth.dto.LoginCDO;
import com.mockexams.backend.auth.dto.RegisterStudentCDO;
import com.mockexams.backend.auth.dto.RegisterTeacherCDO;
import com.mockexams.backend.auth.service.AuthService;
import com.mockexams.backend.auth.service.AuthorizationService;

@RestController
@Validated
@RequestMapping({ "/api/v1/auth", "/api/auth" })
class AuthController {

	private final AuthService authService;
	private final AuthorizationService authorizationService;

	AuthController(AuthService authService, AuthorizationService authorizationService) {
		this.authService = authService;
		this.authorizationService = authorizationService;
	}

	@PostMapping("/register/student")
	ResponseEntity<ApiEnvelope<AuthSessionRDO>> registerStudent(@Valid @RequestBody RegisterStudentCDO command) {
		return ResponseEntity.status(HttpStatus.CREATED).body(ApiEnvelope.of(authService.registerStudent(command)));
	}

	@PostMapping("/register/teacher")
	ResponseEntity<ApiEnvelope<AuthSessionRDO>> registerTeacher(@Valid @RequestBody RegisterTeacherCDO command) {
		return ResponseEntity.status(HttpStatus.CREATED).body(ApiEnvelope.of(authService.registerTeacher(command)));
	}

	@PostMapping("/login")
	ResponseEntity<ApiEnvelope<AuthSessionRDO>> login(@Valid @RequestBody LoginCDO command) {
		return ResponseEntity.ok(ApiEnvelope.of(authService.login(command)));
	}

	@GetMapping({ "/me", "/session" })
	ResponseEntity<ApiEnvelope<CurrentUserRDO>> me(
			@RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader
	) {
		return ResponseEntity.ok(ApiEnvelope.of(
				authService.currentUser(authorizationService.requireActiveUser(authorizationHeader))));
	}
}
